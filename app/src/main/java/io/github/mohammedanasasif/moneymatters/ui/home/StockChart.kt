package io.github.mohammedanasasif.moneymatters.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.mohammedanasasif.moneymatters.ui.theme.AppColors

/**
 * Line chart with a gradient fill and finger scrubbing, modelled on Apple Stocks.
 * The dotted line is the previous period's total, like the "previous close" line in Stocks:
 * once the line crosses it, you have spent more than last time.
 */
@Composable
fun StockChart(
    points: List<ChartPoint>,
    start: Long,
    end: Long,
    baseline: Long?,
    lineColor: Color,
    onScrub: (ChartPoint?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnScrub by rememberUpdatedState(onScrub)
    var scrubX by remember { mutableStateOf<Float?>(null) }
    val span = (end - start).coerceAtLeast(1L)
    val maxValue = maxOf(points.maxOfOrNull { it.value } ?: 0L, baseline ?: 0L).coerceAtLeast(1L)

    fun valueAt(time: Long): Long = points.lastOrNull { it.time <= time }?.value ?: 0L

    Canvas(
        modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(points, start, end) {
                fun report(x: Float) {
                    val clamped = x.coerceIn(0f, size.width.toFloat())
                    scrubX = clamped
                    val time = start + (clamped / size.width * span).toLong()
                    currentOnScrub(ChartPoint(time, valueAt(time)))
                }
                detectHorizontalDragGestures(
                    onDragStart = { report(it.x) },
                    onDragEnd = { scrubX = null; currentOnScrub(null) },
                    onDragCancel = { scrubX = null; currentOnScrub(null) },
                ) { change, _ ->
                    change.consume()
                    report(change.position.x)
                }
            },
    ) {
        val top = 16.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        fun x(time: Long) = (time - start).toFloat() / span * size.width
        fun y(value: Long) = bottom - value.toFloat() / maxValue * (bottom - top)

        if (baseline != null && baseline > 0) {
            val by = y(baseline)
            drawLine(
                color = AppColors.Muted.copy(alpha = 0.6f),
                start = Offset(0f, by),
                end = Offset(size.width, by),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx())),
            )
        }

        if (points.size >= 2) {
            val line = Path()
            val fill = Path()
            points.forEachIndexed { i, p ->
                if (i == 0) {
                    line.moveTo(x(p.time), y(p.value))
                    fill.moveTo(x(p.time), bottom)
                    fill.lineTo(x(p.time), y(p.value))
                } else {
                    line.lineTo(x(p.time), y(p.value))
                    fill.lineTo(x(p.time), y(p.value))
                }
            }
            fill.lineTo(x(points.last().time), bottom)
            fill.close()

            drawPath(
                fill,
                Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.30f), Color.Transparent), startY = top, endY = bottom),
            )
            drawPath(line, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        scrubX?.let { sx ->
            drawLine(Color.White.copy(alpha = 0.5f), Offset(sx, top), Offset(sx, bottom), strokeWidth = 1.dp.toPx())
            val time = start + (sx / size.width * span).toLong()
            val center = Offset(sx, y(valueAt(time)))
            drawCircle(lineColor, radius = 6.dp.toPx(), center = center)
            drawCircle(Color.Black, radius = 2.5.dp.toPx(), center = center)
        }
    }
}
