package com.lavallette.tides.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.lavallette.tides.data.model.TideSample
import com.lavallette.tides.ui.theme.Foam
import com.lavallette.tides.ui.theme.OceanBright
import com.lavallette.tides.ui.theme.Sand
import com.lavallette.tides.ui.theme.SeaGlass
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

@Composable
fun TideChart(
    samples: List<TideSample>,
    nowMillis: Long = System.currentTimeMillis(),
    modifier: Modifier = Modifier
) {
    if (samples.size < 2) return

    val zone = ZoneId.of("America/New_York")
    val hourFmt = DateTimeFormatter.ofPattern("ha").withZone(zone)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val leftPad = 8.dp.toPx()
        val rightPad = size.width - 8.dp.toPx()
        val topPad = 16.dp.toPx()
        val bottomPad = size.height - 28.dp.toPx()
        val chartWidth = rightPad - leftPad
        val chartHeight = bottomPad - topPad

        val minH = samples.minOf { it.heightFt } - 0.3
        val maxH = samples.maxOf { it.heightFt } + 0.3
        val range = max(maxH - minH, 0.5)
        val t0 = samples.first().timeMillis.toDouble()
        val t1 = samples.last().timeMillis.toDouble()
        val tSpan = max(t1 - t0, 1.0)

        fun xFor(t: Long): Float =
            leftPad + (((t - t0) / tSpan) * chartWidth).toFloat()

        fun yFor(h: Double): Float =
            (bottomPad - (((h - minH) / range) * chartHeight)).toFloat()

        for (i in 0..3) {
            val y = topPad + chartHeight * i / 3f
            drawLine(
                color = Foam.copy(alpha = 0.15f),
                start = Offset(leftPad, y),
                end = Offset(rightPad, y),
                strokeWidth = 1f
            )
        }

        val linePath = Path()
        val fillPath = Path()
        samples.forEachIndexed { index, sample ->
            val x = xFor(sample.timeMillis)
            val y = yFor(sample.heightFt)
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, bottomPad)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(xFor(samples.last().timeMillis), bottomPad)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(OceanBright.copy(alpha = 0.55f), Color.Transparent),
                startY = topPad,
                endY = bottomPad
            )
        )
        drawPath(
            path = linePath,
            color = SeaGlass,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        val clampedNow = min(max(nowMillis, samples.first().timeMillis), samples.last().timeMillis)
        val nowX = xFor(clampedNow)
        val before = samples.lastOrNull { it.timeMillis <= nowMillis } ?: samples.first()
        val after = samples.firstOrNull { it.timeMillis >= nowMillis } ?: samples.last()
        val nowHeight = if (before.timeMillis == after.timeMillis) {
            before.heightFt
        } else {
            val r = (nowMillis - before.timeMillis).toDouble() /
                (after.timeMillis - before.timeMillis).toDouble()
            before.heightFt + (after.heightFt - before.heightFt) * r
        }

        drawLine(
            color = Sand,
            start = Offset(nowX, topPad),
            end = Offset(nowX, bottomPad),
            strokeWidth = 2.dp.toPx()
        )
        drawCircle(
            color = Sand,
            radius = 6.dp.toPx(),
            center = Offset(nowX, yFor(nowHeight))
        )

        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(200, 169, 199, 214)
            textSize = 28f
            isAntiAlias = true
        }
        val labelEvery = max(1, samples.size / 5)
        samples.forEachIndexed { index, sample ->
            if (index % labelEvery == 0 || index == samples.lastIndex) {
                val label = hourFmt.format(Instant.ofEpochMilli(sample.timeMillis))
                    .lowercase()
                    .replace("am", "a")
                    .replace("pm", "p")
                val x = xFor(sample.timeMillis)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x - 18f,
                    size.height - 6f,
                    labelPaint
                )
            }
        }
    }
}
