# Direct Xray native Android TUN — Gate 0 experimental

Status: **source-only POC, neither connected nor user-tested.** This code does not enable VPNService in AndroidManifest, does not wire any UI button, does not claim a working tunnel, and must not be merged as production functionality.

## Verified upstream facts

- Xray-core v26.10.17 implements Android TUN in proxy/tun/tun_android.go and requires a VpnService-provided FD via root env.xray.tun.fd.
- XTLS/libXray v26.10.17 release ships armv7, arm64, x86, x86_64 Android JNI and the generated DialerController#protectFd(long), LibXray.registerDialerController, setDNS, resetDNS, Invoke API version 3.
- Release downloaded/sha256 verified by CI prior to packaging.
- This approach can **potentially avoid** a separate tun2socks library, but compatibility, packet handling and DNS safety on devices still require proof.

## Scaffold only

NativeTunConfigFactory creates TUN inbound with explicit default outbound and fd; NativeTunGateZero requests a VpnService builder, registers protected sockets and bootstrap DNS, starts managed Xray, handles failure/teardown.

The service remains manifest-disabled and this helper is not invoked from the UI. It does not implement lifecycle state, foreground notifications, cancellation, actual connectivity verification or reconnection.

## Required before unlocking CONNECT

1. Gate-0 start on a REAL Android device with user consent via VpnService.prepare and foreground service according to current target API rules.
2. Protect all egress sockets; verify no routing loop when Wi-Fi ↔ LTE changes; VPNService.onRevoke must tear down Core, reset DNS and close fd.
3. Check DNS, IPv6, UDP, device network lockdown, route and WebRTC leaks on two phones/operators.
4. Use the exact profile from the encrypted subscription; handle VLESS Reality + XHTTP and any protocol admitted by core, no silent downgrades.
5. After NativeXray is started, send a routed HTTP request and verify public exit address against known direct baseline. Only then switch the connection state to CONNECTED.
6. Configure dedicated verified connected-network speed test. Existing diagnostics test uses temporary managed core and **must not run concurrently with active VPN**.
7. Load-test frequent connect/disconnect, cold launches, Android background restrictions and battery consumption. Rate-limit reconnect; tear down on errors.

Official source:
- https://github.com/XTLS/Xray-core/blob/main/proxy/tun/tun_android.go
- https://github.com/XTLS/Xray-core/blob/main/proxy/tun/README.md
- https://github.com/XTLS/libXray/blob/main/README.md
