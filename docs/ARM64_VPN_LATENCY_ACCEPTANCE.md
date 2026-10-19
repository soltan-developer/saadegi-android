# Gate: arm64 VPN / latency parity acceptance

**Internal candidate; not production.** Parent PRs #2..#5 are stacked. The user's report was v2rayNG ~200 ms and Saadegi ~1300 ms on one server, plus oversized APK and a nonfunctional connect button.

## Why the old build was misleading
- Connect UI was not wired; main AndroidManifest disabled SaadegiVpnService. Native TUN was an unused POC.
- Ping used Cloudflare cp.cloudflare.com; v2rayNG defaults to https://www.gstatic.com/generate_204. Different HTTPS targets, timing, cold starts and network paths cannot be directly compared.
- The default debug bundle included multiple native libXray architectures, inflating the APK.

## What this PR changes
- Internal arm64-v8a only APK; CI checks ABI list and prints exact APK size. A release AAB should later produce ABI-targeted outputs for broader device coverage.
- Default HTTP URL now matches v2rayNG. A pre-measurement warm-up is excluded from the scored 1 or 3 samples. Results are HTTP request time, not ICMP or pure server TCP RTT.
- Diagnostic UX: one test button, mode bottom sheet, per-server latency / success / jitter next to each server; speed button only after a recent successful sample.
- Experimental VPN permission -> Android foreground service -> selected encrypted subscription URI -> Xray native TUN -> HTTPS through explicit VPN Network. Status CONNECTED only after HTTPS response 204. Errors stop and close TUN; explicit disconnect provided.
- Experimental VPN service is debug-manifest enabled only; release remains disabled. Internal package suffix .lab installs beside prior .debug preview (re-enter subscription privately).

## Required same-network checks
1. Install the arm64 internal APK on a real 64-bit Android device. It is NOT for 32-bit-only phones. Do not paste subscription secrets into GitHub issues/logs.
2. Re-enter private HTTPS subscription in app. Select one VLESS/REALITY server already working in current v2rayNG.
3. In v2rayNG use its default delay-test URL https://www.gstatic.com/generate_204. Keep same mobile carrier/Wi-Fi, location and server. Alternate test order and perform 5 rounds; note connection failures separately.
4. In Saadegi run single then three-step tests. Compare **median** and success rate; if discrepancy persists attach *redacted* Android logcat and device Android version, without link/config/passwords.
5. Grant the Android system VPN permission after the explicit experimental warning. Confirm the Android VPN status indicator, test a real HTTPS page, and disconnect from inside app. Verify internet immediately returns after stop/failure.
6. Test screen off, changing Wi-Fi/LTE, reconnect twice, DNS, IPv4+IPv6 and UDP traffic. The app must never claim CONNECTED without routed response; fail-safe must clean up TUN.
7. Confirm test speed 5 MiB only for a recently measured selected node; no parallel whole-subscription download or fallback direct. Compare measured Mbps with other clients only under equal endpoints and repeat conditions.
8. Confirm the APK size printed by CI and that its ZIP Artifact contains one arm64-specific APK. CI green is not evidence of real-device VPN stability.

## Risks still blocking production
- VPN Network discovery can differ across Android OEMs; on failure, trial closes rather than reporting a false connection.
- Native Xray TUN route, bootstrap DNS and process-lifecycle handling require real-device testing for leaks and stability. No blanket kill switch promise.
- HTTP timing includes TLS and endpoint effects; even matching URL does not guarantee same number as v2rayNG.
- Internal debug signing key from different CI runs might change; app uses separate .lab suffix to avoid overwriting existing preview; no stored subscription is migrated.
- Secrets, crash reports and subscriptions must not go to GitHub Actions logs or public repository.

CI is required, and **user device acceptance is separate**.
