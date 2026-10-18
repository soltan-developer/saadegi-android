package ir.saadegi.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Internal, nonfunctional UI foundation. The VPN button deliberately stays disabled until
 * the native engine, entitlement and tunnel verification tests pass.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SaadegiApp() }
    }
}

private val Ivory = Color(0xFFFAF9F5)
private val Terracotta = Color(0xFFB84A26)
private val DeepInk = Color(0xFF1F1E1D)
private val DarkCanvas = Color(0xFF262624)
private val WarmLight = Color(0xFFE8A87C)

@Composable
private fun SaadegiApp() {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = WarmLight, onPrimary = DarkCanvas,
            background = DarkCanvas, onBackground = Color(0xFFEDEDE9),
            surface = Color(0xFF2D2D2A), onSurface = Color(0xFFEDEDE9),
            surfaceContainerLow = Color(0xFF32312E),
        )
    } else {
        lightColorScheme(
            primary = Terracotta, onPrimary = Color.White,
            background = Ivory, onBackground = DeepInk,
            surface = Color.White, onSurface = DeepInk,
            surfaceContainerLow = Color(0xFFF1EDE4),
        )
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        "اتصال" to "◉",
        "مجله" to "▤",
        "هوش مصنوعی" to "✦",
        "پشتیبانی" to "☏",
        "حساب من" to "●",
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = colors) {
            Scaffold(
                topBar = {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("سادگی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("نسخه آزمایشی", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                bottomBar = {
                    NavigationBar {
                        tabs.forEachIndexed { index, entry ->
                            NavigationBarItem(
                                selected = tab == index,
                                onClick = { tab = index },
                                icon = { Text(entry.second, fontSize = 20.sp) },
                                label = { Text(entry.first, maxLines = 1, fontSize = 10.sp) },
                                alwaysShowLabel = true,
                            )
                        }
                    }
                },
            ) { innerPadding ->
                Box(Modifier.fillMaxSize().padding(innerPadding)) {
                    when (tab) {
                        0 -> ConnectScreen()
                        1 -> SectionScreen("مجله سادگی", "اخبار، آموزش‌ها، آفرها و گفت‌وگوی کاربران", "این بخش در فاز اتصال به API سایت فعال می‌شود.")
                        2 -> SectionScreen("هوش مصنوعی", "راهنمای محصولات و مدیریت اکانت‌ها", "فروشگاه از داده‌های همان حساب سادگی استفاده خواهد کرد.")
                        3 -> SectionScreen("پشتیبانی", "تیکت‌های حساب شما", "این بخش پس از پیاده‌سازی ورود امن به تیکت‌های سایت متصل می‌شود.")
                        else -> SectionScreen("حساب من", "شماره موبایل، اشتراک‌ها، سفارش‌ها و کیف پول", "ورود OTP و نشست مستقل موبایل در فاز Mobile API اضافه می‌شود.")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectScreen() {
    var location by rememberSaveable { mutableStateOf("هوشمند") }
    val locations = listOf("هوشمند", "آلمان", "هلند", "ترکیه")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("تحریم‌گذر سادگی", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "اتصال آسان، انتخاب موقعیت و مدیریت اشتراک‌ها",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.size(136.dp),
                    shape = CircleShape,
                ) {
                    Text("⏻", fontSize = 58.sp)
                }
                Text("VPN هنوز آماده استفاده نیست", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "این نسخه فقط رابط اولیه است. تا قبل از آزمون موتور واقعی، اتصال فعال نمی‌شود.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)) {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text("موقعیت سرور", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("انتخاب نمونه برای پیش‌نمایش رابط؛ هنوز سرور زنده‌ای بررسی نشده است.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                locations.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { item ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = location == item,
                                onClick = { location = item },
                                label = { Text(item) },
                            )
                        }
                    }
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("اشتراک من", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                HorizontalDivider()
                Text("پس از ورود به حساب، حجم مصرفی، تاریخ انقضا و بسته‌های رزرو از سایت نمایش داده می‌شوند.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionScreen(title: String, subtitle: String, note: String) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("در دست توسعه", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(note, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
