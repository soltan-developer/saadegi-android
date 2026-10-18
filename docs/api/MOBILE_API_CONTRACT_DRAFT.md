# قرارداد پیشنهادی Mobile API v1 — فقط طراحی

مخزن مسئول پیاده‌سازی API: `saadegi-site`. **هیچ‌کدام از endpointهای زیر هنوز تأیید یا پیاده‌سازی نشده‌اند.** UI فعلی سایت در `/api/rpc` کار می‌کند؛ این طرح یک façade موبایلی نسخه‌دار است، نه تعویض فوری RPC.

## توافق‌های عمومی

- Base path: `/api/mobile/v1`
- JSON UTF-8؛ HTTPS اجباری؛ versioned response `schemaVersion`؛ شناسه درخواست `requestId`.
- Auth: دسترسی کوتاه‌عمر و refresh چرخشی device-bound، مستقل از cookie session وب؛ Android Keystore.
- Error: `{ "code":"...", "message":"...", "requestId":"..." }` بدون raw config، token یا شناسه پنل.
- Pagination: cursor-based + fixed max page size. Requests mutating از Idempotency-Key استفاده کنند.
- استفاده از customer-facing role policy و limit؛ endpointهای Admin پنل هرگز مستقیم به موبایل داده نشوند.

## Auth

- `POST /auth/otp/request` : phone, deviceHint؛ پاسخ بدون اطلاعات اینکه حساب وجود دارد یا نه.
- `POST /auth/otp/verify` : phone, code, deviceId؛ برگرداندن mobile access/refresh و user summary.
- `POST /auth/refresh` : refresh rotation، revoke previous.
- `POST /auth/logout` : revoke device session.
- `GET /me` : basic profile, canUseVpn, notification prefs؛ نسبت‌دهی شماره فقط از session.
- `GET /me/devices` و `DELETE /me/devices/{id}` برای مدیریت دستگاه‌ها.

## VPN

- `GET /vpn/entitlement` : enabled, reason, active/reserved subscriptions, entitlement version, server timestamp.
- `GET /vpn/profiles?capabilities=...` : فهرست profileهای مجاز، version، checksum/signature, expiry, country, engine requirements, protocol/transport capabilities, priority و config protected. هر profile به user, subscription, compatible-engine مرتبط است.
- `GET /vpn/locations` : کشور و node IDs قابل نمایش؛ بدون ادعای availability لحظه‌ای در صورت نبود probe.
- `POST /vpn/diagnostics` : فقط نمونه پاک‌سازی‌شده و opt-in؛ rate limit.
- `POST /vpn/feedback` : کیفیت اتصال، نوع خطا و carrier class اختیاری با رضایت.
- نشانی panel و tokenهای مدیریتی در هیچ پاسخ موبایل نباشند. Secret کانفیگ دستگاه قابل استخراج بالقوه است؛ revoke سمت سرور ضروری.

## محتوا و جامعه

- `GET /posts?type=announcement|tutorial|offer|incident&cursor=...`: محتوای عمومی/شخصی با permission.
- `GET /posts/{id}`, `POST /posts/{id}/reactions`, `GET/POST /posts/{id}/comments`؛ nested replies.
- `POST /comments/{id}/report`، `GET /notifications`, `POST /notifications/read`.
- comment/report با moderation، spam limits، HTML sanitation و content ownership؛ هیچ دسترسی Admin از موبایل.

## پشتیبانی و فروشگاه

- `GET/POST /tickets`, `GET /tickets/{id}`, `POST /tickets/{id}/messages`, `POST /tickets/{id}/read`؛ فایل خصوصی از URL کوتاه‌عمر و مجوز.
- `GET /catalog/products`, `GET /catalog/products/{id}`, `GET /orders`, `GET /subscriptions`, `GET /wallet/summary`.
- خرید/پرداخت و جزئیات تحویل با policy دقیق و متناسب با Play Billing در روش توزیع رسمی؛ API فعلی سایت source-of-truth بماند.

## داده نمونه وضعیت VPN

```json
{
  "schemaVersion": 1,
  "enabled": true,
  "serverTime": "2026-10-18T00:00:00Z",
  "entitlement": {"status": "active", "subscriptionId": "example-only", "expiresAt": "2026-11-18T00:00:00Z"},
  "profilesVersion": "example-v1",
  "supportedLocations": [{"id": "de", "titleFa": "آلمان"}]
}
```

این JSON صرفاً نمونه قراردادی است، نه پاسخ واقعی سرور.

## سیاست سازگاری

- `minAppVersion` و `requiredEngineCapabilities` روی profile، به‌جای تشخیص از نام پروتکل.
- unknown protocol => explicit unsupported UI، بدون اتصال خام و بدون silent downgrade.
- API جدید feature flag با staged rollout. Api contract integration tests قبل از انتشار اپ.
- افزوده شدن site-wide notification و community tables نیازمند migration و indexes است؛ در برنامه توسعه سایت با PR جدا انجام شود.
