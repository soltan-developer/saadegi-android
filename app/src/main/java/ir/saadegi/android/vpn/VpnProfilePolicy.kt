package ir.saadegi.android.vpn

import java.util.Locale

/**
 * Non-secret profile metadata from the authenticated Mobile API.
 *
 * No credentials or raw connection URIs belong here. The opaque encrypted
 * profile must be fetched separately only after entitlement verification.
 */
data class OfferedVpnProfile(
    val id: String,
    val schemaVersion: Int,
    val countryCode: String,
    val protocol: String,
    val transport: String,
    val security: String,
    val expiresAtMs: Long,
    val enabled: Boolean = true,
)

data class LocalEngineCapabilities(
    val supportedSchemaVersions: Set<Int>,
    val protocols: Set<String>,
    val transports: Set<String>,
    val securityModes: Set<String>,
)

enum class ProfileBlockReason {
    VPN_ACCESS_DISABLED,
    SUBSCRIPTION_INACTIVE,
    PROFILE_DISABLED,
    INVALID_PROFILE,
    PROFILE_EXPIRED,
    SCHEMA_UNSUPPORTED,
    PROTOCOL_UNSUPPORTED,
    TRANSPORT_UNSUPPORTED,
    SECURITY_UNSUPPORTED,
}

sealed interface ProfileEligibility {
    data object Eligible : ProfileEligibility
    data class Blocked(val reason: ProfileBlockReason) : ProfileEligibility
}

/**
 * Defense in depth. Server-side ownership/entitlement MUST also be enforced;
 * this device-side check is never a substitute for authenticated API checks.
 */
object VpnProfilePolicy {
    private val safeId = Regex("^[A-Za-z0-9._-]{3,128}$")
    private val safeCountry = Regex("^[a-zA-Z]{2}$")

    fun evaluate(
        profile: OfferedVpnProfile,
        capabilities: LocalEngineCapabilities,
        vpnAccessAllowed: Boolean,
        subscriptionActive: Boolean,
        nowMs: Long,
    ): ProfileEligibility {
        fun blocked(reason: ProfileBlockReason): ProfileEligibility =
            ProfileEligibility.Blocked(reason)
        if (!vpnAccessAllowed) return blocked(ProfileBlockReason.VPN_ACCESS_DISABLED)
        if (!subscriptionActive) return blocked(ProfileBlockReason.SUBSCRIPTION_INACTIVE)
        if (!profile.enabled) return blocked(ProfileBlockReason.PROFILE_DISABLED)
        if (!safeId.matches(profile.id) || !safeCountry.matches(profile.countryCode) ||
            profile.schemaVersion < 1 || profile.expiresAtMs <= 0 || nowMs < 0
        ) return blocked(ProfileBlockReason.INVALID_PROFILE)
        if (nowMs >= profile.expiresAtMs) return blocked(ProfileBlockReason.PROFILE_EXPIRED)
        if (profile.schemaVersion !in capabilities.supportedSchemaVersions) {
            return blocked(ProfileBlockReason.SCHEMA_UNSUPPORTED)
        }
        fun normalize(s: String) = s.trim().lowercase(Locale.ROOT)
        if (normalize(profile.protocol) !in capabilities.protocols.map(::normalize)) {
            return blocked(ProfileBlockReason.PROTOCOL_UNSUPPORTED)
        }
        if (normalize(profile.transport) !in capabilities.transports.map(::normalize)) {
            return blocked(ProfileBlockReason.TRANSPORT_UNSUPPORTED)
        }
        if (normalize(profile.security) !in capabilities.securityModes.map(::normalize)) {
            return blocked(ProfileBlockReason.SECURITY_UNSUPPORTED)
        }
        return ProfileEligibility.Eligible
    }
}
