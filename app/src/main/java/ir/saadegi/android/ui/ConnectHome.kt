package ir.saadegi.android.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.saadegi.android.diagnostics.QualityResult
import ir.saadegi.android.vpn.SaadegiVpnService
import ir.saadegi.android.vpn.TunnelPhase
import ir.saadegi.android.vpn.VpnLiveStatus
import ir.saadegi.android.subscription.SubscriptionNode
import ir.saadegi.android.subscription.SubscriptionRepository
import ir.saadegi.android.subscription.SubscriptionSnapshot
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Subscription-only pilot: live imported node metadata, no fake countries or connected state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectHome() {
    val context = LocalContext.current
    val repo = remember(context) { SubscriptionRepository(context) }
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<SubscriptionSnapshot?>(null) }
    var busy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showImport by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showLocations by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
    var linkVisible by remember { mutableStateOf(false) }
    var location by rememberSaveable { mutableStateOf("همه سرورها") }
    var nodeId by rememberSaveable { mutableStateOf<String?>(null) }
    val nodeResults = remember { mutableStateMapOf<String, QualityResult>() }
    val palette = MaterialTheme.colorScheme
    val connection by VpnLiveStatus.state.collectAsState()
    val internalTrial = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    var showTrialConsent by remember { mutableStateOf(false) }
    val approval = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && nodeId != null) {
            val service = Intent(context, SaadegiVpnService::class.java).apply {
                action = SaadegiVpnService.ACTION_CONNECT
                putExtra(SaadegiVpnService.EXTRA_PROFILE_ID, nodeId)
            }
            context.startForegroundService(service)
        }
    }
    fun startConnection() {
        if (!internalTrial || nodeId == null) return
        val request = VpnService.prepare(context)
        if (request == null) {
            val service = Intent(context, SaadegiVpnService::class.java).apply {
                action = SaadegiVpnService.ACTION_CONNECT
                putExtra(SaadegiVpnService.EXTRA_PROFILE_ID, nodeId)
            }
            context.startForegroundService(service)
        } else approval.launch(request)
    }
    fun stopConnection() {
        val service = Intent(context, SaadegiVpnService::class.java).apply {
            action = SaadegiVpnService.ACTION_STOP
        }
        context.startService(service)
    }

    fun runAction(action: suspend () -> SubscriptionSnapshot?) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                snapshot = action()
                nodeResults.clear()
                val nodes = snapshot?.parsed?.nodes.orEmpty()
                if (location != "همه سرورها" && nodes.none { it.location == location }) location = "همه سرورها"
                if (nodes.none { it.id == nodeId }) nodeId = null
            } catch (ex: Exception) {
                error = when (ex.message) {
                    "INVALID_SUBSCRIPTION_URL" -> "فقط لینک معتبر HTTPS پذیرفته می‌شود."
                    "SUBSCRIPTION_FORMAT_UNSUPPORTED" -> "فرمت اشتراک قابل شناسایی نیست؛ فعلاً لینک‌های متنی و Base64 پشتیبانی می‌شوند."
                    "NO_VALID_NODES" -> "هیچ کانفیگ معتبری در اشتراک پیدا نشد."
                    "SUBSCRIPTION_TOO_LARGE", "TOO_MANY_NODES" -> "فایل اشتراک از حد مجاز بزرگ‌تر است."
                    "SUBSCRIPTION_HTTP_ERROR" -> "سرور اشتراک پاسخ موفق نداد. لینک را بررسی کنید."
                    else -> "دریافت یا ذخیره اشتراک ناموفق بود. اینترنت، لینک و گواهی SSL را بررسی کنید."
                }
            } finally { busy = false }
        }
    }
    LaunchedEffect(Unit) {
        try { snapshot = repo.load() }
        catch (_: Exception) { error = "خواندن اشتراک ذخیره‌شده ممکن نشد. لینک را دوباره وارد کنید." }
        finally { loading = false }
    }

    val nodes = snapshot?.parsed?.nodes.orEmpty()
    val locations = nodes.map { it.location }.distinct()
    val visible = nodes.filter { location == "همه سرورها" || it.location == location }
    val selected = nodes.firstOrNull { it.id == nodeId }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(top = 8.dp, bottom = 5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("اتصال، به سادگی", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("مدیریت اشتراک بدون نیاز به ورود به حساب", color = palette.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        item { SubscriptionHero(nodes.size, selected, connection.phase, connection.message,
            internalTrial, onStart = { showTrialConsent = true }, onStop = { stopConnection() }) }
        item {
            Surface(color = palette.surface, shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(43.dp).background(palette.secondaryContainer, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center) {
                            SaadegiIcon(AppIcon.Shield, palette.primary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("سابسکریپشن من", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(snapshot?.sourceHost ?: "لینک اشتراک خود را اضافه کنید",
                                fontSize = 11.sp, color = palette.onSurfaceVariant, maxLines = 1)
                        }
                        if (busy || loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                    if (snapshot == null) {
                        Button(
                            modifier = Modifier.fillMaxWidth(), enabled = !busy && !loading,
                            onClick = { link = ""; linkVisible = false; showImport = true },
                        ) { Text("افزودن لینک سابسکریپشن") }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(modifier = Modifier.weight(1f), enabled = !busy,
                                onClick = { runAction { repo.refresh() } }) { Text("به‌روزرسانی") }
                            OutlinedButton(modifier = Modifier.weight(1f), enabled = !busy,
                                onClick = { link = ""; linkVisible = false; showImport = true }) { Text("تغییر لینک") }
                        }
                        TextButton(onClick = { showDelete = true }, enabled = !busy) {
                            Text("حذف اشتراک از دستگاه", color = palette.error, fontSize = 11.sp)
                        }
                    }
                    error?.let { Text(it, color = palette.error, fontSize = 12.sp) }
                }
            }
        }
        if (snapshot != null) {
            item { SubscriptionUsageCard(snapshot!!) }
            item {
                Surface(
                    color = palette.surface, shape = RoundedCornerShape(19.dp),
                    modifier = Modifier.fillMaxWidth().clickable { showLocations = true },
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SaadegiIcon(AppIcon.Globe, palette.primary)
                        Column(Modifier.weight(1f)) {
                            Text("موقعیت کانفیگ‌ها", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("استخراج‌شده از نام اینباند", color = palette.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Text(location, fontSize = 12.sp, color = palette.primary)
                        SaadegiIcon(AppIcon.Arrow, palette.onSurfaceVariant, size = 16.dp)
                    }
                }
            }
            item {
                DiagnosticsPanel(repo, visible, nodeId, nodeResults,
                    vpnActive = connection.phase == TunnelPhase.CONNECTED ||
                        connection.phase == TunnelPhase.STARTING ||
                        connection.phase == TunnelPhase.VERIFYING)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("سرورهای دریافتی", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${visible.size} از ${nodes.size}", fontSize = 12.sp, color = palette.onSurfaceVariant)
                }
            }
            items(visible, key = { it.id }) { node ->
                ServerRow(node, nodeId == node.id, nodeResults[node.id]) { nodeId = node.id }
            }
            item {
                Text(
                    "این سرورها از اشتراک شما خوانده شده‌اند؛ هنوز امکان اتصال VPN فعال نیست. " +
                        "موقعیت صرفاً از نام کانفیگ استخراج می‌شود و موقعیت تأییدشده نیست.",
                    fontSize = 11.sp, lineHeight = 17.sp, color = palette.onSurfaceVariant,
                )
            }
        }
    }

    if (showTrialConsent) AlertDialog(
        onDismissRequest = { showTrialConsent = false },
        title = { Text("اتصال آزمایشی اندروید") },
        text = { Text("این نسخه داخلی هنوز روی دستگاه واقعی تأیید نشده است. ممکن است اینترنت موقتاً قطع شود. اگر بررسی ترافیک شکست بخورد، تونل بسته می‌شود. برای ادامه اجازه VPN اندروید لازم است.") },
        confirmButton = { TextButton(onClick = { showTrialConsent = false; startConnection() }) {
            Text("تلاش برای اتصال")
        } },
        dismissButton = { TextButton(onClick = { showTrialConsent = false }) { Text("انصراف") } },
    )

    if (showImport) AlertDialog(
        onDismissRequest = { showImport = false; link = "" },
        title = { Text("افزودن لینک اشتراک") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("لینک HTTPS را وارد کنید؛ آدرس و کانفیگ‌ها در حافظه امن دستگاه ذخیره می‌شوند.",
                    fontSize = 12.sp, color = palette.onSurfaceVariant)
                OutlinedTextField(
                    value = link, onValueChange = { link = it.take(2048) },
                    label = { Text("لینک سابسکریپشن") }, singleLine = true,
                    visualTransformation = if (linkVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { linkVisible = !linkVisible }) {
                    Text(if (linkVisible) "پنهان‌کردن" else "نمایش لینک")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = link.isNotBlank() && !busy, onClick = {
                val target = link.trim()
                link = ""
                showImport = false
                runAction { repo.importSubscription(target) }
            }) { Text("دریافت سرورها") }
        },
        dismissButton = { TextButton(onClick = { showImport = false; link = "" }) { Text("انصراف") } },
    )

    if (showDelete) AlertDialog(
        onDismissRequest = { showDelete = false },
        title = { Text("حذف اشتراک؟") },
        text = { Text("لینک و تمام کانفیگ‌های ذخیره‌شده از این دستگاه پاک می‌شوند.") },
        confirmButton = {
            TextButton(onClick = {
                showDelete = false
                runAction { repo.delete(); nodeId = null; location = "همه سرورها"; null }
            }) { Text("حذف", color = palette.error) }
        },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text("انصراف") } },
    )

    if (showLocations) ModalBottomSheet(
        onDismissRequest = { showLocations = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
            Text("فیلتر موقعیت", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("بر اساس نام کانفیگ‌های واقعی", fontSize = 12.sp, color = palette.onSurfaceVariant)
            (listOf("همه سرورها") + locations).forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { location = option; showLocations = false }
                    .padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(option, Modifier.weight(1f), fontSize = 14.sp)
                    if (option == location) SaadegiIcon(AppIcon.Check, palette.primary)
                }
                HorizontalDivider(color = palette.outlineVariant)
            }
            Spacer(Modifier.size(15.dp))
        }
    }
}

@Composable
private fun SubscriptionHero(
    count: Int,
    selected: SubscriptionNode?,
    phase: TunnelPhase,
    statusMessage: String?,
    internalTrial: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val active = phase == TunnelPhase.CONNECTED
    val working = phase == TunnelPhase.STARTING || phase == TunnelPhase.VERIFYING
    Box(Modifier.fillMaxWidth().background(
        Brush.linearGradient(listOf(Color(0xFF292624), Color(0xFF55392E))),
        RoundedCornerShape(28.dp),
    )) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("تحریم‌گذر سادگی", Modifier.weight(1f), fontSize = 13.sp, color = Color(0xFFF8EEE6))
                Text("نسخه آزمون", fontSize = 10.sp, color = Color(0xFFEDD1BD))
            }
            Box(Modifier.size(84.dp).background(Color(0xFFEED4C4), CircleShape),
                contentAlignment = Alignment.Center) {
                SaadegiIcon(AppIcon.Power, BrandColors.Terracotta, size = 39.dp)
            }
            Text(
                when {
                    phase == TunnelPhase.CONNECTED -> "اتصال تأیید شد"
                    phase == TunnelPhase.VERIFYING -> "در حال بررسی ترافیک"
                    phase == TunnelPhase.STARTING -> "در حال برقراری اتصال"
                    phase == TunnelPhase.ERROR -> "اتصال برقرار نشد"
                    selected == null && count > 0 -> "یکی از سرورها را انتخاب کنید"
                    selected == null -> "یک اشتراک اضافه کنید"
                    else -> selected.title
                },
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                textAlign = TextAlign.Center,
            )
            Text(
                statusMessage ?: if (selected == null) "وضعیت اتصال از موتور واقعی دریافت می‌شود"
                    else selected.protocol + " · " + selected.location,
                color = Color(0xFFE8D2C4), fontSize = 11.sp, textAlign = TextAlign.Center,
                maxLines = 3,
            )
            if (internalTrial) {
                if (active || working) {
                    OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                        Text("قطع اتصال", fontSize = 13.sp)
                    }
                } else {
                    Button(onClick = onStart, enabled = selected != null,
                        modifier = Modifier.fillMaxWidth()) {
                        Text("اتصال آزمایشی", fontSize = 13.sp)
                    }
                }
            } else {
                Text("اتصال تا تکمیل آزمون دستگاه غیرفعال است",
                    color = Color(0xFFFFC6A6), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SubscriptionUsageCard(snapshot: SubscriptionSnapshot) {
    val palette = MaterialTheme.colorScheme
    val usage = snapshot.parsed.usage
    Surface(color = palette.surface, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("اطلاعات اشتراک", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "آخرین به‌روزرسانی: " + DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT, Locale("fa")
                ).format(Date(snapshot.parsed.updatedAtMillis)),
                fontSize = 11.sp, color = palette.onSurfaceVariant,
            )
            if (usage == null) {
                Text("سرور اطلاعات مصرف و انقضا ارائه نکرده است.",
                    color = palette.onSurfaceVariant, fontSize = 12.sp)
            } else {
                Text(
                    "مصرف: ${String.format(Locale.US, "%.2f", usage.usedBytes / 1073741824.0)} از " +
                        "${String.format(Locale.US, "%.2f", usage.totalBytes / 1073741824.0)} گیگابایت",
                    fontSize = 12.sp, fontWeight = FontWeight.Medium,
                )
                Text("انقضا: " + (usage.expiresAtSeconds?.let {
                    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("fa")).format(Date(it * 1000))
                } ?: "اعلام نشده"), color = palette.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ServerRow(node: SubscriptionNode, selected: Boolean, result: QualityResult?, onSelect: () -> Unit) {
    val palette = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        color = if (selected) palette.secondaryContainer else palette.surface,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(Modifier.size(38.dp).background(palette.surfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {
                SaadegiIcon(if (selected) AppIcon.Check else AppIcon.Globe, palette.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(node.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2)
                Text("${node.protocol} · ${node.transport} · ${node.location}",
                    color = palette.onSurfaceVariant, fontSize = 11.sp)
                result?.let {
                    Text(
                        if (it.medianLatencyMs == null) "بدون پاسخ · " + it.successes + "/" + it.attempts
                        else "HTTP " + it.medianLatencyMs + " ms · موفق " + it.successes +
                            "/" + it.attempts +
                            (it.jitterMs?.let { jitter -> " · نوسان " + jitter + " ms" } ?: ""),
                        fontSize = 11.sp,
                        color = if (it.successes == 0) palette.error else palette.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (!node.compatibleWithCurrentEngine) {
                Text("موتور دیگر", color = palette.error, fontSize = 10.sp)
            } else if (selected) {
                Text("انتخاب‌شده", color = palette.primary, fontSize = 10.sp)
            }
        }
    }
}
