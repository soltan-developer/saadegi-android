# معیارهای پذیرش، امنیت و انتشار

این فایل معیارهای Gate را تعیین می‌کند و **ادعای اجرای تست نیست**.

## اتصال و چرخه حیات

- [ ] یک VPNService فعال، 10 بار connect/disconnect تکراری بدون zombie service یا tunnel leak.
- [ ] VPN permission رد/ابطال شود: حالت درست و خطای مفهوم؛ onRevoke teardown.
- [ ] Wi-Fi -> mobile data -> Wi-Fi؛ airplane mode؛ screen lock؛ power saving؛ Android process death؛ reboot.
- [ ] sign-in/switch-user/logout و VPN entitlement revoke بدون نمایش config کاربر دیگر.
- [ ] حالت auto و manual: country selection respect؛ عدم سوئیچ مکرر؛ احترام به opt-in always-on.
- [ ] active/reserved/expired/hide/denied server-side رفتار درست و نمایش مورد انتظار داشته باشند.

## پروتکل و مسیر شبکه

- [ ] نمونه واقعی VLESS+Reality و transports موجود پنل با مشخصات سرور.
- [ ] TCP، UDP، DNS، IPv4/IPv6 و WebRTC/DNS leak در سناریوی تمام‌تونل قابل پوشش.
- [ ] مسیریابی DNS، protected sockets و loop prevention، captive portal و Kill-Switch OS مورد بررسی.
- [ ] throughput و connection RTT/battery در دستگاه واقعی با baseline v2rayNG مقایسه شوند، با شرایط برابر.
- [ ] Core upgrade regression و profile version compatibility test پیش از انتشار هر هسته جدید.

## امنیت و حریم خصوصی

- [ ] یک کاربر نمی‌تواند API/اشتراک/تیکت کاربر دیگر را دریافت کند (IDOR/authorization tests).
- [ ] OTP brute force, device refresh rotation, token replay, session revocation و rate limiting آزموده شود.
- [ ] no raw key / subscription URL / 2FA secret / OTP / ticket attachments در analytics/log/crash.
- [ ] network config input validation, injection, zip-bomb/file handling, safe links and WebView hardening.
- [ ] SBOM و license report برای همه direct و transitive dependencies؛ MPL notices و source availability برای اجزای MPL.
- [ ] update signing و reproducible build metadata؛ APK hash و staged rollout.
- [ ] privacy disclosure واضح برای کاربر و رضایت صریح برای ارسال diagnostics.

## UX و کیفیت

- [ ] رابط فارسی RTL؛ 360dp تا 840dp، Android font scaling 2x، landscape و TalkBack.
- [ ] حالت روشن/تاریک با متن خوانا، کنتراست AA و min touch target 48dp.
- [ ] UI هنگام network timeout آفلاین freeze نکند؛ feed و ticket روی VPN lifecycle اثر نگذارند.
- [ ] هر status کارت محتوای متفاوت، متن اقدام‌پذیر، وضعیت loading/error/empty و fallback درست دارد.
- [ ] در feed: گزارش تخلف/حذف/ویرایش/پاسخ/نظرسنجی، محافظت اسپم و moderation.
- [ ] در support: ارسال پیام/پیوست امن، optimistic rollback، عدم اعلان خودفرستاده.
- [ ] بدون ایجاد حس اتصال موفق قبل از tunnel + handshake + check.

## سیاست انتشار

- [ ] شرایط انتشار Google Play برای VPNService، privacy policy، Billing و target API دوباره در روز انتشار بررسی شوند.
- [ ] تست internal قبل از closed beta؛ گزارش crash-free sessions، موفقیت اتصال، نرخ reconnect و error codes.
- [ ] انتشار بیرون Play فقط از کانال رسمی با امضای پایدار و راهنمای نصب/به‌روزرسانی امن.
- [ ] مجوز و تصمیم حقوقی کتبی پیش از عرضه نسخه proprietary نهایی.

منابع سیاست روز طرح:
- https://support.google.com/googleplay/android-developer/answer/16558241
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://developer.android.com/reference/android/net/VpnService
