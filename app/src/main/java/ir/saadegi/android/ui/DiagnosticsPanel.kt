package ir.saadegi.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.saadegi.android.diagnostics.ProbeEndpoint
import ir.saadegi.android.diagnostics.ProbeMode
import ir.saadegi.android.diagnostics.QualityResult
import ir.saadegi.android.diagnostics.QualityScoring
import ir.saadegi.android.diagnostics.SpeedTestPolicy
import android.os.SystemClock
import ir.saadegi.android.diagnostics.XrayNativeDiagnostics
import ir.saadegi.android.subscription.SubscriptionNode
import ir.saadegi.android.subscription.SubscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** All displayed latency/speed results originate from a real Xray-proxied request. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsPanel(
    repository: SubscriptionRepository,
    visibleNodes: List<SubscriptionNode>,
    selectedNodeId: String?,
    measurements: MutableMap<String, QualityResult>,
    vpnActive: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val engine = remember(repository) { XrayNativeDiagnostics(repository) }
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var runningJob by remember { mutableStateOf<Job?>(null) }
    var showModePicker by remember { mutableStateOf(false) }
    var endpoint by remember { mutableStateOf(ProbeEndpoint.GOOGLE) }
    var error by remember { mutableStateOf<String?>(null) }
    var completed by remember { mutableIntStateOf(0) }
    var lastMode by remember { mutableStateOf<ProbeMode?>(null) }
    var speed by remember { mutableStateOf<XrayNativeDiagnostics.SpeedResult?>(null) }
    var speedNodeId by remember { mutableStateOf<String?>(null) }
    var showSpeedWarning by remember { mutableStateOf(false) }
    val measured = measurements
    val measuredAt = remember { mutableStateMapOf<String, Long>() }
    val targetNodes = visibleNodes.take(XrayNativeDiagnostics.MAX_NODES_PER_RUN)

    fun runLatency(mode: ProbeMode) {
        if (running || vpnActive || targetNodes.isEmpty()) return
        running = true
        error = null
        completed = 0
        lastMode = mode
        measured.clear()
        measuredAt.clear()
        speed = null
        speedNodeId = null
        runningJob = scope.launch {
            try {
                engine.measure(targetNodes.map { it.id }, mode, endpoint) { result ->
                    withContext(Dispatchers.Main) {
                        measured[result.profileId] = result
                        measuredAt[result.profileId] = SystemClock.elapsedRealtime()
                        completed++
                    }
                }
            } catch (_: CancellationException) {
                error = "آزمون متوقف شد."
            } catch (_: Exception) {
                error = "آزمون از طریق موتور Xray انجام نشد. گزارش عیب‌یابی را بررسی کنید."
            } finally { running = false }
        }
    }

    fun runSpeed() {
        val node = selectedNodeId ?: return
        if (running || vpnActive || !SpeedTestPolicy.canTest(
                node, measured[node], measuredAt[node], SystemClock.elapsedRealtime()
            )) return
        running = true
        error = null
        speed = null
        speedNodeId = null
        runningJob = scope.launch {
            try {
                // A fresh real proxied HTTP check is mandatory before every
                // 5 MiB transfer; an earlier result can become stale on handoff.
                val check = engine.measure(listOf(node), ProbeMode.ONE_STEP, endpoint) { }
                    .firstOrNull()
                if (check == null || check.successes == 0) {
                    error = "ابتدا ارتباط واقعی سرور در شبکه فعلی باید موفق باشد."
                } else {
                    speed = engine.testFiveMiB(node)
                    speedNodeId = node
                }
            } catch (_: CancellationException) {
                error = "تست سرعت متوقف شد."
            } catch (_: Exception) {
                error = "دانلود از مسیر کانفیگ انتخاب‌شده ناموفق بود. گزارش عیب‌یابی را بررسی کنید."
            } finally { running = false }
        }
    }

    Surface(shape = RoundedCornerShape(22.dp), color = colors.surface) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("آزمایش کیفیت سرورها", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "زمان پاسخ HTTP از مسیر Xray؛ این عدد پینگ TCP نیست.",
                color = colors.onSurfaceVariant, fontSize = 11.sp, lineHeight = 17.sp,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !running && !vpnActive && targetNodes.isNotEmpty(),
                onClick = { showModePicker = true },
            ) { Text("شروع تست تأخیر  ▾", fontSize = 14.sp) }
            Text("مقصد: " + endpoint.label + " · زمان پاسخ شامل اتصال و TLS است",
                color = colors.onSurfaceVariant, fontSize = 11.sp)
            if (visibleNodes.size > targetNodes.size) Text(
                "برای مدیریت زمان و مصرف، هر بار نخستین ۲۰ سرور فیلترشده سنجیده می‌شوند.",
                color = colors.onSurfaceVariant, fontSize = 11.sp,
            )
            if (running) Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("در حال آزمون… " + completed + " از " + targetNodes.size,
                    modifier = Modifier.weight(1f), fontSize = 12.sp)
                TextButton(onClick = { runningJob?.cancel() }) { Text("توقف", fontSize = 12.sp) }
            }
            if (vpnActive) Text("برای آزمایش سایر کانفیگ‌ها ابتدا اتصال آزمایشی را قطع کنید.",
                color = colors.onSurfaceVariant, fontSize = 11.sp)
            if (lastMode != null && measured.isNotEmpty()) {
                val successful = measured.values.count { it.successes > 0 }
                Text("نتیجه: " + successful + " از " + measured.size + " سرور پاسخ‌گو · جزئیات روی کارت هر سرور",
                    fontSize = 12.sp, color = colors.onSurfaceVariant)
            }
            val testedSelected = SpeedTestPolicy.canTest(
                selectedNodeId, selectedNodeId?.let(measured::get),
                selectedNodeId?.let(measuredAt::get), SystemClock.elapsedRealtime()
            )
            if (testedSelected && selectedNodeId != null) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !running,
                    onClick = { showSpeedWarning = true },
                ) { Text("تست سرعت همین سرور · ۵ MiB", fontSize = 13.sp) }
                Text(
                    "فقط سرور انتخاب‌شده آزموده می‌شود. این دانلود از سهمیه اشتراک و اینترنت مصرف می‌کند.",
                    fontSize = 11.sp, color = colors.onSurfaceVariant,
                )
            } else {
                Text(
                    if (selectedNodeId == null)
                        "یکی از سرورها را انتخاب کنید؛ سپس تست تأخیر را اجرا کنید."
                    else
                        "برای نمایش تست سرعت، ابتدا تأخیر واقعی این سرور را با موفقیت اندازه بگیرید.",
                    fontSize = 11.sp, color = colors.onSurfaceVariant,
                )
            }
            if (speedNodeId == selectedNodeId) speed?.let {
                Text(
                    "دانلود کامل ۵ MiB · " +
                        String.format(Locale.US, "%.2f", it.mbps) + " Mbps  (" +
                        String.format(Locale.US, "%.2f", it.megabytesPerSecond) + " MB/s) · " +
                        String.format(Locale.US, "%.1f", it.seconds) + " ثانیه",
                    color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                )
                if (it.lowConfidence) Text(
                    "این انتقال خیلی کوتاه بود؛ نتیجه فقط تخمین سریع است، نه سرعت پایدار حداکثری.",
                    fontSize = 11.sp, color = colors.onSurfaceVariant,
                )
            }
            error?.let { Text(it, color = colors.error, fontSize = 12.sp) }
        }
    }

    if (showModePicker) ModalBottomSheet(
        onDismissRequest = { showModePicker = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("تنظیمات تست تأخیر", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("سرور مقصد را انتخاب کن. در هر دو برنامه از همین URL استفاده کن تا نتیجه قابل مقایسه باشد.",
                color = colors.onSurfaceVariant, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProbeEndpoint.entries.forEach { option ->
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { endpoint = option },
                        enabled = endpoint != option,
                    ) { Text(option.label, fontSize = 11.sp) }
                }
            }
            Text("در سه‌مرحله‌ای هر درخواست مستقل است. به علت راه‌اندازی مجدد هسته، زمان ممکن است بیشتر از حالت متصل باشد.",
                fontSize = 11.sp, color = colors.onSurfaceVariant)
            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                showModePicker = false
                runLatency(ProbeMode.ONE_STEP)
            }) { Text("سنجش سریع · یک‌مرحله‌ای") }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                showModePicker = false
                runLatency(ProbeMode.THREE_STEP)
            }) { Text("سنجش پایداری · سه‌مرحله‌ای") }
            Text("برای مقایسه منصفانه با v2rayNG هر دو اپ را روی یک اینترنت و URL یکسان بسنجید.",
                color = colors.onSurfaceVariant, fontSize = 11.sp)
        }
    }

    if (showSpeedWarning) AlertDialog(
        onDismissRequest = { showSpeedWarning = false },
        title = { Text("تست سرعت واقعی") },
        text = { Text("حدود ۵ مگابایت از طریق همین کانفیگ دانلود می‌شود و از حجم اشتراک شما کم خواهد شد. ادامه می‌دهید؟") },
        confirmButton = {
            TextButton(onClick = { showSpeedWarning = false; runSpeed() }) { Text("شروع تست") }
        },
        dismissButton = { TextButton(onClick = { showSpeedWarning = false }) { Text("انصراف") } },
    )
}
