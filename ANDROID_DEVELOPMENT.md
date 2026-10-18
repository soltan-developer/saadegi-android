# Saadegi Android — implementation status and developer guide

Branch: \`feat/android-native-foundation-vpn-poc\` | Status: **not a working VPN**

This branch includes an Android Compose shell, themed Persian RTL screens, a disabled-by-design VpnService entrypoint, a version-neutral engine interface, pure connection state-machine, node-scoring logic and unit tests. It has *no production VPN binary, no user authentication, no server profiles, no ticket/API connection*. The primary connect button is deliberately disabled; we must never claim VPN connectivity on a UI-only build.

## Build (requires an Android development environment)

- JDK 17, Android SDK API 37 and build tools 36.0.0.
- Gradle 9.4.1; AGP 9.2.0 (built-in Kotlin), Compose compiler plugin 2.2.10, Compose BOM 2026.09.00.
- Run \`gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug\`.
- Gradle wrapper is not included yet: the build environment here does not have Gradle/Android SDK and cannot download them. A trusted local machine can run \`gradle wrapper --gradle-version 9.4.1\` and commit *both* generated gradlew scripts and the wrapper jar to follow normal repo practices.
- CI workflow declares the same build through GitHub Actions. If the account has exhausted CI minutes, the job may remain queued; **green builds must be observed**, not assumed.

## Native Gate-0 integration checklist

1. Pin a release of https://github.com/XTLS/libXray (MIT wrapper for MPL-2.0 Xray core); libXray documents an Android gomobile build. Review dependency tree and single Go runtime constraint.
2. Pin MIT HevSocks5Tunnel https://github.com/heiher/hev-socks5-tunnel to expose JNI tun2socks. The available release AAR has JNI class \`hev.htproxy.TProxyService\`; confirm exact version and ABI support.
3. Bundle audited artifacts with reproducible version and SHA-256, third-party notices and source obligations. Do not download unsigned/unpinned libraries inside the app at runtime.
4. A VpnService implementation must request permission via \`VpnService.prepare()\`, create and protect the outbound Xray socket, bind local SOCKS, establish TUN, start tun2socks, handle UDP+DNS+IPv6, and verify handshakes **before** showing CONNECTED.
5. Implement lifecycle safety: foreground type/notification based on active target API policy; onRevoke, process death, connectivity handoff, explicit stop, no leaks.
6. Enable service in AndroidManifest **only after** fully working native integration and tests. An empty TUN would cut off the customer's internet and is strictly forbidden.

## Integration with saadegi-site

Keep this app's source separate. In the site create \`/api/mobile/v1\` via its own PR following \`docs/api/MOBILE_API_CONTRACT_DRAFT.md\` on the planning branch. Check server VPN entitlement and ticket ownership independently of client UI; never expose 3x-ui administrator tokens. News, social and storefront need backend access and are not integrated here.

## Security guardrails

- No raw proxy URI, recovery codes, profile secrets, OTP or user ticket text in logs/analytics.
- No GPL-licensed Android client code copied into the product; license audit includes all transitive dependencies of libXray.
- App backups disabled for future token storage, cleartext transport prohibited.
- \`applicationId\` and signing keys need confirmation before public release; never commit signing keys.
- Replace Unicode-only placeholder icons with a licensed, consistent vector icon set and native Persian font after confirming font licensing.

## Environment limitation in this PR

Android SDK and Gradle were not installed in the build environment, and outbound DNS to distribution servers was unavailable. The connected repository was updated with source files, but no Android APK was compiled or device connectivity verified here. GitHub Actions is included to provide an actual build outcome once CI is available.
