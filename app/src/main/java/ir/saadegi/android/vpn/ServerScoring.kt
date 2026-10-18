package ir.saadegi.android.vpn

/** Metrics come from observed probes; do not fabricate values for unavailable nodes. */
data class ServerCandidate(
    val id: String,
    val countryCode: String,
    val latencyMs: Int?,
    val handshakeSuccessRate: Double,
    val failureRate: Double,
    val recentStable: Boolean = false,
)

object ServerScoring {
    /** A 0..100 comparative score, not a promise of throughput or availability. */
    fun score(candidate: ServerCandidate): Double? {
        val latency = candidate.latencyMs ?: return null
        if (latency <= 0 || latency > 30_000) return null
        val success = candidate.handshakeSuccessRate
        val failures = candidate.failureRate
        if (!success.isFinite() || !failures.isFinite() ||
            success !in 0.0..1.0 || failures !in 0.0..1.0) return null
        val latencyComponent = (1_000.0 / (1_000.0 + latency)) * 30.0
        return (success * 50 + (1.0 - failures) * 15 +
            latencyComponent + if (candidate.recentStable) 5.0 else 0.0)
            .coerceIn(0.0, 100.0)
    }

    /**
     * If an existing healthy route is nearly as good as the top candidate,
     * prefer it to avoid oscillation and packet loss from needless handoffs.
     */
    fun choose(
        candidates: List<ServerCandidate>,
        countryCode: String? = null,
        activeId: String? = null,
        minimumGain: Double = 12.0,
    ): ServerCandidate? {
        val ranked = candidates.asSequence()
            .filter { countryCode == null || it.countryCode == countryCode }
            .mapNotNull { server -> score(server)?.let { server to it } }
            .sortedByDescending { it.second }
            .toList()
        val best = ranked.firstOrNull() ?: return null
        val current = ranked.firstOrNull { it.first.id == activeId }
        if (current != null && best.second - current.second < minimumGain) return current.first
        return best.first
    }
}
