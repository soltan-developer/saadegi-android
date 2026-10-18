# طراحی محصول و Design System سادگی — پیشنهاد

وضعیت: طرح محصول، نه فایل‌های گرافیکی نهایی. رویکرد: Apple-level polish + Android-native behavior؛ کپی ظاهری iOS ممنوع. زبان اصلی فارسی RTL، حالت روشن و تاریک برابر در کیفیت.

## هویت بصری واقعی سایت

بر اساس CSS موجود سایت در ۱۸ اکتبر ۲۰۲۶:
- Light background: `#FAF9F5`; foreground: `#1F1E1D`; surface: `#FFFFFF`
- Primary brand terracotta: `#D97757`; dark brand/accent: `#B84A26`
- Dark background: `#262624`; dark surface: `#2D2D2A`; warm light accent: `#E8A87C`
- Warm neutrals: `#F1EDE4`, `#E8E4D9` and dark border `#45443E`

`#D97757` با نوشته سفید نسبت حدود 3.12:1 دارد؛ برای متن کوچک روی دکمه روشن مناسب نیست. برای دکمه‌های دارای نوشته سفید از `#B84A26` (تقریباً 5.19:1) استفاده شود. نارنجی روشن بیشتر برای تزئین و نمودار، نه تنها نشانه وضعیت.

## Principles

1. Connect-first: صفحه اصلی تنها یک CTA بزرگ برای اتصال؛ جزئیات فنی در Advanced.
2. Calm by default: بدون تبلیغ پاپ‌آپی هنگام Connect؛ اخبار در کارت‌های ثانویه.
3. Honest states: «متصل» فقط پس از verify؛ خطاها قابل فهم و قابل اقدام.
4. Native motion: 160–260ms برای micro-interactions، 220–360ms برای navigation، سیستم reduce-motion محترم.
5. Touchable: touch targets حداقل 48dp، typography مقیاس‌پذیر، font Vazirmatn با مجوز فونت رعایت‌شده.
6. Accessibility: کنتراست WCAG AA برای متن، focus/semantics، TalkBack، RTL، landscape، large font.
7. Performance: blurهای سنگین، شیشه‌ای‌سازی سراسری، backdrop animation دائمی و لایه‌بندی‌های پرمصرف ممنوع.
8. Distinct identity: rounded components، subtle elevation، translucent nav bar محدود، فاصله‌گذاری generous و microcopy دوستانه.

## Navigation و مجوزها

پنج تب پایین:
- «اتصال» (صفحه اصلی VPN)
- «مجله» (اخبار، آموزش‌ها، کمپین‌ها، واکنش/نظرات)
- «هوش مصنوعی» (فروشگاه و راهنمای انتخاب ابزار)
- «پشتیبانی» (تیکت‌های سایت)
- «حساب من» (اشتراک‌ها، سفارش‌ها، کیف پول، تنظیمات)

در حساب‌های بدون دسترسی VPN، تب اول «خانه» می‌شود و بخش اتصال اصلاً نمایش داده نمی‌شود. این صرفاً UX است و کنترل مجوز باید سروری باشد. اخبار عمومی بدون نیاز به ورود قابل خواندن باشند و تعامل/تیکت نیازمند ورود.

## صفحه ۱: اتصال

```text
┌──────────────────────────────┐
│ سادگی                  ◯ اعلان│
│   تحریم‌گذر                   │
│                              │
│        [ Power Ring ]        │
│        «آماده اتصال»          │
│                              │
│ [ هوشمند ▾ ]  [ کشور ▾ ]      │
│ کیفیت: «در حال سنجش»          │
│                              │
│ [اشتراک][حجم مانده][انقضا]     │
│                              │
│ آخرین اطلاعیه / باز کردن      │
│ اتصال | مجله | AI | تیکت | حساب│
└──────────────────────────────┘
```

- فشردن Power: بازخورد haptic محدود، حلقه progress با state واقعی، آموزش دسترسی VPN اولین بار.
- خطای DNS یا انقضا: توضیح فارسی و CTA مشخص، نه کد خطای خام.
- انتخاب کشور Sheet پایین با جست‌وجو، نام کشور و وضعیت واقعی؛ پرچم فقط در صورت قابل اتکا بودن.
- وضعیت «بهترین» فقط وقتی تست معتبر انجام شده است، نه صرفاً کمترین ping.
- Advanced: پروفایل دستی در صورت مجوز، app split tunneling اختیاری و diagnostics با رضایت.

## صفحه ۲: مجله

Feed card با تصویر مناسب و placeholder استاندارد؛ چهار دسته: اطلاعیه، آموزش، آفر/کمپین، وضعیت سرویس. قابلیت reaction، threaded comments، report، moderation، poll؛ ارسال تصویر/فایل کاربر خارج از MVP. تعامل باید optimistic با rollback و آمار واقعی باشد. اعلان‌های خوانده‌نشده بر اساس سرور و event ID.

## صفحه ۳: هوش مصنوعی

Product cards بر اساس داده سایت: قیمت/موجودی/توضیحات، راهنما، مقایسه و دکمه خرید. «پیشنهاد ابزار» ابتدا rule-based/curated و فقط پس از صحت‌سنجی توصیه‌ها به assistant. اطلاعات تحویل اکانت بدون ثبت در analytics/crash logs.

## صفحه ۴: پشتیبانی

فهرست تیکت‌های همان حساب، فیلتر باز/بسته/خوانده‌نشده، صفحه گفت‌وگوی مرتب، فایل خصوصی با URL موقت و احراز هویت؛ ادامه مکالمه سایت بدون کپی دیتابیس. بدنه پاسخ و فایل حساس در اعلان پوش نشان داده نشود.

## صفحه ۵: حساب

ورود شماره + OTP، خروج امن، مدیریت دستگاه‌ها، اعلان‌های ترجیحی، کیف پول و سفارش‌ها، اشتراک VPN فعال/رزرو، مجوزها و «ارسال گزارش عیب‌یابی» با رضایت صریح.

## حالت‌ها و Motion

- IDLE: ring static, label «برای اتصال لمس کنید».
- CONNECTING: soft determinate/indeterminate progress, label «در حال برقراری اتصال».
- VERIFYING: «بررسی کیفیت اتصال»؛ هنوز connected badge نشان نده.
- CONNECTED: state distinctly green + text «متصل»؛ timer real.
- RECONNECTING: amber neutral with retry progress؛ بدون نمایش اتصال موفق.
- ERROR: icon + actionable text + «تلاش دوباره/تغییر کشور».
- DISABLED: در صورت لغو مجوز/انقضا، مسیر خرید یا تمدید روشن.

## نمونه‌های عملی Design QA

- تست 360dp، 393dp، 600dp و تبلت 840dp، portrait/landscape.
- فارسی بلند و عدد لاتین در کارت‌ها overflow نکند.
- فونت سیستم ×1.5 و ×2 بدون قطع متن.
- تیکت طولانی، کیبورد باز، گشتاور gesture back و صفحه دارای inset.
- Dark mode واقعی: backgrounds warm neutral، متن خوانا، نارنجی تزئینی به‌جای تکیه بر رنگ برای انتقال حالت.

مراجع: https://developer.android.com/develop/ui/compose/designsystems/material3 | https://developer.apple.com/design/human-interface-guidelines/motion
