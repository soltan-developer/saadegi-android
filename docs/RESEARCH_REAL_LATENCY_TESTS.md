# بررسی سورس کلاینت‌های معروف: تست تأخیر واقعی (۲۰۲۶-۱۰-۱۹)

فقط رفتار فنی بررسی شده است، کد GPL آن‌ها به اپ اختصاصی سادگی کپی نشده است.

## v2rayNG
- [RealPingWorkerService](https://github.com/2dust/v2rayNG/blob/master/V2rayNG/app/src/main/java/com/v2ray/ang/service/RealPingWorkerService.kt)
- [SpeedtestManager](https://github.com/2dust/v2rayNG/blob/master/V2rayNG/app/src/main/java/com/v2ray/ang/handler/SpeedtestManager.kt)
- برای بعضی پروتکل‌های TCP ابتدا زمان برقراری سوکت به host:port را غربال می‌کند؛ سپس از Libv2ray.measureOutboundDelay(config, testUrl) برای سنجش واقعی outbound استفاده می‌کند. برای Hysteria2، WireGuard، HTTP/3 و بعضی تنظیمات پیچیده precheck TCP حذف می‌شود. Coroutine concurrency قابل تنظیم است.
- نتیجه: TCP connect time معادل سنجش کیفیت کانفیگ نیست و نباید با آن برچسب Real Delay زد.

## Mihomo (Clash Meta)
- [healthcheck.go](https://github.com/MetaCubeX/mihomo/blob/Meta/adapter/provider/healthcheck.go)
- [urltest.go](https://github.com/MetaCubeX/mihomo/blob/Meta/adapter/outboundgroup/urltest.go)
- HealthCheck با URLTest از مسیر پراکسی، timeout، کنترل نرخ موازی تا ۱۰ و دوره/حالت lazy کار می‌کند. URLTest group تاریخچه آخرین تأخیر و Alive flag دارد و فقط وقتی سرور بهتر از tolerance باشد خودکار تغییر می‌دهد.
- نتیجه: hysteresis/tolerance برای جلوگیری از نوسان و قطع مکرر اهمیت دارد.

## Hiddify
- [active_proxy_notifier.dart](https://github.com/hiddify/hiddify-app/blob/main/lib/features/proxy/active/active_proxy_notifier.dart)
- [active_proxy_delay_indicator.dart](https://github.com/hiddify/hiddify-app/blob/main/lib/features/proxy/active/active_proxy_delay_indicator.dart)
- درخواست URL Test از هسته، نمایش Timeout جدا از حالت VPN Service، throttle برای لمس کاربر و reconnect به stream موتور با backoff.
- نتیجه: وضعیت روشن‌بودن تونل با موفقیت واقعی درخواست یکی نیست.

## sing-box
- [common/urltest/urltest.go](https://github.com/SagerNet/sing-box/blob/testing/common/urltest/urltest.go)
- [protocol/group/urltest.go](https://github.com/SagerNet/sing-box/blob/testing/protocol/group/urltest.go)
- HTTP HEAD با detour دایلر همان outbound؛ URL پیش‌فرض https://www.gstatic.com/generate_204. Multiplex warm-up در صورت نیاز، سنجش موازی تا ۱۰، تاریخچه و انتخاب auto با tolerance (پیش‌فرض ۵۰ms).
- نتیجه: HTTP-probe واقعی + کنترل timeout + tolerance از raw TCP ping بهتر است.

## libXray در نسخه اختصاصی سادگی
- [upstream README](https://github.com/XTLS/libXray/blob/main/README.md)
- [invoke_model.go](https://github.com/XTLS/libXray/blob/main/invoke_model.go)
- pingBatch با apiVersion=3 حداکثر ۵ Xray outbound را با HTTP HEAD (timeout پیش‌فرض قابل تنظیم) از مسیر همان outbound می‌سنجد؛ پاسخ شامل success و delay و error است. مقادیر 10000 و 11000 برای خطا/timeout نشانه ویژه‌اند و نباید latency موفق تلقی شوند.
- managed pingBatch و managed runXray با هم اجرا نمی‌شوند؛ قفل طول عمر موتور باید محترم باشد.

## طراحی انتخابی سادگی
1. یک‌مرحله‌ای: یک HTTP HEAD از مسیر Xray برای سنجش سریع.
2. سه‌مرحله‌ای: سه درخواست مستقل؛ median، range=max-min به‌عنوان «نوسان» و success ratio برای پایداری.
3. رتبه‌بندی: نرخ موفقیت > امتیاز پایداری > میانه تأخیر؛ نمونه ناموفق هیچ‌وقت صفر میلی‌ثانیه نیست. یک تست به‌تنهایی ثبات را ثابت نمی‌کند.
4. هم‌زمانی محدود: هر batch حداکثر ۵ کانفیگ؛ برای تجربه مناسب کاربر سقف نخستین ۲۰ سرور فیلترشده در هر نوبت.
5. دانلود سرعت ۵ MiB: فقط از SOCKS مربوط به کانفیگ تست‌شده؛ اتصال مستقیم جایگزین ممنوع. زمان‌سنجی با clock monotonic و شمارش بایت واقعاً دانلودشده؛ سهمیه کاربر مصرف می‌شود.
6. تست‌ها با URL یکسان و در یک شبکه مقایسه شوند. نتیجه هیچ probe واحدی تضمین سرعت واقعی در تمام سرویس‌ها نیست.
7. در آینده: history expiry/cache، آزمون چند endpoint و آزمون جداگانه در حالت VPN متصل.

**وضعیت:** طراحی و کد اولیه انجام شده‌اند، بیلد GitHub به‌تنهایی به معنای اثبات صحت ارتباط واقعی روی دستگاه نیست.
