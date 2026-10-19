package ir.saadegi.android.vpn

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NativeTunConfigFactoryTest {
    @Test fun createsExplicitFdAndOutboundTunnelRouting() {
        val source = JSONObject("""{"protocol":"vless","settings":{"vnext":[]}}""")
        val config = NativeTunConfigFactory.create(42, source)
        assertEquals("42", config.getJSONObject("env").getString("xray.tun.fd"))
        assertEquals("tun", config.getJSONArray("inbounds").getJSONObject(0).getString("protocol"))
        assertEquals(NativeTunConfigFactory.TUN_MTU,
            config.getJSONArray("inbounds").getJSONObject(0).getJSONObject("settings").getInt("mtu"))
        assertEquals("saadegi-egress", config.getJSONArray("outbounds").getJSONObject(0).getString("tag"))
        val rule = config.getJSONObject("routing").getJSONArray("rules").getJSONObject(0)
        assertEquals("saadegi-tun", rule.getJSONArray("inboundTag").getString(0))
        assertEquals("saadegi-egress", rule.getString("outboundTag"))
        assertFalse(source.has("tag")) // preserve input purity
    }

    @Test fun missingFdOrProtocolFails() {
        assertThrows(IllegalArgumentException::class.java) {
            NativeTunConfigFactory.create(0, JSONObject("""{"protocol":"vless"}"""))
        }
        assertThrows(IllegalArgumentException::class.java) {
            NativeTunConfigFactory.create(42, JSONObject("""{"protocol":""}"""))
        }
    }
}
