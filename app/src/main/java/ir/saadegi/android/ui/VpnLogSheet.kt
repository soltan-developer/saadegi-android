package ir.saadegi.android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.saadegi.android.vpn.VpnDiagnosticLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnLogSheet(onDismiss: () -> Unit) {
    val log by VpnDiagnosticLog.lines.collectAsState()
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("عیب‌یابی اتصال", fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(
                "مراحل اتصال بدون رمز، آدرس سرور یا لینک اشتراک ثبت می‌شوند. " +
                    "تا زمانی که گزارش را کپی نکنی چیزی ارسال نخواهد شد.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 370.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (log.isEmpty()) {
                    Text("هنوز رویدادی ثبت نشده است؛ یک‌بار اتصال آزمایشی را انجام بده.",
                        fontSize = 13.sp)
                } else {
                    log.reversed().forEach { line ->
                        Text(line, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = log.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("Saadegi VPN Diagnostics", VpnDiagnosticLog.export())
                        )
                    },
                ) { Text("کپی گزارش", fontSize = 12.sp) }
                TextButton(modifier = Modifier.weight(1f), onClick = onDismiss) {
                    Text("بستن")
                }
            }
        }
    }
}
