# معماری اندروید سادگی — نسخه پیشنهادی ۱

وضعیت: **Design proposal / نیازمند آزمون و بازبینی حقوقی** — ۲۰۲۶-۱۰-۱۸  
مالک محصول: سادگی | ریپوی اپ: `soltan-developer/saadegi-android`  
سیاست: کد اختصاصی/بسته؛ هیچ فورک GPL وارد ریپو یا باینری محصول نشود.

## تصمیم‌ها (ADR-001)

1. Android native با Kotlin + Jetpack Compose، معماری Unidirectional Data Flow / MVVM.
2. موتور نخست Xray-core تحت MPL-2.0؛ ترجیح اولیه wrapper رسمی **XTLS/libXray** تحت MIT و AAR اندرویدی آن است. Adapter سادگی مستقلاً نوشته می‌شود. اگر libXray در POC ناسازگار بود، Go/Android bridge اختصاصی (بدون کپی از کلاینت GPL) گزینه دوم است؛ ساخت و تمام transitive dependencies باید ممیزی شوند.
3. تونل سیستم Android VpnService و لایه tun2socks مبتنی بر HevSocks5Tunnel (MIT) **پس از** POC عملکرد و بررسی وابستگی‌ها. مسیر مفهومی: Android TUN <-> tun2socks <-> SOCKS5 localhost <-> Xray outbound <-> server. یک هسته و یک VpnService فعال؛ بدون اجرای هم‌زمان موتورهای چندگانه.
4. از v2rayNG، PattNG، NekoBox، SFA و پروژه‌های GPL فقط برای بررسی رفتار و تست سازگاری استفاده شود؛ بدون کپی کد، asset یا کتابخانه GPL به بسته اختصاصی. کتابخانه AndroidLibXrayLite دارای LGPL-3.0 است و نباید بدون بررسی حقوقی/فنی وارد باینری شود.
5. لایه مستقل `VpnEngine` با API: `capabilities()`, `prepare(config)`, `connect()`, `disconnect()`, `observeState()`, `observeMetrics()`؛ Xray adapter نخست، جایگاه موتور جایگزین صرفاً interface و بدون کد GPL.
6. Backend همان `saadegi-site` با API ویژه موبایل و مجوز سروری مشترک؛ هیچ پنل 3x-ui، کلید مدیریت یا API داخلی پنل مستقیماً در اپ قرار نگیرد.

## ماژول‌های پیشنهادی

```text
app
core/designsystem     (tokens, components, accessibility)
core/network          (TLS, HTTP, errors, caching)
core/auth             (OTP, mobile session, refresh rotation)
core/storage          (encrypted local profiles, preferences)
core/observability    (redacted, opt-in, aggregate)
vpn/api               (engine-neutral contract)
vpn/xray              (libXray adapter / custom bridge fallback; MPL+MIT compliance)
vpn/tunnel            (VpnService, TUN/tun2socks, protect, DNS)
vpn/orchestrator      (connection state, scoring, failover)
vpn/diagnostics       (privacy-preserving checks)
feature/connect
feature/locations
feature/updates
feature/community
feature/ai-catalog
feature/orders
feature/tickets
feature/profile
```

## جریان اتصال

```text
User Connect
 -> local entitlement & cached-profile check
 -> server policy / profile refresh if reachable
 -> Android VpnService.prepare + user approval if needed
 -> protected outbound sockets, TUN, local SOCKS, Xray
 -> handshake + scoped connectivity test
 -> CONNECTED only on successful verification
 -> periodic passive health + conditional measured probes
 -> reconnect/failover with capped backoff and user-visible status
 -> DISCONNECTED on user action/revocation/fatal error
```

State machine: `IDLE → PREPARING → CONNECTING → VERIFYING → CONNECTED → RECONNECTING`; terminal `DISCONNECTED`, `ERROR`; `DISCONNECTING` for explicit teardown. State transitions serialized with a mutex/actor to prevent double-connect and zombie processes. Never report connected solely because VPN permission was granted.

## انتخاب هوشمند و سنجش

- انتخاب خودکار باید با «امتیاز کیفیت قابل اندازه‌گیری» صورت بگیرد: موفقیت probe/handshake، درصد خطا، p50/p95 latency، پایداری recent sessions، خسارت ناشی از تغییر مسیر؛ weighted policy قابل پیکربندی و versioned.
- جلوگیری از نوسان با hysteresis + cooldown؛ انتخاب دستی کشور بر انتخاب خودکار اولویت دارد.
- فقط پروفایل‌های واقعاً مجاز کاربر، در نسخه موتور فعلی و با protocol/transport شناخته‌شده وارد candidate pool شوند.
- health endpoint نباید اطلاعات محرمانه، جغرافیای دقیق یا تاریخچه مرور کاربر را دریافت کند.
- هنگام شکست همه مسیرها، خطا و اقدام روشن نشان داده شود؛ ادعای خودکار بودن یا «همیشه متصل» ممنوع.

## مسیرهای شبکه و DNS

- Android VpnService تنها مسیر ترافیک تحت پوشش است؛ مدیریت foreground lifecycle و onRevoke الزامی.
- socketهای Xray و probe باید مطابق API سیستم از VPN محافظت شوند تا loop ایجاد نشود.
- IPv4/IPv6، UDP، DNS، split tunnel اختیاری و تغییر Wi-Fi/LTE به‌صورت جداگانه تست شوند.
- DNS leakage، IPv6 leakage، captive portal، airplane mode، reboot، process death و battery restrictions آزموده شوند.
- Always-on / lockdown فقط با فعال‌سازی آگاهانه کاربر از امکانات سیستم‌عامل؛ اگر lock-down فعال است، هرگز مخفیانه fallback مستقیم نکنیم.
- fail closed وقتی کاربر «جلوگیری از نشت ترافیک» را انتخاب کرده؛ خطر قطع اتصال اینترنت در UI بیان شود.

## مدل Subscription و پروفایل

- `/api/mobile/v1/vpn/entitlement` وضعیت مجاز بودن، اشتراک فعال/رزرو/منقضی و محدودیت سروری را می‌دهد.
- `/api/mobile/v1/vpn/profiles` فقط پروفایل‌های مجاز کاربر را بعد از اعتبارسنجی دسترسی به VPN تولید می‌کند. credentials هر profile فقط در کانال TLS و storage رمزگذاری‌شده؛ هر داده‌ای که به دستگاه می‌رسد بالقوه برای مالک دستگاه قابل بازیابی است.
- versioned profile schema و capability negotiation؛ اپ کانفیگ ناشناخته را رد می‌کند نه اینکه در موتور ارسال کند.
- پرداخت و کیف پول از APIهای فعلی سایت؛ اپ منطق مالی تکراری ایجاد نمی‌کند.
- خاموش‌کردن دسترسی VPN برای کاربر در سایت باید در API و refresh مجوز اپ نیز اعمال شود؛ مخفی‌شدن منو به معنی کنترل امنیتی نیست.
- حالت قطع اینترنت: پروفایل مجاز cache شده می‌تواند بر اساس سیاست روشن سرور و انقضای آن کار کند؛ لغو فوری دسترسی در نبود شبکه تضمین‌پذیر نیست، پس TTL کوتاه و اعمال revoke در سمت سرور لازم است.

## حریم خصوصی و امنیت

- توکن موبایل مستقل از Session Cookie وب، با refresh rotation و device-scoped revocation؛ Android Keystore برای نگهداری کلید و encrypted-at-rest.
- OTP و secrets در Logcat، crash reports، analytics، سیستم لاگ RPC و خطاها ممنوع.
- امنیت metadata/config، TLS، source integrity، dependency SBOM، reproducible/pinned builds و امضای release در pipeline بررسی شود.
- ابزار عیب‌یابی فقط با رضایت صریح کاربر logهای پاک‌سازی‌شده را ارسال کند؛ raw URLs، کلیدها، IP مقصد کاربران و سابقه مرور ارسال نشود.
- از APIهای سیستم‌عامل و public SDK استفاده شود؛ دسترسی روت یا Accessibility Service برای اجرای VPN لازم نیست.

## ریسک‌های تعیین‌کننده پیش از توسعه کامل

R1: اثبات ساخت/استفاده از libXray Android AAR با نسخه pin‌شده، یک Go runtime در هر process و integration معتبر TUN/UDP/DNS.  
R2: ممیزی dependency tree و الزامات MPL/MIT/LGPL/GPL توسط مسئول حقوقی.  
R3: آزمون واقعی VLESS/Reality/XHTTP با پنل فعلی و ناسازگاری‌های Transport.  
R4: نرخ تغییر شبکه و محدودیت‌های background/foreground در Android 16+.  
R5: عدم دسترسی API سایت هنگام اختلال اینترنت؛ bootstrap و fallback امن باید آزموده شود.

مرجع اصلی: https://github.com/XTLS/Xray-core | https://github.com/heiher/hev-socks5-tunnel | https://www.mozilla.org/en-US/MPL/2.0/FAQ/ | https://developer.android.com/reference/android/net/VpnService
