package com.v2ray.ang.ui.compose

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val LightColor = lightColorScheme(
    primary = Color(0xFFFFFFFF), // White, so filled buttons read on the port-red background
    onPrimary = Color(0xFF8B0000), // Container red
    primaryContainer = Color(0xFFF3DDBA), // Sand
    onPrimaryContainer = Color(0xFF4A1500), // Deep rust
    secondary = Color(0xFFE8C08A), // Tan / rope
    onSecondary = Color(0xFF3B2410), // Dark cargo brown
    secondaryContainer = Color(0xFF6B4423), // Cargo brown
    onSecondaryContainer = Color(0xFFF3DDBA), // Sand
    tertiary = Color(0xFF8FD6D2), // Harbor teal
    onTertiary = Color(0xFF04302F), // Deep harbor
    tertiaryContainer = Color(0xFF0F4C4A), // Deep harbor
    onTertiaryContainer = Color(0xFFB7EDE9), // Foam
    error = Color(0xFFFFB4AB), // Signal red (light)
    errorContainer = Color(0xFF690005), // Deep rust
    onError = Color(0xFF690005), // Deep rust
    onErrorContainer = Color(0xFFFFDAD6), // Pale red
    background = Color(0xFF8B0000), // Container red
    onBackground = Color(0xFFFFFFFF), // White
    surface = Color(0xFF8B0000), // Container red
    onSurface = Color(0xFFFFFFFF), // White
    surfaceVariant = Color(0xFF6B4423), // Cargo brown
    onSurfaceVariant = Color(0xFFF3DDBA), // Sand
    outline = Color(0xFFC8A27A), // Weathered tan
    outlineVariant = Color(0xFF8B5E3C), // Rust brown
    inverseSurface = Color(0xFFF3DDBA), // Sand
    inverseOnSurface = Color(0xFF3B2410), // Dark cargo brown
    inversePrimary = Color(0xFFE8C08A), // Tan
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFFE8C08A), // Tan
    surfaceContainerLowest = Color(0xFF5E1A0F), // Deep rust
    surfaceContainerLow = Color(0xFF73210F), // Rust
    surfaceContainer = Color(0xFF7F2612), // Rust
    surfaceContainerHigh = Color(0xFF8B2E15), // Rust
    surfaceContainerHighest = Color(0xFF96351A), // Weathered rust
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFFFFA07F), // Rust, readable on dark cargo surfaces
    onPrimary = Color(0xFF4A1500), // Deep rust
    primaryContainer = Color(0xFF7A2A0C), // Rust
    onPrimaryContainer = Color(0xFFFFD9C7), // Pale rust
    secondary = Color(0xFFE8C08A), // Tan / rope
    onSecondary = Color(0xFF3B2410), // Dark cargo brown
    secondaryContainer = Color(0xFF5C3617), // Cargo brown
    onSecondaryContainer = Color(0xFFF7E3C6), // Sand
    tertiary = Color(0xFF8FD6D2), // Harbor teal
    onTertiary = Color(0xFF04302F), // Deep harbor
    tertiaryContainer = Color(0xFF0F4C4A), // Deep harbor
    onTertiaryContainer = Color(0xFFB7EDE9), // Foam
    error = Color(0xFFFFB4AB), // Signal red (light)
    errorContainer = Color(0xFF690005), // Deep rust
    onError = Color(0xFF690005), // Deep rust
    onErrorContainer = Color(0xFFFFDAD6), // Pale red
    background = Color(0xFF26140D), // Hold brown (near black)
    onBackground = Color(0xFFFFFFFF), // White
    surface = Color(0xFF26140D), // Hold brown
    onSurface = Color(0xFFFFFFFF), // White
    surfaceVariant = Color(0xFF4A2C1A), // Cargo brown
    onSurfaceVariant = Color(0xFFF0D9B5), // Sand
    outline = Color(0xFFA97C5A), // Weathered tan
    outlineVariant = Color(0xFF4A2C1A), // Cargo brown
    inverseSurface = Color(0xFFF3DDBA), // Sand
    inverseOnSurface = Color(0xFF3B2410), // Dark cargo brown
    inversePrimary = Color(0xFF8B0000), // Container red
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFFE8C08A), // Tan
    surfaceContainerLowest = Color(0xFF1C0F09), // Cargo hold
    surfaceContainerLow = Color(0xFF26140D), // Hold brown
    surfaceContainer = Color(0xFF2E1910), // Hold brown
    surfaceContainerHigh = Color(0xFF3A1F14), // Cargo brown
    surfaceContainerHighest = Color(0xFF452619), // Cargo brown
)

// Semantic colors. These sit on the port-red background rather than on a neutral surface, so each
// is a light tint of its hue: dark hues would disappear into the background.
// Contrast on background (#8B0000 light / #26140D dark): ping 6.1/10.7, pingRed 5.1/8.9,
// configType 5.9/13.4.
val colorPing = Color(0xFF8FD6D2) // Harbor teal, matches tertiary
val colorPingRed = Color(0xFFFFA07F) // Rust, matches primary
val colorConfigType = Color(0xFFE8C08A) // Tan, matches secondary

// The run button follows the primary pair: white fill with an onPrimary glyph. The inactive fill is
// white at 20%, which composites to a muted red on the port background and to grey on a neutral one.
val colorFabActive = Color(0xFFFFFFFF) // White
val colorFabInactiveLight = Color(0x33FFFFFF) // 20% white on a light background
val colorFabInactiveDark = Color(0x33FFFFFF) // 20% white on a dark background
val dividerColorLight = Color(0xFFE0E0E0) // Light Gray
val dividerColorDark = Color(0x3DFFFFFF) // 24% white, reads on red and on near-black

// Toast colors, 85% opaque. Hues stay semantic, tones come from the port palette.
val toastNormalBgLight = Color(0xD95C2E14) // Cargo brown
val toastNormalBgDark = Color(0xD93B2410) // Deep cargo brown
val toastSuccessBg = Color(0xD90F4C4A) // Harbor teal
val toastErrorBg = Color(0xD97A1F00) // Rust
val toastInfoBg = Color(0xD92F4A55) // Steel / harbor water
val toastIconCircleBg = Color(0x33FFFFFF) // Semi-transparent White
val toastTextColor = Color.White // White

object ThemeManager {
    private val _dynamicColorEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, true)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun setDynamicColorEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun refresh() {
        _dynamicColorEnabled.value =
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, true)
    }
}

/**
 * This build is dark only: the port palette is designed for a dark surface, and the light and
 * follow-system options were removed along with the UI mode setting.
 */
@Composable
fun resolveDarkTheme(): Boolean = true

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColor
        else -> LightColor
    }
    val snackbarController = rememberAppSnackbarController()

    // The scheme's background can be dark even in light mode (a dark brand colour), so chrome and
    // light-on-dark assets follow the background's luminance rather than the light/dark mode flag.
    val darkBackground = colorScheme.background.luminance() < 0.5f

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkBackground
                isAppearanceLightNavigationBars = !darkBackground
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkBackground,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
