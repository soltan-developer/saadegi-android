package ir.saadegi.android.diagnostics

import org.junit.Assert.*
import org.junit.Test

class SpeedTestPolicyTest {
    private val successful = QualityScoring.aggregate("node-a", listOf(75L, 80L, 78L))

    @Test fun onlyRecentSuccessfulSelectedProfileOffersSpeedTest() {
        assertTrue(SpeedTestPolicy.canTest("node-a", successful, 10_000L, 20_000L))
        assertFalse(SpeedTestPolicy.canTest("node-b", successful, 10_000L, 20_000L))
        assertFalse(SpeedTestPolicy.canTest("node-a", successful, 10_000L, 110_001L))
        assertFalse(SpeedTestPolicy.canTest(null, successful, 10_000L, 20_000L))
        assertFalse(SpeedTestPolicy.canTest("node-a", successful, null, 20_000L))
        assertFalse(SpeedTestPolicy.canTest("node-a", successful, 25_000L, 20_000L))
    }

    @Test fun failedLatencyDoesNotUnlockSpeedTest() {
        val failed = QualityScoring.aggregate("node-a", listOf(null, null, null))
        assertFalse(SpeedTestPolicy.canTest("node-a", failed, 10_000L, 10_001L))
    }

    @Test fun corruptedOrPartialDownloadCannotBeReportedAsSuccess() {
        val expected = SpeedTestPolicy.EXPECTED_DOWNLOAD_BYTES
        assertTrue(SpeedTestPolicy.meetsIntegrity(200, expected.toLong(), expected, 2.1))
        assertTrue(SpeedTestPolicy.meetsIntegrity(200, -1, expected, 2.1))
        assertFalse(SpeedTestPolicy.meetsIntegrity(206, expected.toLong(), expected, 2.1))
        assertFalse(SpeedTestPolicy.meetsIntegrity(200, expected.toLong(), expected - 1, 2.1))
        assertFalse(SpeedTestPolicy.meetsIntegrity(200, (expected + 1).toLong(), expected, 2.1))
        assertFalse(SpeedTestPolicy.meetsIntegrity(200, -1, expected, 0.0))
        assertFalse(SpeedTestPolicy.meetsIntegrity(200, -1, expected, Double.NaN))
    }

    @Test fun bandwidthUnitsUseDecimalMegabitsAndMegabytes() {
        val bytes = 5_000_000
        assertEquals(20.0, SpeedTestPolicy.mbps(bytes, 2.0), 0.001)
        assertEquals(2.5, SpeedTestPolicy.megabytesPerSecond(bytes, 2.0), 0.001)
        assertTrue(SpeedTestPolicy.shortSample(0.9))
        assertFalse(SpeedTestPolicy.shortSample(2.0))
    }
}
