package ir.saadegi.android.vpn

import org.json.JSONArray
import org.json.JSONObject

/**
 * Gate-0 direct Xray TUN experiment.
 * The service entry point MUST remain disabled until end-to-end device testing.
 */
object NativeTunConfigFactory {
    const val TUN_TAG = "saadegi-tun"
    const val EGRESS_TAG = "saadegi-egress"
    const val TUN_MTU = 1280

    fun create(tunFd: Int, sourceOutbound: JSONObject): JSONObject {
        require(tunFd > 0) { "TUN_FD_MISSING" }
        val outbound = JSONObject(sourceOutbound.toString()).put("tag", EGRESS_TAG)
        require(outbound.optString("protocol").isNotBlank()) { "OUTBOUND_PROTOCOL_MISSING" }
        val inbound = JSONObject()
            .put("tag", TUN_TAG)
            .put("port", 0)
            .put("protocol", "tun")
            .put("settings", JSONObject().put("mtu", TUN_MTU))
        val rule = JSONObject()
            .put("type", "field")
            .put("inboundTag", JSONArray().put(TUN_TAG))
            .put("outboundTag", EGRESS_TAG)
        return JSONObject()
            // Official Xray Android TUN support takes the VPN fd from this root env.
            .put("env", JSONObject().put("xray.tun.fd", tunFd.toString()))
            .put("log", JSONObject().put("loglevel", "none"))
            .put("inbounds", JSONArray().put(inbound))
            .put("outbounds", JSONArray().put(outbound))
            .put("routing", JSONObject().put("rules", JSONArray().put(rule)))
    }
}
