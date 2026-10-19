package ir.saadegi.android.diagnostics

import org.junit.Assert.*
import org.junit.Test

class QualityScoringTest {
    @Test fun threeStageMeasuresLatencyVariation() {
        val stable = QualityScoring.aggregate("stable", listOf(105L, 110L, 108L))
        val flaky = QualityScoring.aggregate("flaky", listOf(25L, null, null))
        assertEquals(108L, stable.medianLatencyMs)
        assertEquals(5L, stable.jitterMs)
        assertEquals(3, stable.successes)
        assertEquals("stable", QualityScoring.rank(listOf(flaky, stable)).first().profileId)
    }

    @Test fun failuresNeverCountAsZeroMillisecondLatency() {
        val r = QualityScoring.aggregate("down", listOf(null, null, null))
        assertNull(r.medianLatencyMs)
        assertNull(r.stabilityScore)
        assertEquals(0, r.successes)
    }

    @Test fun oneStageCannotInventJitter() {
        val r = QualityScoring.aggregate("one", listOf(125L))
        assertEquals(125L, r.medianLatencyMs)
        assertNull(r.jitterMs)
        assertEquals(1, r.attempts)
    }

    @Test fun halfSuccessfulTestsHaveLowerScore() {
        val partial = QualityScoring.aggregate("partial", listOf(80L, null, 95L))
        val success = QualityScoring.aggregate("ok", listOf(100L, 104L, 102L))
        assertTrue(success.stabilityScore!! > partial.stabilityScore!!)
    }
}
