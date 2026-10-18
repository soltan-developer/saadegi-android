package ir.saadegi.android.vpn

/**
 * The implementation will be provided by a separate audited native module.
 * GPL app/client code must not be copied into this proprietary app.
 */
interface VpnEngine {
    fun capabilities(): Set<String>
    suspend fun start(configuration: OpaqueVpnConfiguration): Result<Unit>
    suspend fun stop(): Result<Unit>
    fun isRunning(): Boolean
}

/**
 * Configuration content must be created by the authenticated Mobile API.
 * Do not override toString: an accidental log must not expose credentials.
 */
class OpaqueVpnConfiguration internal constructor(
    val profileId: String,
    val contents: ByteArray,
) {
    fun clear() = contents.fill(0)
}

class NativeCoreUnavailable : VpnEngine {
    override fun capabilities(): Set<String> = emptySet()
    override suspend fun start(configuration: OpaqueVpnConfiguration): Result<Unit> =
        Result.failure(IllegalStateException("VPN_ENGINE_NOT_INTEGRATED"))
    override suspend fun stop(): Result<Unit> = Result.success(Unit)
    override fun isRunning(): Boolean = false
}
