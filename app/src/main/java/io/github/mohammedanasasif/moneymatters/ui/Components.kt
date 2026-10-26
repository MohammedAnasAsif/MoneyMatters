package io.github.mohammedanasasif.moneymatters.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mohammedanasasif.moneymatters.ui.theme.AppColors

/** The coloured price pill from Stocks, used for amounts. */
@Composable
fun AmountPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

/** Watchlist sparkline: line, fading fill underneath, dashed line at last period's level. */
@Composable
fun Sparkline(values: List<Long>, baseline: Long?, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val max = maxOf(values.max(), baseline ?: 0L).coerceAtLeast(1L).toFloat()
        val top = 2.dp.toPx()
        val bottom = size.height
        fun y(v: Long) = bottom - v / max * (bottom - top)
        fun x(i: Int) = size.width * i / (values.size - 1)

        if (baseline != null && baseline > 0) {
            drawLine(
                color = AppColors.Muted.copy(alpha = 0.7f),
                start = Offset(0f, y(baseline)),
                end = Offset(size.width, y(baseline)),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx())),
            )
        }

        val line = Path()
        val fill = Path()
        values.forEachIndexed { i, v ->
            if (i == 0) {
                line.moveTo(x(i), y(v))
                fill.moveTo(x(i), bottom)
                fill.lineTo(x(i), y(v))
            } else {
                line.lineTo(x(i), y(v))
                fill.lineTo(x(i), y(v))
            }
        }
        fill.lineTo(x(values.size - 1), bottom)
        fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent), startY = top, endY = bottom))
        drawPath(line, color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Green when the change is good for you, red when it isn't, grey when nothing changed. */
fun changeColor(change: Long, upIsGood: Boolean): Color = when {
    change == 0L -> AppColors.SurfaceHighest
    (change > 0) == upIsGood -> AppColors.Up
    else -> AppColors.Down
}

/** Text-only segmented control, like the 1D / 1W / 1M row in Stocks. */
@Composable
fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    spread: Boolean = false,
) {
    Row(
        modifier,
        horizontalArrangement = if (spread) Arrangement.SpaceBetween else Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Text(
                label(option),
                color = if (isSelected) Color.White else AppColors.Muted,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) AppColors.SurfaceHigh else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
fun BackBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
