# Saadegi Android - connection troubleshooting (internal APK)

This internal build is NOT production ready. It is based on the user's real-world observation that v2rayNG reported about 200 ms while Saadegi showed about 1300 ms and VPN did not connect.

## In-app diagnostic workflow
1. Install the latest ARM64 internal APK, open the VPN tab and select one subscription node.
2. Grant system VPN permission and attempt one connection.
3. Tap «گزارش اتصال و خطاها» directly under the main connect card.
4. Tap «کپی گزارش». Review before sharing. The bounded in-memory log includes only time and predefined stage codes, never subscription URL, node hostname, IP, password, raw Xray error text, stack trace or 2FA secrets.
5. Typical phases: PROFILE_LOAD -> CONVERT -> TUN_PERMISSION -> TUN_ESTABLISH -> XRAY_START -> VERIFY_BEGIN -> VERIFIED. The last completed phase identifies the failure boundary.
6. If DNS/traffic verification fails, the service tries to tear down the TUN rather than claiming connected.

## Fair delay measurement
- Upstream source: https://github.com/XTLS/libXray/blob/main/xray/ping_batch.go
- `pingBatch` creates and then closes an Xray core per call, and uses `DisableKeepAlives`. A warm-up in the preceding batch does not warm the subsequent measured batch, so the ineffective warm-up was removed.
- The reported measurement is an HTTP HEAD response time **via the Xray outbound**, not ICMP/TCP ping. Fresh connections and TLS setup influence it.
- Select Google 204 or Cloudflare 204 explicitly. Set the same URL in v2rayNG and Saadegi on the same phone/network, alternate test order, and compare multiple rounds.
- Three-stage result shows median and variation across independently initiated requests. A failure is never a fake 0 ms result.
- Cancel button interrupts at the next native batch boundary, not necessarily mid-flight inside native Go.

## Package size / typography
- ARM64-only APK, R8 and resource shrinking, native library compression. CI prints actual APK byte size.
- A smaller compressed APK does not imply less storage after installation; the Go native library is extracted.
- Persian interface uses pinned Vazirmatn from Google Fonts under SIL OFL 1.1 with an embedded notice.

## Release blockers
- Source compilation success is not proof the VPN works on the physical phone. Check a real tunneled HTTPS request plus DNS, IPv6, UDP, sleep/restart and Wi-Fi/LTE changes.
- Before public release: verify license obligations, compatibility, crash-free sessions and privacy policy.
- Do not copy real subscription links or tokens into the public GitHub repo.
