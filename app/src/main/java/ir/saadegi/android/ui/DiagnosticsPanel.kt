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
import ir.saadegi.android.diagnostics.ProbeMode
import ir.saadegi.android.diagnostics.QualityResult
import ir.saadegi.android.diagnostics.QualityScoring
import ir.saadegi.android.diagnostics.XrayNativeDiagnostics
import ir.saadegi.android.subscription.SubscriptionNode
import ir.saadegi.android.subscription.SubscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** All displayed latency/speed results originate from a real Xray-proxied request. */
@Composable
fun DiagnosticsPanel(
    repository: SubscriptionRepository,
    visibleNodes: List<SubscriptionNode>,
    selectedNodeId: String?,
) {
    val colors = MaterialTheme.colorScheme
    val engine = remember(repository) { XrayNativeDiagnostics(repository) }
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var completed by remember { mutableIntStateOf(0) }
    var lastMode by remember { mutableStateOf<ProbeMode?>(null) }
    var speed by remember { mutableStateOf<XrayNativeDiagnostics.SpeedResult?>(null) }
    var showSpeedWarning by remember { mutableStateOf(false) }
    val measured = remember { mutableStateMapOf<String, QualityResult>() }
    val targetNodes = visibleNodes.take(XrayNativeDiagnostics.MAX_NODES_PER_RUN)

    fun runLatency(mode: ProbeMode) {
        if (running || targetNodes.isEmpty()) return
        running = true
        error = null
        completed = 0
        lastMode = mode
        measured.clear()
        scope.launch {
            try {
                engine.measure(targetNodes.map { it.id }, mode) { result ->
                    withContext(Dispatchers.Main) {
                        measured[result.profileId] = result
                        completed++
                    }
                }
            } catch (_: Exception) {
                error = "آزمون از طریق موتور Xray انجام نشد. دسترسی اینترنت و سازگاری کانفیگ‌ها را بررسی کنید."
            } finally { running = false }
        }
    }

    fun runSpeed() {
        val node = selectedNodeId ?: return
        if (running) return
        running = true
        error = null
        speed = null
        scope.launch {
            try {
                speed = engine.testFiveMiB(node)
            } catch (_: Exception) {
                error = "دانلود از مسیر کانفیگ انتخاب‌شده ناموفق بود. سرور یا مسیر فایل آزمون را بررسی کنید."
            } finally { running = false }
        }
    }

    Surface(shape = RoundedCornerShape(22.dp), color = colors.surface) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("سنجش واقعی کیفیت اتصال", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "تأخیر از درخواست واقعی HTTP داخل Xray به دست می‌آید، نه پینگ مستقیم گوشی.",
                color = colors.onSurfaceVariant, fontSize = 11.sp, lineHeight = 17.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), enabled = !running && targetNodes.isNotEmpty(),
                    onClick = { runLatency(ProbeMode.ONE_STEP) }) {
                    Text("یک‌مرحله‌ای", fontSize = 12.sp, maxLines = 1)
                }
                OutlinedButton(modifier = Modifier.weight(1f), enabled = !running && targetNodes.isNotEmpty(),
                    onClick = { runLatency(ProbeMode.THREE_STEP) }) {
                    Text("سه‌مرحله‌ای", fontSize = 12.sp, maxLines = 1)
                }
            }
            Text(
                "سه‌مرحله‌ای: سه درخواست برای بررسی موفقیت، میانه تأخیر و نوسان؛ برای یافتن سرور پایدارتر مناسب‌تر است.",
                color = colors.onSurfaceVariant, fontSize = 11.sp, lineHeight = 17.sp,
            )
            if (visibleNodes.size > targetNodes.size) Text(
                "برای مدیریت زمان و مصرف، هر بار نخستین ۲۰ سرور فیلترشده سنجیده می‌شوند.",
                color = colors.onSurfaceVariant, fontSize = 11.sp,
            )
            if (running) Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("در حال آزمون… " + completed + " از " + targetNodes.size, fontSize = 12.sp)
            }
            if (lastMode != null && measured.isNotEmpty()) {
                Text("نتایج واقعی (" + (if (lastMode == ProbeMode.ONE_STEP) "یک" else "سه") + " مرحله)",
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                QualityScoring.rank(measured.values.toList()).forEachIndexed { index, result ->
                    val node = visibleNodes.firstOrNull { it.id == result.profileId }
                    Column(Modifier.fillMaxWidth().background(colors.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text((index + 1).toString() + ". " + (node?.title ?: "سرور"),
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        val description = if (result.medianLatencyMs == null) "اتصال ناموفق" else
                            result.medianLatencyMs.toString() + " ms · موفق " +
                                result.successes + "/" + result.attempts +
                                (result.jitterMs?.let { " · نوسان " + it + "ms" } ?: "") +
                                (result.stabilityScore?.let { " · امتیاز " + it } ?: "")
                        Text(description, color = if (result.successes == 0) colors.error else colors.onSurfaceVariant,
                            fontSize = 11.sp)
                    }
                }
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !running && selectedNodeId != null,
                onClick = { showSpeedWarning = true },
            ) { Text("تست سرعت دانلود ۵ مگابایت", fontSize = 13.sp) }
            Text(
                if (selectedNodeId == null) "برای تست سرعت ابتدا یکی از سرورها را انتخاب کنید."
                else "تست فقط از مسیر سرور انتخاب‌شده انجام می‌شود و از سهمیه اینترنت و اشتراک مصرف می‌کند.",
                fontSize = 11.sp, color = colors.onSurfaceVariant,
            )
            speed?.let {
                Text(
                    "دانلود ۵ MiB · " + String.format(Locale.US, "%.2f", it.mbps) + " Mbps · " +
                        String.format(Locale.US, "%.1f", it.seconds) + " ثانیه",
                    color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            error?.let { Text(it, color = colors.error, fontSize = 12.sp) }
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
