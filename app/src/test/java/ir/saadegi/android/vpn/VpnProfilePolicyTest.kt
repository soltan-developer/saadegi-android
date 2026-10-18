package ir.saadegi.android.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnProfilePolicyTest {
    private val capabilities = LocalEngineCapabilities(
        supportedSchemaVersions = setOf(1),
        protocols = setOf("vless", "vmess"),
        transports = setOf("tcp", "xhttp"),
        securityModes = setOf("reality", "tls"),
    )
    private val profile = OfferedVpnProfile(
        id = "node-de-01", schemaVersion = 1, countryCode = "DE",
        protocol = "VLESS", transport = "XHTTP", security = "REALITY",
        expiresAtMs = 2_000_000,
    )
    private fun eligible(
        p: OfferedVpnProfile = profile,
        vpnAccessAllowed: Boolean = true,
        subscriptionActive: Boolean = true,
        nowMs: Long = 1_000_000,
    ): ProfileEligibility = VpnProfilePolicy.evaluate(
        p, capabilities, vpnAccessAllowed, subscriptionActive, nowMs,
    )

    @Test fun validProfileIsAccepted() {
        assertEquals(ProfileEligibility.Eligible, eligible())
    }

    @Test fun disabledVpnAccessOverridesValidSubscription() {
        assertEquals(
            ProfileEligibility.Blocked(ProfileBlockReason.VPN_ACCESS_DISABLED),
            eligible(vpnAccessAllowed = false),
        )
    }

    @Test fun inactiveSubscriptionIsRejected() {
        assertEquals(
            ProfileEligibility.Blocked(ProfileBlockReason.SUBSCRIPTION_INACTIVE),
            eligible(subscriptionActive = false),
        )
    }

    @Test fun expirationIsInclusive() {
        assertEquals(
            ProfileEligibility.Blocked(ProfileBlockReason.PROFILE_EXPIRED),
            eligible(nowMs = 2_000_000),
        )
    }

    @Test fun unknownProtocolDoesNotFallBackSilently() {
        assertEquals(
            ProfileEligibility.Blocked(ProfileBlockReason.PROTOCOL_UNSUPPORTED),
            eligible(profile.copy(protocol = "future-protocol")),
        )
    }

    @Test fun newTransportRequiresCapability() {
        assertEquals(
            ProfileEligibility.Blocked(ProfileBlockReason.TRANSPORT_UNSUPPORTED),
            eligible(profile.copy(transport = "new-transport")),
        )
    }

    @Test fun unknownSecurityIsNotDowngraded() {
        assertEquals(
            ProfileEligibility.Blocked(ProfileBlockReason.SECURITY_UNSUPPORTED),
            eligible(profile.copy(security = "none")),
        )
    }

    @Test fun newerSchemaRequiresAppUpgrade() {
        assertEquals(
            ProfileEligibility.Blocked(ProfileBlockReason.SCHEMA_UNSUPPORTED),
            eligible(profile.copy(schemaVersion = 99)),
        )
    }

    @Test fun malformedIdsAndCountriesAreRejected() {
        assertEquals(
            ProfileEligibility.Blocked(ProfileBlockReason.INVALID_PROFILE),
            eligible(profile.copy(id = "../../config")),
        )
        assertEquals(
            ProfileEligibility.Blocked(ProfileBlockReason.INVALID_PROFILE),
            eligible(profile.copy(countryCode = "unknown")),
        )
    }
}
