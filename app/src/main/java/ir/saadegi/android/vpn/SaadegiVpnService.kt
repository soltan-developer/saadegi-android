package ir.saadegi.android.vpn

import android.content.Intent
import android.net.VpnService

/**
 * Reserved Android system integration point.
 *
 * Do not enable this service in the manifest until a real core, protected
 * sockets and verified TUN forwarding are wired. Establishing an empty TUN
 * would silently black-hole the customer's entire internet connection.
 */
class SaadegiVpnService : VpnService() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
