package ir.saadegi.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.saadegi.android.ui.AppIcon
import ir.saadegi.android.ui.ConnectHome
import ir.saadegi.android.ui.FeaturePreview
import ir.saadegi.android.ui.SaadegiIcon
import ir.saadegi.android.ui.SaadegiTheme
import ir.saadegi.android.ui.previewSections

/**
 * UI preview. This build deliberately cannot establish a VPN connection.
 * Connect remains unavailable until native TUN and Xray traffic tests pass.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        setContent { SaadegiApp() }
    }
}

private data class Destination(val label: String, val icon: AppIcon)
private val destinations = listOf(
    Destination("اتصال", AppIcon.Connect),
    Destination("مجله", AppIcon.News),
    Destination("هوش مصنوعی", AppIcon.Sparkles),
    Destination("پشتیبانی", AppIcon.Chat),
    Destination("حساب", AppIcon.User),
)

@Composable
private fun SaadegiApp() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        SaadegiTheme {
            val c = MaterialTheme.colorScheme
            Scaffold(
                containerColor = c.background,
                contentWindowInsets = WindowInsets(0,0,0,0),
                topBar = { BrandedHeader() },
                bottomBar = {
                    NavigationBar(
                        containerColor = c.surface,
                        contentColor = c.onSurface,
                        tonalElevation = 0.dp,
                    ) {
                        destinations.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = selected == index,
                                onClick = { selected = index },
                                icon = {
                                    SaadegiIcon(
                                        item.icon,
                                        if (selected == index) c.primary else c.onSurfaceVariant,
                                        size = 21.dp,
                                    )
                                },
                                label = {
                                    Text(
                                        item.label,
                                        fontSize = 10.sp,
                                        fontWeight = if(selected == index) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                    )
                                },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = c.primary,
                                    selectedTextColor = c.primary,
                                    indicatorColor = c.secondaryContainer,
                                    unselectedIconColor = c.onSurfaceVariant,
                                    unselectedTextColor = c.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                },
            ) { insets ->
                Box(
                    modifier=Modifier.fillMaxSize().padding(insets),
                ) {
                    if(selected==0) ConnectHome()
                    else FeaturePreview(previewSections[selected-1])
                }
            }
        }
    }
}

@Composable
private fun BrandedHeader() {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(c.background)
            .statusBarsPadding()
            .padding(horizontal=20.dp, vertical=13.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier=Modifier.size(41.dp),
            shape=RoundedCornerShape(14.dp),
            color=c.primary,
        ) {
            Box(contentAlignment=Alignment.Center) {
                Text("س",color=c.onPrimary,fontWeight=FontWeight.Bold,fontSize=25.sp)
            }
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(1.dp)) {
            Text("سادگی",fontSize=21.sp,fontWeight=FontWeight.Bold,color=c.onBackground)
            Text("خدمات دیجیتال هوشمند",fontSize=10.sp,color=c.onSurfaceVariant)
        }
        Surface(shape=RoundedCornerShape(50.dp),color=c.surfaceVariant) {
            Text(
                "نسخه آزمایشی",
                modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp),
                color=c.onSurfaceVariant,
                fontWeight=FontWeight.Medium,
                fontSize=10.sp,
            )
        }
    }
}
