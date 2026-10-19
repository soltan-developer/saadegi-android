package ir.saadegi.android.vpn

import android.net.VpnService
import android.os.ParcelFileDescriptor
import libXray.DialerController
import libXray.LibXray
import org.json.JSONObject

/**
 * INTERNAL GATE-0 ONLY. This is intentionally not attached to the production
 * app, button, or manifest's disabled VPN service. Never call without prior
 * VpnService.prepare() approval, foreground notification, and tests.
 *
 * Xray 26.10.17 implements native Android TUN using root env.xray.tun.fd.
 * No separate tun2socks shared library is needed for this prototype.
 */
class NativeTunGateZero(private val owner: VpnService) {
    private var interfaceFd: ParcelFileDescriptor? = null
    private var coreStarted: Boolean = false

    @Synchronized
    fun start(outbound: JSONObject): Boolean {
        if (interfaceFd != null || coreStarted) return false
        if (VpnService.prepare(owner) != null) return false

        val protector = object : DialerController {
            override fun protectFd(fd: Long): Boolean =
                fd in 0..Int.MAX_VALUE.toLong() && owner.protect(fd.toInt())
        }
        var tun: ParcelFileDescriptor? = null
        try {
            // The address + default route is only safe when Xray is ready.
            // We tear down the fd immediately if any subsequent step fails.
            tun = owner.Builder()
                .setSession("سادگی — آزمایش داخلی")
                .setMtu(NativeTunConfigFactory.TUN_MTU)
                .addAddress("10.77.0.2", 30)
                .addRoute("0.0.0.0", 0)
                .addAddress("fd77:1::2", 126)
                .addRoute("::", 0)
                .addDnsServer("1.1.1.1")
                .establish() ?: return false

            LibXray.registerDialerController(protector)
            // Ensure Go bootstrap DNS socket is excluded from the TUN to avoid
            // resolver loops. The tunnel's app DNS traffic is still routed
            // through the Xray TUN outbound. DNS leak tests are mandatory.
            LibXray.setDNS(protector, "1.1.1.1:53")
            val config = NativeTunConfigFactory.create(tun.fd, outbound)
            val started = invoke("runXray", JSONObject().put("xrayJson", config.toString()))
            if (!started.optBoolean("success", false)) return false
            coreStarted = true
            interfaceFd = tun
            tun = null
            // An Xray instance started != tunnel verified. An independent
            // routed HTTP/DNS probe is required before UI says "متصل".
            return true
        } catch (_: Exception) {
            return false
        } finally {
            if (!coreStarted) {
                tun?.close()
                try { invoke("stopXray", JSONObject()) } catch (_: Exception) {}
                try { LibXray.resetDNS() } catch (_: Exception) {}
                // Releasing controller in failure path prevents re-use.
                try { LibXray.registerDialerController(null) } catch (_: Exception) {}
            }
        }
    }

    @Synchronized
    fun stop() {
        if (coreStarted) {
            try { invoke("stopXray", JSONObject()) } catch (_: Exception) {}
        }
        coreStarted = false
        try { LibXray.resetDNS() } catch (_: Exception) {}
        try { LibXray.registerDialerController(null) } catch (_: Exception) {}
        interfaceFd?.close()
        interfaceFd = null
    }

    private fun invoke(method: String, payload: JSONObject): JSONObject {
        val request = JSONObject()
            .put("apiVersion", 3)
            .put("method", method)
            .put("payload", payload)
        return JSONObject(LibXray.invoke(request.toString()))
    }
}
