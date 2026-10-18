# Saadegi Android | سادگی

اپلیکیشن اختصاصی اندروید سادگی — Kotlin + Jetpack Compose.

> **وضعیت: نمونه اولیه توسعه (غیرقابل استفاده به‌عنوان VPN).**
> این شاخه یک اسکلت اجرایی رابط کاربری و لایه‌های پایه اتصال دارد. موتور Xray، تونل TUN، ورود کاربر و API هنوز متصل نشده‌اند. دکمه VPN تعمداً غیرفعال است.

## اسناد و کد

- [راهنمای توسعه و نیازمندی ساخت](ANDROID_DEVELOPMENT.md)
- `app/src/main/java/ir/saadegi/android/MainActivity.kt` — رابط اولیه RTL و حالت روشن/تاریک
- `app/src/main/java/ir/saadegi/android/vpn/` — مرزبندی VPN، ماشین حالت و ارزیابی سرورها
- `app/src/test/java/ir/saadegi/android/vpn/` — تست‌های واحد
- `.github/workflows/android-check.yml` — ساخت و تست پیشنهادی GitHub Actions

اسناد معماری، طراحی، Mobile API و نقشه راه در [PR #1](https://github.com/soltan-developer/saadegi-android/pull/1) نگهداری می‌شوند.

## محرمانگی و مجوز

کد محصول خصوصی است. استفاده از Xray-core (MPL-2.0) از طریق wrapper رسمی libXray (MIT) گزینه ارزیابی اولیه است؛ استفاده نهایی به ممیزی تمامی کتابخانه‌ها، لایسنس‌ها و آزمون روی دستگاه واقعی وابسته است. کد GPL کلاینت‌های ثالث نباید بدون بررسی حقوقی به این اپ وارد شود.

**هنوز APK، بیلد موفق Android و VPN متصل تحویل نشده است.**
