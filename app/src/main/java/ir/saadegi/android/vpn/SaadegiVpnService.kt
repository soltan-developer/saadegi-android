package ir.saadegi.android.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import androidx.core.app.NotificationCompat
import ir.saadegi.android.subscription.SubscriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import libXray.LibXray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Internal Android VPN validation build only.
 * Not included in the published/release manifest until device acceptance tests pass.
 */
class SaadegiVpnService : VpnService() {
    companion object {
        const val ACTION_CONNECT = "ir.saadegi.android.vpn.CONNECT"
        const val ACTION_STOP = "ir.saadegi.android.vpn.STOP"
        const val EXTRA_PROFILE_ID = "profileId"
        private const val NOTIFICATION_ID = 3107
        private const val CHANNEL_ID = "saadegi_internal_vpn"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectionJob: Job? = null
    private var native: NativeTunGateZero? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            VpnDiagnosticLog.add(VpnDiagnosticLog.Step.STOP_REQUEST)
            stopConnection()
            stopSelf()
            return START_NOT_STICKY
        }
        val profileId = intent?.getStringExtra(EXTRA_PROFILE_ID)
        if (intent?.action != ACTION_CONNECT ||
            profileId == null || !Regex("^[a-f0-9]{24}$").matches(profileId)) {
            VpnDiagnosticLog.add(VpnDiagnosticLog.Step.INVALID_PROFILE)
            VpnLiveStatus.set(TunnelStatus(TunnelPhase.ERROR, message = "سرور معتبر انتخاب نشده است."))
            stopSelf()
            return START_NOT_STICKY
        }
        if (connectionJob?.isActive == true || native != null) return START_NOT_STICKY
        VpnDiagnosticLog.add(VpnDiagnosticLog.Step.SERVICE)
        try {
            startOngoingNotification()
        } catch (_: Exception) {
            VpnDiagnosticLog.add(VpnDiagnosticLog.Step.FOREGROUND_FAILED)
            VpnLiveStatus.set(TunnelStatus(TunnelPhase.ERROR, message = "اجرای سرویس پس‌زمینه ممکن نشد."))
            stopSelf()
            return START_NOT_STICKY
        }
        connectionJob = scope.launch { connectAndVerify(profileId) }
        return START_NOT_STICKY
    }

    private fun startOngoingNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_ID, "اتصال آزمایشی سادگی", NotificationManager.IMPORTANCE_LOW))
        val stopIntent = Intent(this, SaadegiVpnService::class.java).apply { action = ACTION_STOP }
        val pending = PendingIntent.getService(
            this, 8, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("تحریم‌گذر سادگی — آزمایش اتصال")
            .setContentText("اتصال در حال بررسی است؛ امکان توقف فوری وجود دارد")
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "قطع اتصال", pending)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        VpnDiagnosticLog.add(VpnDiagnosticLog.Step.FOREGROUND)
    }

    private suspend fun connectAndVerify(profileId: String) {
        VpnLiveStatus.set(TunnelStatus(TunnelPhase.STARTING, profileId, "در حال آماده‌سازی تونل"))
        try {
            VpnDiagnosticLog.add(VpnDiagnosticLog.Step.PROFILE_LOAD)
            val uri = SubscriptionRepository(this).getProfileUri(profileId)
                ?: throw IllegalStateException("PROFILE_MISSING")
            VpnDiagnosticLog.add(VpnDiagnosticLog.Step.CONVERT)
            val converted = JSONObject(LibXray.invoke(JSONObject()
                .put("apiVersion", 3)
                .put("method", "convertShareLinksToXrayJson")
                .put("payload", JSONObject().put("text", uri)).toString()))
            if (!converted.optBoolean("success")) {
                VpnDiagnosticLog.add(VpnDiagnosticLog.Step.CONVERT_FAILED)
                throw IllegalStateException("UNSUPPORTED_PROFILE")
            }
            val outbounds = converted.optJSONObject("data")?.optJSONArray("outbounds")
            if (outbounds == null || outbounds.length() != 1)
                throw IllegalStateException("INVALID_PROFILE")
            val gate = NativeTunGateZero(this)
            native = gate
            if (!gate.start(outbounds.getJSONObject(0)))
                throw IllegalStateException("TUN_START_FAILED")
            currentCoroutineContext().ensureActive()
            VpnLiveStatus.set(TunnelStatus(TunnelPhase.VERIFYING, profileId, "بررسی عبور واقعی ترافیک"))
            VpnDiagnosticLog.add(VpnDiagnosticLog.Step.VERIFY_BEGIN)
            if (!verifyRoutedHttps()) throw IllegalStateException("VPN_TRAFFIC_VERIFICATION_FAILED")
            currentCoroutineContext().ensureActive()
            VpnDiagnosticLog.add(VpnDiagnosticLog.Step.VERIFIED)
            VpnLiveStatus.set(TunnelStatus(TunnelPhase.CONNECTED, profileId, "ترافیک VPN تأیید شد"))
        } catch (_: CancellationException) {
            VpnDiagnosticLog.add(VpnDiagnosticLog.Step.CANCELLED)
            native?.stop()
            native = null
            VpnLiveStatus.set(TunnelStatus(TunnelPhase.IDLE))
            stopSelf()
        } catch (error: Exception) {
            val mapped = when (error.message) {
                "PROFILE_MISSING" -> VpnDiagnosticLog.Step.PROFILE_MISSING
                "UNSUPPORTED_PROFILE", "INVALID_PROFILE" -> VpnDiagnosticLog.Step.CONVERT_FAILED
                "TUN_START_FAILED" -> VpnDiagnosticLog.Step.TUN_FAILED
                "VPN_TRAFFIC_VERIFICATION_FAILED" -> VpnDiagnosticLog.Step.VERIFY_HTTP_FAILED
                else -> VpnDiagnosticLog.classify(error)
            }
            VpnDiagnosticLog.add(mapped)
            native?.stop()
            native = null
            VpnLiveStatus.set(TunnelStatus(TunnelPhase.ERROR, profileId,
                mapped.label + "؛ اتصال برای محافظت از اینترنت متوقف شد. گزارش عیب‌یابی را باز کنید."))
            stopSelf()
        }
    }

    /** Check an explicit VPN Network, never silently retry over phone's default network. */
    private suspend fun verifyRoutedHttps(): Boolean = withContext(Dispatchers.IO) {
        val connectivity = getSystemService(ConnectivityManager::class.java)
        val target = URL("https://www.gstatic.com/generate_204")
        var foundVpn = false
        repeat(15) {
            val active: Network? = connectivity.allNetworks.firstOrNull { network ->
                connectivity.getNetworkCapabilities(network)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            }
            if (active != null) {
                foundVpn = true
                try {
                    val connection = active.openConnection(target) as HttpsURLConnection
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 2_000
                    connection.readTimeout = 2_000
                    try {
                        if (connection.responseCode == 204) return@withContext true
                    } finally { connection.disconnect() }
                } catch (_: Exception) { /* no direct-network fallback */ }
            }
            delay(400)
        }
        if (!foundVpn) VpnDiagnosticLog.add(VpnDiagnosticLog.Step.VERIFY_NETWORK_MISSING)
        false
    }

    private fun stopConnection() {
        connectionJob?.cancel()
        connectionJob = null
        VpnLiveStatus.set(TunnelStatus(TunnelPhase.STOPPING))
        native?.stop()
        native = null
        VpnLiveStatus.set(TunnelStatus(TunnelPhase.IDLE))
        VpnDiagnosticLog.add(VpnDiagnosticLog.Step.STOPPED)
    }

    override fun onRevoke() {
        VpnDiagnosticLog.add(VpnDiagnosticLog.Step.REVOKED)
        stopConnection()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        connectionJob?.cancel()
        native?.stop()
        native = null
        scope.cancel()
        if (VpnLiveStatus.state.value.phase != TunnelPhase.ERROR)
            VpnLiveStatus.set(TunnelStatus(TunnelPhase.IDLE))
        super.onDestroy()
    }
}
