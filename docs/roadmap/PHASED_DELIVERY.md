# نقشه راه اجرایی Saadegi Android — مرحله‌ای و دارای Gate

وضعیت: پیشنهادی؛ اجرای کد و تست واقعی انجام نشده است. هر فاز خروجی قابل مشاهده، تست و معیار خروج دارد. توسعه در branch/PRهای کوچک؛ هیچ merge/deploy خودکار.

## Gate 0: حقوقی + POC ریسک اتصال (اولویت مطلق)

- ممیزی مجوز Xray-core (MPL-2.0)، HevSocks5Tunnel (MIT)، Go bridge و کل dependency tree؛ ثبت SBOM و notice.
- ساخت/استفاده از AAR رسمی libXray (MIT) برای Xray-core (MPL-2.0) با نسخه و hash pin‌شده؛ Adapter اندروید اختصاصی. در صورت عدم سازگاری، bridge خودنوشته مستقل، نه کپی AndroidLibXrayLite یا v2rayNG.
- ساخت ساده VpnService + local SOCKS + TUN/tun2socks؛ اثبات forwarding TCP، UDP، DNS، IPv4 و IPv6.
- آزمایش حداقل VLESS+REALITY و transportهای واقعاً مورد استفاده در 3x-ui؛ XHTTP فقط وقتی در نسخه انتخابی Xray و سرور موجود پشتیبانی تاییدشد.
- تست روی 2 برند گوشی و 2 شبکه واقعی؛ چرخه قطع/وصل، sleep، تغییر شبکه؛ مستندسازی throughput, RTT, battery, crash.
- **خروج:** اتصال پایدار در دستگاه واقعی + ممیزی مجوز قابل قبول. اگر نشد: معماری بازبینی می‌شود، UI را علت شکست قرار ندهیم.

## Gate 1: پایه پروژه و Design System

- Gradle Kotlin DSL؛ build flavor internal/release؛ package ID نهایی قبل از انتشار تثبیت شود؛ compile/target API مطابق سیاست انتشار روز.
- Compose navigation, DI ساده، network stack، crash-safe storage، startup health، release signing از secret manager؛ نه داخل Git.
- اجزای design: colors, typography, spacing, shapes, haptics, motion, skeleton, error/empty states.
- PoC screen: one-tap connect + status + country picker + protected switch.
- **خروج:** build تکرارپذیر، تست UI کوچک/بزرگ، cleartext disabled، binary بدون secret.

## Gate 2: Mobile API سایت و اشتراک‌ها

- در `saadegi-site` به‌شکل PRهای جدا و کوچک: `/api/mobile/v1`، reuse user/OTP، mobile sessions، refresh rotation، RBAC/entitlement.
- اتصال profiles generated server-side به user و 3x-ui؛ نسخه‌دار، حداقل secret و محدودیت نرخ.
- APIهای my-subscriptions و usage با اعتبارسنجی و cache؛ محاسبه مصرف و انقضا از منبع سرور.
- لینک موجود ticket و order و wallet را از طریق adapter استفاده کنیم؛ قرارداد سایت فعلی را نشکنیم.
- **خروج:** کاربر واقعی فقط داده خودش را دریافت کند؛ لغو مجوز سمت سرور enforce؛ logout همه tokenهای دستگاه را باطل کند.

## Gate 3: VPN تولیدی و الگوریتم انتخاب هوشمند

- پروفایل دستی/خودکار، انتخاب کشور، health probes، memory of last good، backoff + cooldown.
- مدیریت reboot/foreground/background/Android lockdown و automatic reconnection طبق مجوز سیستم.
- تست security/performance matrix؛ خطاهای قابل اقدام.
- **خروج:** تست‌های طولانی پایداری و شبکه؛ بدون DNS/IPv6 leak در سناریوهای سیاست اعلام‌شده.

## Gate 4: اخبار، تیکت و فروشگاه

- خوراک عمومی اخبار، آموزش‌ها، کمپین‌ها، status؛ cache آفلاین public-content و deep link.
- تیکت مستقیم سایت: create/reply/attachments/status/read receipts؛ داده خصوصی بدون cache ناامن.
- مدیریت سفارش‌ها، اکانت‌های تحویل‌شده، راهنما، فروشگاه محصولات هوش مصنوعی.
- **خروج:** تجربه end-to-end سایت/موبایل یکسان؛ خرید دوباره/پیام تکراری/اعلان اشتباه ندارد.

## Gate 5: تعاملات و رشد

- نظرات، واکنش‌ها، نظرسنجی، گزارش تخلف، moderation، rate limit و ضداسپم.
- اعلان‌های هدفمند opt-in؛ segment براساس ترجیح کاربر، و پرهیز از افشای اطلاعات حساس.
- AI discovery بر مبنای catalog واقعی؛ از وعده‌های غیرمستند درباره قابلیت محصولات جلوگیری شود.
- **خروج:** moderation console عملیاتی، سوءاستفاده و حذف/ویرایش قابل کنترل، consent تست‌شده.

## Gate 6: آماده‌سازی انتشار

- Android 16/target 36 برای Google Play پس از 31 اوت 2026 (در زمان تهیه طرح) و سازگاری نسخه‌های قدیمی هدف؛ مطالعه دوباره سیاست روز پیش از آپلود.
- VPNService disclosure، رمزگذاری تونل، privacy policy، Data Safety، signing/rollout/canary، به‌روزرسانی امن.
- پرداخت دیجیتال در Play تابع سیاست Billing است؛ مسیر پرداخت سایت را بدون بررسی سیاست به درون Play منتقل نکنیم.
- توزیع APK خارج Play: امضای نسخه‌ها، hash/verification و anti-tamper update metadata.
- rollout محدود با Crash-free sessions و نرخ اتصال واقعی؛ rollback plan و support escalation.
- **خروج:** بازبینی حقوقی و امنیتی، QA signoff، Release Candidate با build قابل بازتولید.

## اولویت توسعه

اول کیفیت و پایداری VPN، بعد حساب/تیکت/خبر، سپس خرید و تعاملات اجتماعی. توسعه رابط باید هم‌زمان با PoC VPN، اما مستقل از آن پیش برود؛ هیچ feature جدیدی نباید lifecycle اتصال را وابسته به feed، ads یا ticket کند.

## قوانین ادغام با saadegi-site

- اپ فقط در `saadegi-android`؛ Endpointهای جدید سایت PR مستقل به `saadegi-site`.
- از فایل‌های مشترک `server/rpc.mjs`, `src/api/client.ts` و `server/lib/settings.mjs` در توسعه موازی سایت بدون هماهنگی تغییر پرحجم انجام ندهیم.
- migrationهای دیتابیس سایت append-only + tested، دسترسی VPN مطابق `server/lib/vpn-access.mjs`.
- هشدار: server/rpc.mjs در خطا args را برای بسیاری از روش‌ها log می‌کند؛ هیچ credential/token پروفایل از مسیر RPC قدیمی بدون redaction کامل رد نشود.
- قبل از هر merge build، lint، unit، integration و security regression ضروری است.
