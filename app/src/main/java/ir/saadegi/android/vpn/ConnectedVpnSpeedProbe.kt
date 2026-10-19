package ir.saadegi.android.vpn

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import ir.saadegi.android.diagnostics.SpeedTestPolicy
import ir.saadegi.android.diagnostics.XrayNativeDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * This module is NOT wired into the app until the Android VPN session can
 * prove CONNECTED for the requested profile and provide its own VPN Network.
 *
 * Crucial difference from the offline diagnostics:
 * - no second runXray() instance is started;
 * - opening HTTPS through the explicit VPN Network prevents direct fallback.
 */
class ConnectedVpnSpeedProbe(private val connectivity: ConnectivityManager) {
    companion object {
        private const val SPEED_URL = "https://speed.cloudflare.com/__down?bytes=5242880"
        private const val MAX_BYTES = SpeedTestPolicy.EXPECTED_DOWNLOAD_BYTES
    }

    /**
     * Issued only after the owning VpnService has established TUN AND verified
     * a real tunneled request. Never construct based on UI selection alone.
     */
    data class VerifiedSession internal constructor(
        val connectedProfileId: String,
        val vpnNetwork: Network,
    )

    suspend fun test(
        requestedProfileId: String,
        verified: VerifiedSession,
    ): XrayNativeDiagnostics.SpeedResult = withContext(Dispatchers.IO) {
        require(requestedProfileId == verified.connectedProfileId) { "ACTIVE_PROFILE_MISMATCH" }
        val capabilities = connectivity.getNetworkCapabilities(verified.vpnNetwork)
            ?: throw IllegalStateException("VPN_NETWORK_LOST")
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
            throw IllegalStateException("NOT_A_VPN_NETWORK")

        val connection = (verified.vpnNetwork.openConnection(URL(SPEED_URL)) as HttpsURLConnection)
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.connectTimeout = 9_000
        connection.readTimeout = 12_000
        connection.setRequestProperty("Cache-Control", "no-cache, no-store")
        connection.setRequestProperty("Accept-Encoding", "identity")

        val start = System.nanoTime()
        try {
            val code = connection.responseCode
            val declared = connection.contentLengthLong
            if (code != 200 || (declared >= 0 && declared != MAX_BYTES.toLong()) ||
                connection.contentEncoding?.lowercase() !in listOf(null, "identity")
            ) throw IllegalStateException("INVALID_SPEED_RESPONSE")

            var received = 0
            connection.inputStream.use { input ->
                val buffer = ByteArray(32768)
                while (received < MAX_BYTES) {
                    currentCoroutineContext().ensureActive()
                    if (System.nanoTime() - start > 30_000_000_000L)
                        throw IllegalStateException("SPEED_TIMEOUT")
                    if (connectivity.getNetworkCapabilities(verified.vpnNetwork)
                            ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) != true)
                        throw IllegalStateException("VPN_NETWORK_LOST")
                    val count = input.read(buffer, 0, minOf(buffer.size, MAX_BYTES - received))
                    if (count < 0) break
                    received += count
                }
                if (declared < 0 && received == MAX_BYTES && input.read() != -1)
                    throw IllegalStateException("OVERSIZED_RESPONSE")
            }
            val seconds = (System.nanoTime() - start) / 1_000_000_000.0
            if (!SpeedTestPolicy.meetsIntegrity(code, declared, received, seconds))
                throw IllegalStateException("INCOMPLETE_SPEED_TEST")
            return@withContext XrayNativeDiagnostics.SpeedResult(
                transferredBytes = received,
                seconds = seconds,
                mbps = SpeedTestPolicy.mbps(received, seconds),
                megabytesPerSecond = SpeedTestPolicy.megabytesPerSecond(received, seconds),
                lowConfidence = SpeedTestPolicy.shortSample(seconds),
            )
        } finally {
            connection.disconnect()
        }
    }
}
