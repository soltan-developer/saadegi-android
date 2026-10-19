package ir.saadegi.android.diagnostics

enum class ProbeMode(val samples: Int) { ONE_STEP(1), THREE_STEP(3) }

/** Real HEAD sample in ms. A null sample is a failed connection, never zero latency. */
data class QualityResult(
    val profileId: String,
    val attempts: Int,
    val successes: Int,
    val medianLatencyMs: Long?,
    val jitterMs: Long?,
    val stabilityScore: Int?,
    val samplesMs: List<Long?>,
)

object QualityScoring {
    fun aggregate(profileId: String, samples: List<Long?>): QualityResult {
        require(samples.isNotEmpty() && samples.size <= 3)
        val valid = samples.filterNotNull().filter { it in 1..9_999 }.sorted()
        if (valid.isEmpty()) return QualityResult(profileId, samples.size, 0, null, null, null, samples)
        val median = if (valid.size % 2 == 0)
            (valid[valid.size / 2 - 1] + valid[valid.size / 2]) / 2
            else valid[valid.size / 2]
        val jitter = if (valid.size >= 2) valid.last() - valid.first() else null
        // Comparable only within the same mode, endpoint and network.
        // Reliability dominates raw latency; 1-step cannot establish stability.
        val availability = valid.size * 100.0 / samples.size
        val latencyPenalty = median.coerceAtMost(1500) / 1500.0 * 35
        val jitterPenalty = (jitter ?: 0L).coerceAtMost(1000) / 1000.0 * 15
        val score = (availability - latencyPenalty - jitterPenalty).toInt().coerceIn(0, 100)
        return QualityResult(profileId, samples.size, valid.size, median, jitter, score, samples)
    }

    fun rank(results: List<QualityResult>): List<QualityResult> = results.sortedWith(
        compareByDescending<QualityResult> { it.successes.toDouble() / it.attempts }
            .thenByDescending { it.stabilityScore ?: -1 }
            .thenBy { it.medianLatencyMs ?: Long.MAX_VALUE }
    )
}
