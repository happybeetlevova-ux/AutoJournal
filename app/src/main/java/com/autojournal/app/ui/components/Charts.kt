package com.autojournal.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Столбчатая диаграмма (например, расходы по месяцам). */
@Composable
fun BarChart(values: List<Double>, labels: List<String>, color: Color, modifier: Modifier = Modifier) {
    val max = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val grid = MaterialTheme.colorScheme.outlineVariant
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val n = values.size
            if (n == 0) return@Canvas
            for (i in 0..3) {
                val y = size.height * i / 3f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            val slot = size.width / n
            val bw = slot * 0.62f
            values.forEachIndexed { i, v ->
                val h = (v / max).toFloat() * size.height
                if (h > 0f) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(i * slot + (slot - bw) / 2f, size.height - h),
                        size = Size(bw, h),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach {
                Text(
                    it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Линейный график (например, расход л/100 км по заправкам). */
@Composable
fun LineChart(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max()
        val range = if (max - min > 0) max - min else 1.0
        for (i in 0..3) {
            val y = size.height * i / 3f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        val step = size.width / (values.size - 1)
        val top = size.height * 0.1f
        val usable = size.height * 0.8f
        val pts = values.mapIndexed { i, v ->
            Offset(i * step, top + usable - ((v - min) / range).toFloat() * usable)
        }
        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (k in 1 until pts.size) lineTo(pts[k].x, pts[k].y)
        }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        pts.forEach { drawCircle(color, radius = 4.dp.toPx(), center = it) }
    }
}
