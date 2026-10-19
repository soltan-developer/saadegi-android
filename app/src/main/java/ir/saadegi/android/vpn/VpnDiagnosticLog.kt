package ir.saadegi.android.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Sanitized, bounded, in-memory events. No profile URIs, IPs, tokens or raw errors. */
object VpnDiagnosticLog {
    enum class Step(val label: String) {
        READY("آماده آزمایش"), REQUEST("درخواست اتصال"), CONSENT("درخواست مجوز VPN"),
        GRANTED("مجوز VPN داده شد"), DENIED("مجوز VPN رد شد"),
        SERVICE("سرویس اتصال آغاز شد"), INVALID_PROFILE("شناسه سرور نامعتبر"),
        FOREGROUND("اعلان سرویس فعال شد"), FOREGROUND_FAILED("سرویس پس‌زمینه مجاز نیست"),
        PROFILE_LOAD("خواندن کانفیگ رمزگذاری‌شده"), PROFILE_MISSING("کانفیگ یافت نشد"),
        CONVERT("تبدیل کانفیگ برای Xray"), CONVERT_FAILED("تبدیل کانفیگ ناموفق"),
        TUN_PERMISSION("بررسی مجوز TUN"), TUN_ESTABLISH("ساخت تونل اندروید"),
        TUN_FAILED("ساخت تونل ناموفق"), XRAY_START("راه‌اندازی هسته Xray"),
        XRAY_REJECT("هسته Xray راه‌اندازی نشد"),
        VERIFY_BEGIN("بررسی درخواست HTTPS از داخل VPN"),
        VERIFY_NETWORK_MISSING("شبکه VPN در اندروید پیدا نشد"),
        VERIFY_HTTP_FAILED("درخواست HTTPS از VPN پاسخ معتبر نداد"),
        VERIFIED("عبور ترافیک VPN تأیید شد"), STOP_REQUEST("درخواست قطع اتصال"),
        STOPPED("اتصال و تونل متوقف شدند"), REVOKED("اندروید مجوز VPN را لغو کرد"),
        CANCELLED("فرایند اتصال لغو شد"), ERROR_SECURITY("خطای مجوز/امنیت اندروید"),
        ERROR_DNS("خطای حل نشانی DNS"), ERROR_TLS("خطای TLS"),
        ERROR_TIMEOUT("پایان زمان انتظار شبکه"), ERROR_IO("خطای ورودی/خروجی شبکه"),
        ERROR_NATIVE("خطای هسته یا پیکربندی"), ERROR_OTHER("خطای ناشناخته اتصال")
    }

    private val lock = Any()
    private val ring = ArrayDeque<String>()
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines = _lines.asStateFlow()

    fun add(step: Step) = synchronized(lock) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        ring.addLast(stamp + " · " + step.name + " · " + step.label)
        while (ring.size > 80) ring.removeFirst()
        _lines.value = ring.toList()
    }

    fun classify(error: Throwable): Step = when (error) {
        is SecurityException -> Step.ERROR_SECURITY
        is java.net.UnknownHostException -> Step.ERROR_DNS
        is javax.net.ssl.SSLException -> Step.ERROR_TLS
        is java.net.SocketTimeoutException -> Step.ERROR_TIMEOUT
        is java.io.IOException -> Step.ERROR_IO
        is IllegalArgumentException, is IllegalStateException -> Step.ERROR_NATIVE
        else -> Step.ERROR_OTHER
    }

    fun export(): String = buildString {
        appendLine("Saadegi Android - VPN diagnostic report")
        appendLine("Android API: " + android.os.Build.VERSION.SDK_INT)
        appendLine("No credentials, hostname, IP, URL, secret or raw exception is included.")
        synchronized(lock) { ring.forEach { appendLine(it) } }
    }
}
