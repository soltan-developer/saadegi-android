package ir.saadegi.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PreviewSection(
    val title: String,
    val description: String,
    val icon: AppIcon,
    val entries: List<Pair<String,String>>,
)

val previewSections = listOf(
    PreviewSection("مجله سادگی","اخبار، آموزش و فرصت‌های جدید",AppIcon.News,
        listOf("اطلاعیه‌های رسمی" to "اخبار و تغییرات سرویس‌ها",
            "آموزش‌های کاربردی" to "راهنما و تجربه‌های حرفه‌ای",
            "آفرها و کمپین‌ها" to "پیشنهادهای ویژه و نظرسنجی")),
    PreviewSection("هوش مصنوعی","محصولات و ابزارهای مناسب شما",AppIcon.Sparkles,
        listOf("انتخاب ابزار" to "مقایسه خدمات براساس نیاز",
            "مدیریت اشتراک" to "سفارش‌ها و اطلاعات تحویل",
            "آکادمی سادگی" to "آموزش قدم‌به‌قدم ابزارها")),
    PreviewSection("پشتیبانی","همراه شما در تمام مراحل",AppIcon.Chat,
        listOf("گفت‌وگوی تیکتی" to "متصل به پشتیبانی سایت",
            "پیگیری درخواست" to "مشاهده وضعیت و پاسخ‌ها",
            "راهنمای استفاده" to "حل مشکلات پرتکرار")),
    PreviewSection("حساب من","تمام خدمات سادگی، یکجا",AppIcon.User,
        listOf("ورود با موبایل" to "همان حساب کاربری سایت",
            "اشتراک‌های من" to "مدیریت و تمدید سرویس",
            "سفارش‌ها و کیف پول" to "مشاهده خریدها و تراکنش‌ها")),
)

@Composable
fun FeaturePreview(section: PreviewSection) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier=Modifier.verticalScroll(rememberScrollState()).padding(horizontal=20.dp),
        verticalArrangement=Arrangement.spacedBy(15.dp),
    ) {
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Surface(
                shape=RoundedCornerShape(17.dp),color=c.secondaryContainer,
                modifier=Modifier.size(56.dp),
            ){
                androidx.compose.foundation.layout.Box(contentAlignment=Alignment.Center) {
                    SaadegiIcon(section.icon,c.onSecondaryContainer,size=27.dp)
                }
            }
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(section.title,fontSize=24.sp,fontWeight=FontWeight.Bold,color=c.onBackground)
                Text(section.description,fontSize=12.sp,color=c.onSurfaceVariant)
            }
        }
        Surface(shape=RoundedCornerShape(25.dp),color=c.surface) {
            Column(Modifier.padding(18.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("در دست طراحی و توسعه",fontSize=15.sp,fontWeight=FontWeight.Bold,color=c.primary)
                Text(
                    "این بخش پس از اتصال امن به سایت فعال می‌شود. محتوا و اطلاعات ساختگی نمایش نمی‌دهیم.",
                    fontSize=12.sp, color=c.onSurfaceVariant,
                )
            }
        }
        section.entries.forEachIndexed {index, entry ->
            Surface(shape=RoundedCornerShape(20.dp),color=c.surface,shadowElevation=1.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=16.dp),
                    verticalAlignment=Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(35.dp).background(c.surfaceVariant,RoundedCornerShape(12.dp)),
                        contentAlignment=Alignment.Center,
                    ) {
                        SaadegiIcon(AppIcon.Check,c.primary,size=17.dp)
                    }
                    Spacer(Modifier.size(13.dp))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        Text(entry.first,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                        Text(entry.second,fontSize=11.sp,color=c.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
