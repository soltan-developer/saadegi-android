package ir.saadegi.android.diagnostics

/**
 * An initial reachability test is a prerequisite for a per-profile speed test,
 * not proof that a VPN tunnel is connected. The speed test performs a fresh
 * outbound check again before transferring data.
 */
object SpeedTestPolicy {
    const val MAX_RESULT_AGE_MS: Long = 90_000
    const val EXPECTED_DOWNLOAD_BYTES: Int = 5 * 1024 * 1024

    fun canTest(
        selectedId: String?,
        measuredResult: QualityResult?,
        measuredAtMillis: Long?,
        nowMillis: Long,
    ): Boolean {
        if (selectedId.isNullOrEmpty() || measuredResult == null || measuredAtMillis == null) return false
        if (measuredResult.profileId != selectedId || measuredResult.successes <= 0) return false
        if (nowMillis < measuredAtMillis) return false
        return nowMillis - measuredAtMillis <= MAX_RESULT_AGE_MS
    }

    fun meetsIntegrity(
        responseCode: Int,
        contentLength: Long,
        receivedBytes: Int,
        elapsedSeconds: Double,
    ): Boolean {
        if (responseCode != 200) return false
        if (contentLength >= 0 && contentLength != EXPECTED_DOWNLOAD_BYTES.toLong()) return false
        if (receivedBytes != EXPECTED_DOWNLOAD_BYTES) return false
        return elapsedSeconds.isFinite() && elapsedSeconds > 0
    }

    fun mbps(bytes: Int, seconds: Double): Double =
        if (bytes <= 0 || !seconds.isFinite() || seconds <= 0) 0.0
        else bytes * 8.0 / seconds / 1_000_000.0

    fun megabytesPerSecond(bytes: Int, seconds: Double): Double =
        if (bytes <= 0 || !seconds.isFinite() || seconds <= 0) 0.0
        else bytes.toDouble() / seconds / 1_000_000.0

    /**
     * A fixed 5 MiB download is useful for a quick estimate. If completed
     * too quickly it must not be presented as a definitive sustained max speed.
     */
    fun shortSample(seconds: Double): Boolean = seconds < 2.0
}
