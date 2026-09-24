package fr.mickael.budget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Teal = Color(0xFF26A69A)
private val TealLight = Color(0xFF4DB6AC)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFFFFB74D),
    secondaryContainer = Color(0xFF1F3A38),
    onSecondaryContainer = Color(0xFFB2DFDB),
    background = Color(0xFF10181D),
    onBackground = Color(0xFFE3E8EB),
    surface = Color(0xFF10181D),
    onSurface = Color(0xFFE3E8EB),
    surfaceVariant = Color(0xFF26343D),
    onSurfaceVariant = Color(0xFFB0BEC5),
    surfaceContainer = Color(0xFF1A252C),
    surfaceContainerLow = Color(0xFF162026),
    surfaceContainerHigh = Color(0xFF1E2A33),
    surfaceContainerHighest = Color(0xFF26343D),
    outlineVariant = Color(0xFF34444E),
    error = Color(0xFFEF9A9A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF00796B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFFEF6C00),
    secondaryContainer = Color(0xFFD7ECE9),
    onSecondaryContainer = Color(0xFF00201C),
    surfaceVariant = Color(0xFFE0E6E8),
    onSurfaceVariant = Color(0xFF4A5A62),
    background = Color(0xFFF5F7F8),
    surface = Color(0xFFF5F7F8),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFECEFF1),
    surfaceContainerHighest = Color(0xFFE3E8EB),
)

/** Couleurs sémantiques pour les montants. */
object MoneyColors {
    val positive @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32)
    val warning @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFFFFB74D) else Color(0xFFE65100)
    val negative @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFFE57373) else Color(0xFFC62828)
}

private val AppTypography = Typography().let {
    it.copy(
        displaySmall = it.displaySmall.copy(fontWeight = FontWeight.Bold, fontSize = 38.sp),
        titleLarge = it.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = it.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun BudgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
