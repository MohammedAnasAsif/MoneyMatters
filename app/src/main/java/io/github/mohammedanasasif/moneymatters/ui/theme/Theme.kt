package io.github.mohammedanasasif.moneymatters.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Apple Stocks-like palette: true black, grey surfaces, green/red for direction. */
object AppColors {
    val Background = Color.Black
    val Surface = Color(0xFF1C1C1E)
    val SurfaceHigh = Color(0xFF2C2C2E)
    val SurfaceHighest = Color(0xFF3A3A3C)
    val Divider = Color(0xFF2C2C2E)
    val Muted = Color(0xFF8E8E93)
    val Up = Color(0xFF30D158)
    val Down = Color(0xFFFF453A)
    val Accent = Color(0xFF0A84FF)
}

@Composable
fun MoneyMattersTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            secondary = AppColors.Accent,
            secondaryContainer = AppColors.SurfaceHighest,
            onSecondaryContainer = Color.White,
            background = AppColors.Background,
            onBackground = Color.White,
            surface = AppColors.Surface,
            onSurface = Color.White,
            surfaceVariant = AppColors.SurfaceHigh,
            onSurfaceVariant = AppColors.Muted,
            surfaceContainerLowest = AppColors.Background,
            surfaceContainerLow = AppColors.Surface,
            surfaceContainer = AppColors.Surface,
            surfaceContainerHigh = AppColors.SurfaceHigh,
            surfaceContainerHighest = AppColors.SurfaceHighest,
            outline = AppColors.SurfaceHighest,
            outlineVariant = AppColors.Divider,
            error = AppColors.Down,
        ),
        content = content,
    )
}
