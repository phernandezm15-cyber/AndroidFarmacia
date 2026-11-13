package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class FarmaciaExtendedColors(
  val success: Color,
  val onSuccess: Color,
  val successContainer: Color,
  val warning: Color,
  val onWarning: Color,
  val warningContainer: Color,
  val chartIndigo: Color,
  val chartEmerald: Color,
  val chartAmber: Color,
  val chartCyan: Color,
  val chartViolet: Color,
  val chartSlate: Color,
)

val LocalFarmaciaColors = staticCompositionLocalOf {
  FarmaciaExtendedColors(
    success = EmeraldSuccess,
    onSuccess = Color.White,
    successContainer = EmeraldSuccessContainer,
    warning = AmberWarning,
    onWarning = Color(0xFF451A03),
    warningContainer = AmberWarningContainer,
    chartIndigo = ChartIndigo,
    chartEmerald = ChartEmerald,
    chartAmber = ChartAmber,
    chartCyan = ChartCyan,
    chartViolet = ChartViolet,
    chartSlate = ChartSlate,
  )
}

private val LightColorScheme = lightColorScheme(
  primary = IndigoPrimary,
  onPrimary = Color.White,
  primaryContainer = IndigoLightContainer,
  onPrimaryContainer = IndigoPrimary,
  secondary = AccentTeal,
  onSecondary = Color.White,
  secondaryContainer = AccentTealLight,
  onSecondaryContainer = AccentTealDark,
  tertiary = AccentBlue,
  onTertiary = Color.White,
  background = LightBackground,
  onBackground = LightOnSurface,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightOnSurfaceVariant,
  surfaceContainerLow = Color(0xFFF8FAFC),
  surfaceContainer = Color(0xFFF1F5F9),
  surfaceContainerHigh = Color(0xFFE2E8F0),
  outline = LightOutline,
  outlineVariant = Color(0xFFE2E8F0),
  error = RedDestructive,
  onError = Color.White,
  errorContainer = RedDestructiveContainer,
  onErrorContainer = Color(0xFF991B1B),
)

private val DarkColorScheme = darkColorScheme(
  primary = IndigoDarkPrimary,
  onPrimary = Color(0xFF0A1942),
  primaryContainer = IndigoDarkContainer,
  onPrimaryContainer = Color(0xFFDBEAFE),
  secondary = Color(0xFF2DD4BF),
  onSecondary = Color(0xFF042F2E),
  secondaryContainer = AccentTealDark,
  onSecondaryContainer = Color(0xFFCCFBF1),
  tertiary = AccentBlue,
  onTertiary = Color.White,
  background = DarkBackground,
  onBackground = DarkOnSurface,
  surface = DarkSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceVariant,
  surfaceContainerLow = Color(0xFF0F172A),
  surfaceContainer = Color(0xFF1E293B),
  surfaceContainerHigh = Color(0xFF273449),
  outline = DarkOutline,
  outlineVariant = Color(0xFF1E293B),
  error = Color(0xFFF87171),
  onError = Color(0xFF450A0A),
  errorContainer = RedDestructiveDarkContainer,
  onErrorContainer = Color(0xFFFEE2E2),
)

val FarmaciaShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun FarmaciaTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val extendedColors = if (darkTheme) {
    FarmaciaExtendedColors(
      success = Color(0xFF34D399),
      onSuccess = Color(0xFF022C22),
      successContainer = EmeraldSuccessDarkContainer,
      warning = Color(0xFFFBBF24),
      onWarning = Color(0xFF451A03),
      warningContainer = AmberWarningDarkContainer,
      chartIndigo = Color(0xFF60A5FA),
      chartEmerald = Color(0xFF34D399),
      chartAmber = Color(0xFFFBBF24),
      chartCyan = Color(0xFF22D3EE),
      chartViolet = Color(0xFFA78BFA),
      chartSlate = Color(0xFF94A3B8),
    )
  } else {
    FarmaciaExtendedColors(
      success = EmeraldSuccess,
      onSuccess = Color.White,
      successContainer = EmeraldSuccessContainer,
      warning = AmberWarning,
      onWarning = Color(0xFF451A03),
      warningContainer = AmberWarningContainer,
      chartIndigo = ChartIndigo,
      chartEmerald = ChartEmerald,
      chartAmber = ChartAmber,
      chartCyan = ChartCyan,
      chartViolet = ChartViolet,
      chartSlate = ChartSlate,
    )
  }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      runCatching {
        val window = view.context.findActivity()?.window
        if (window != null) {
          val controller = WindowCompat.getInsetsController(window, view)
          controller.isAppearanceLightStatusBars = !darkTheme
          controller.isAppearanceLightNavigationBars = !darkTheme
        }
      }
    }
  }

  CompositionLocalProvider(LocalFarmaciaColors provides extendedColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = FarmaciaTypography,
      shapes = FarmaciaShapes,
      content = content,
    )
  }
}

object FarmaciaColors {
  val extended: FarmaciaExtendedColors
    @Composable get() = LocalFarmaciaColors.current
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}
