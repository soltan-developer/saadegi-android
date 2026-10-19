package ir.saadegi.android.subscription

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class SubscriptionParserTest {
    private val vless = "vless://UUID@de.example.org:443?security=reality&type=tcp#%F0%9F%87%A9%F0%9F%87%AA%20Germany%20A"
    private val trojan = "trojan://secret@tr.example.org:443?type=ws#Turkey"
    private val vmess = "vmess://" + Base64.getEncoder().encodeToString(
        """{"v":"2","ps":"Netherlands 01","add":"nl.example.org","port":"443","id":"redacted","net":"ws"}"""
            .toByteArray(Charsets.UTF_8)
    )

    @Test fun readsStandardPlaintextNodesAndActualRemarkCountries() {
        val parsed = SubscriptionParser.parse(vless + "\n" + trojan, "upload=100; download=400; total=1000; expire=1795000000", 12345)
        assertEquals(2, parsed.nodes.size)
        assertEquals("آلمان", parsed.nodes[0].location)
        assertEquals("ترکیه", parsed.nodes[1].location)
        assertEquals("Vless", parsed.nodes[0].protocol)
        assertEquals(500L, parsed.usage?.usedBytes)
        assertEquals(1000L, parsed.usage?.totalBytes)
        assertEquals(1795000000L, parsed.usage?.expiresAtSeconds)
    }

    @Test fun acceptsBase64SubscriptionAndVmessJson() {
        val raw = vless + "\n" + vmess
        val encoded = Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8))
        val parsed = SubscriptionParser.parse(encoded)
        assertEquals(2, parsed.nodes.size)
        assertEquals("VMess", parsed.nodes[1].protocol)
        assertEquals("هلند", parsed.nodes[1].location)
        assertEquals("ws", parsed.nodes[1].transport)
    }

    @Test fun unsupportedProtocolCanBeDisplayedButNotPresentedAsEngineCompatible() {
        val parsed = SubscriptionParser.parse("hysteria2://pass@demo.example.com:8443#Other")
        assertEquals(1, parsed.nodes.size)
        assertFalse(parsed.nodes[0].compatibleWithCurrentEngine)
        assertEquals("نامشخص", parsed.nodes[0].location)
    }

    @Test fun noMadeUpUsageOrLocation() {
        val parsed = SubscriptionParser.parse("vless://uuid@de.example.com:443#Random")
        assertEquals("نامشخص", parsed.nodes[0].location)
        assertNull(parsed.usage)
    }

    @Test fun deduplicateAndIgnoreMalformedLines() {
        val parsed = SubscriptionParser.parse(vless + "\n" + vless + "\nvless://bad\n# sample")
        assertEquals(1, parsed.nodes.size)
    }

    @Test fun emptyAndHtmlPagesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { SubscriptionParser.parse("<html>403 Forbidden</html>") }
        assertThrows(IllegalArgumentException::class.java) { SubscriptionParser.parse("") }
    }

    @Test fun rejectsHugeSubscriptions() {
        val tooBig = "X".repeat(1_048_577)
        assertThrows(IllegalArgumentException::class.java) { SubscriptionParser.parse(tooBig) }
    }
}
