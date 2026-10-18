package ir.saadegi.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One screen, one job: future VPN connect. Clearly labeled UI-only preview.
 * No invented ping, available node, consumed traffic, or connection status.
 */
@Composable
fun ConnectHome() {
    var selectedCountry by rememberSaveable { mutableStateOf("هوشمند") }
    var countrySheetVisible by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val countries = listOf("هوشمند", "آلمان", "هلند", "ترکیه")

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("اتصال، به سادگی", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)
            Text(
                "یک لمس تا دنیای بدون محدودیت",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
            )
        }
        ConnectionHero()
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .clickable { countrySheetVisible = true },
            color = colors.surface,
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 17.dp, vertical = 17.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(44.dp).background(colors.secondaryContainer, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SaadegiIcon(AppIcon.Globe, colors.onSecondaryContainer)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("موقعیت اتصال", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("انتخاب موقعیت دلخواه", fontSize = 11.sp, color = colors.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(selectedCountry, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
                    Text("پیش‌نمایش", fontSize = 10.sp, color = colors.onSurfaceVariant)
                }
                SaadegiIcon(AppIcon.Arrow, colors.onSurfaceVariant, size = 17.dp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            MiniInfo(
                Modifier.weight(1f),
                AppIcon.Shield,
                "اشتراک شما",
                "پس از ورود",
            )
            MiniInfo(
                Modifier.weight(1f),
                AppIcon.Clock,
                "وضعیت سرویس",
                "در انتظار راه‌اندازی",
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SaadegiIcon(AppIcon.Shield, colors.onSurfaceVariant, size=16.dp)
            Text(
                "این نسخه فقط نمای ظاهری است؛ اتصال واقعی هنوز فعال نیست.",
                fontSize = 11.sp,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    if (countrySheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { countrySheetVisible = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Text("انتخاب موقعیت", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(
                    "موقعیت‌ها در این نسخه نمایشی‌اند و هنوز به سرورهای واقعی متصل نیستند.",
                    color = colors.onSurfaceVariant, fontSize=12.sp,
                )
                countries.forEachIndexed { index, country ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedCountry = country; countrySheetVisible = false }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SaadegiIcon(if(index == 0) AppIcon.Sparkles else AppIcon.Globe,
                            if(selectedCountry == country) colors.primary else colors.onSurfaceVariant)
                        Spacer(Modifier.width(13.dp))
                        Text(country, Modifier.weight(1f), fontSize=14.sp, fontWeight = FontWeight.Medium)
                        if(selectedCountry == country) SaadegiIcon(AppIcon.Check, colors.primary)
                    }
                    if(index != countries.lastIndex) HorizontalDivider(color=colors.outlineVariant.copy(alpha=.55f))
                }
            }
        }
    }
}

@Composable
private fun ConnectionHero() {
    val corner = RoundedCornerShape(30.dp)
    Box(
        modifier = Modifier.fillMaxWidth().clip(corner).background(
            Brush.linearGradient(
                listOf(Color(0xFF282726), Color(0xFF3A302B), Color(0xFF5A3B31)),
            )
        ),
    ) {
        Box(
            modifier = Modifier.align(Alignment.TopStart).size(185.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0x40F0A074), Color.Transparent),
                    )
                ),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 17.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                Text(
                    "تحریم‌گذر سادگی",
                    modifier=Modifier.weight(1f),
                    color=Color(0xFFEDE7DE),
                    fontSize=13.sp,
                    fontWeight=FontWeight.Medium,
                )
                Row(
                    modifier=Modifier.background(Color(0x26FFFFFF),RoundedCornerShape(50.dp))
                        .padding(horizontal=10.dp, vertical=6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).background(Color(0xFFF4BF91),CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("پیش‌نمایش",fontSize=10.sp,color=Color(0xFFF4E3D6))
                }
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier=Modifier.size(124.dp)
                    .border(1.dp,Color(0x3DFFEDE1),CircleShape)
                    .padding(9.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFFF2D8C5),Color(0xFFE9B69A))),CircleShape),
                contentAlignment=Alignment.Center,
            ) {
                Box(
                    modifier=Modifier.size(82.dp)
                        .background(Color(0xFFF7ECE5),CircleShape),
                    contentAlignment=Alignment.Center,
                ) {
                    SaadegiIcon(AppIcon.Power, BrandColors.Terracotta, size=37.dp)
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                "اتصال به‌زودی فعال می‌شود",
                color=Color.White, fontSize=19.sp, fontWeight=FontWeight.Bold,
            )
            Text(
                "موتور VPN در حال پیاده‌سازی و آزمون است",
                color=Color(0xFFE2CEC1), fontSize=11.sp, textAlign=TextAlign.Center,
            )
            Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable
private fun MiniInfo(
    modifier: Modifier, icon: AppIcon, heading: String, value: String,
) {
    val c = MaterialTheme.colorScheme
    Surface(modifier=modifier, shape=RoundedCornerShape(20.dp), color=c.surface, shadowElevation=1.dp) {
        Column(Modifier.padding(horizontal=15.dp, vertical=13.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            SaadegiIcon(icon,c.primary,size=19.dp)
            Text(heading, color=c.onSurfaceVariant, fontSize=11.sp)
            Text(value, color=c.onSurface, fontWeight=FontWeight.SemiBold, fontSize=12.sp,maxLines=1)
        }
    }
}
