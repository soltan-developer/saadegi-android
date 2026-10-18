package ir.saadegi.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Scalable stroke icons, unlike font glyphs they never turn into missing-character boxes. */
enum class AppIcon { Power, Connect, News, Sparkles, Chat, User, Globe, Arrow, Shield, Check, Bell, Clock }

@Composable
fun SaadegiIcon(type: AppIcon, color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val factor = this.size.minDimension / 24f
        withTransform({ scale(factor, factor, pivot = Offset.Zero) }) {
            fun line(a: Float, b: Float, c: Float, d: Float) {
                drawLine(color, Offset(a, b), Offset(c, d), 1.8f, cap = StrokeCap.Round)
            }
            fun circle(x: Float, y: Float, radius: Float) =
                drawCircle(color, radius, Offset(x, y), style = Stroke(width = 1.8f))
            fun outline(points: List<Offset>, close: Boolean = false) {
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                    if (close) close()
                }
                drawPath(path, color, style = Stroke(1.8f, cap = StrokeCap.Round))
            }
            when(type) {
                AppIcon.Power -> {
                    drawArc(color,  -46f, 272f, false, Offset(4f, 4f), Size(16f, 16f), style = Stroke(2.1f, cap = StrokeCap.Round))
                    line(12f, 2.8f, 12f, 11.5f)
                }
                AppIcon.Connect -> {
                    circle(12f, 12f, 8.3f)
                    line(12f, 3.7f, 12f, 9f)
                    drawArc(color, 18f, 144f, false, Offset(7f, 7f), Size(10f, 10f), style = Stroke(1.8f))
                }
                AppIcon.News -> {
                    drawRoundRect(color, Offset(4.5f, 3f), Size(15f, 18f), CornerRadius(2f,2f), style=Stroke(1.8f))
                    line(8f, 8f, 16f, 8f);line(8f, 12f, 16f, 12f);line(8f, 16f, 13.5f, 16f)
                }
                AppIcon.Sparkles -> {
                    outline(listOf(Offset(12f, 2.5f),Offset(14.7f, 9.1f),Offset(21.5f,12f),Offset(14.7f,14.7f),Offset(12f,21.5f),Offset(9.3f,14.7f),Offset(2.5f,12f),Offset(9.3f,9.1f)),true)
                }
                AppIcon.Chat -> {
                    outline(listOf(Offset(5f, 5f),Offset(19f,5f),Offset(21f,8f),Offset(21f,16f),Offset(18f,19f),Offset(9f,19f),Offset(4f,22f),Offset(4f,18f),Offset(3f,16f),Offset(3f,8f)),true)
                    line(8f,11f,16f,11f);line(8f,14.5f,13f,14.5f)
                }
                AppIcon.User -> {
                    circle(12f, 8f, 3.5f)
                    drawArc(color, 205f, 130f, false, Offset(4f, 13f), Size(16f, 13f),style = Stroke(1.8f,cap=StrokeCap.Round))
                }
                AppIcon.Globe -> {
                    circle(12f,12f,9f)
                    drawOval(color, Offset(8f,3f),Size(8f,18f),style=Stroke(1.8f))
                    line(3f,12f,21f,12f)
                }
                AppIcon.Arrow -> {line(17f,12f,6f,12f);line(11f,7f,6f,12f);line(11f,17f,6f,12f)}
                AppIcon.Shield -> {
                    outline(listOf(Offset(12f,2f),Offset(20f,6f),Offset(19f,15f),Offset(12f,22f),Offset(5f,15f),Offset(4f,6f)),true)
                    line(9f,12f,11f,14f);line(11f,14f,15.5f,9.5f)
                }
                AppIcon.Check -> {line(5f,12f,10f,17f);line(10f,17f,19f,7f)}
                AppIcon.Clock -> {
                    circle(12f,12f,9f);line(12f,6.5f,12f,12f);line(12f,12f,15.5f,14f)
                }
                AppIcon.Bell -> {
                    outline(listOf(Offset(5f,17f),Offset(7f,14.5f),Offset(7f,9f),Offset(9f,5f),Offset(15f,5f),Offset(17f,9f),Offset(17f,14.5f),Offset(19f,17f)),false)
                    line(5f,17f,19f,17f);line(10.2f,20f,13.8f,20f)
                }
            }
        }
    }
}
