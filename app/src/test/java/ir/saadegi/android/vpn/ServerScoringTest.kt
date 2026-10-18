package ir.saadegi.android.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerScoringTest {
    private val fast = ServerCandidate("fast", "de", 50, 0.4, 0.55)
    private val steady = ServerCandidate("steady", "de", 160, 0.99, 0.01, true)
    private val holland = ServerCandidate("holland", "nl", 80, 0.9, 0.08)

    @Test fun reliabilityBeatsRawPing() {
        assertEquals("steady", ServerScoring.choose(listOf(fast, steady))?.id)
    }

    @Test fun manualCountryFilterIsRespected() {
        assertEquals("holland", ServerScoring.choose(listOf(fast, steady, holland), "nl")?.id)
        assertNull(ServerScoring.choose(listOf(fast, steady), "tr"))
    }

    @Test fun hysteresisAvoidsNeedlessSwitch() {
        val active = steady.copy(id="active", latencyMs=160, handshakeSuccessRate=0.95)
        assertEquals("active", ServerScoring.choose(listOf(active, steady), activeId="active")?.id)
    }

    @Test fun invalidSamplesAreExcluded() {
        assertNull(ServerScoring.choose(listOf(fast.copy(latencyMs=null))))
        assertNull(ServerScoring.score(fast.copy(handshakeSuccessRate=2.0)))
        assertTrue((ServerScoring.score(steady) ?: 0.0) in 0.0..100.0)
    }
}
