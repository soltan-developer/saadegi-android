# Manual subscription-first pilot — Saadegi Android

Status: Draft PR, **not a working VPN**. User asked to defer OTP account integration until subscriptions and VPN are verified.

## Implemented in this PR
- Add or replace **one** manually entered HTTPS subscription URL. Password-visibility toggle for secure on-screen editing.
- Fetch over Android TLS stack (certificate validation); no HTTP or automatic redirects; bounded response (1 MiB), 9s connect / 14s read timeouts.
- Parse newline-separated raw proxy URIs or Base64-wrapped URI lists. Node types shown: VLESS, VMess, Trojan, Shadowsocks, Hysteria2, TUIC, WireGuard. Actual Xray compatibility cannot be inferred by scheme alone; unsupported protocols display an explicit warning.
- Derive display title and *tentative location* only from config/inbound remark (URL fragment or VMess ps). Unknown names render "نامشخص". **No direct 3x-ui panel query** in this mode; a normal subscription does not expose all inbound attributes reliably.
- Read optional standard \`subscription-userinfo\` header (\`upload\`, \`download\`, \`total\`, \`expire\`). Without that header, no invented usage or expiry is displayed.
- Persist encrypted source URL + raw profile list using Android Keystore AES-256-GCM in app-private storage. Android backup disabled. No raw URL/credentials in logs or error messages.
- Refresh and removal; keep last cached valid subscription when refresh fails. Node list scales via LazyColumn.
- Selecting a node is a **UI-only selection**, NOT a real VPN connection.

## Known restrictions
- Subscription over HTTPS only; no HTTP, forced redirects, Clash YAML, sing-box JSON, encrypted subscriptions, direct single URI import, multiple subscription sources, or subscription-specific headers yet.
- Invalid individual profiles are ignored; subscriptions without supported URI records are rejected. No guarantee that every imported URI's transport is supported by the native VPN core.
- Labels from inbounds are not authenticated geography. Domain/IP host alone is not reliable location evidence.
- Some 3x-ui generated subscriptions have no usage headers; the app cannot guess remaining traffic or expiry.
- Reading and storing profiles does not create an Android VpnService tunnel. Connecting remains disabled until audited libXray/Hev integration and real DNS/UDP/TUN device testing.
- Never send production subscription links, subscription passwords or 3x-ui tokens to public repository issues, CI logs or this chat.

## Security review before release
- Verify encrypted-store recovery on reinstall/Keystore invalidation; no raw credentials in diagnostics/logcat/crash reports.
- Test redirect refusal and HTTPS certificate rejection on-device.
- Test huge / malformed input, VMess base64, URI remarks, unsupported transports, country parsing.
- Evaluate whether source userinfo (and other provider headers) requires special subscriptions / proper content negotiation; do not silently enable HTTP.
- Probe profile compatibility with actual core capabilities (currently only coarse display marker).
- Add secure multi-subscription support if real-world users need more than one source.

## Next milestone
Wire a real native Xray and TUN engine with proper socket protection and verify traffic without leaks using a test subscription; enable connect action only after Gate-0 testing. **OTP/saadegi-site backend login is intentionally deferred.**
