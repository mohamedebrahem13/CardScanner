package com.cardscanner.ui

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardscanner.CardScannerResult
import com.cardscanner.R
import com.cardscanner.config.CardScannerColorScheme
import com.cardscanner.config.CardScannerConfig
import com.cardscanner.config.CardScannerLanguage
import com.cardscanner.config.CardScannerThemeMode
import com.cardscanner.view_model.CardScannerViewModel
import java.util.Locale

@Composable
internal fun CardScannerScreen(
    config: CardScannerConfig,
    onCancel: () -> Unit,
    onCardDetected: (CardScannerResult) -> Unit,
    viewModel: CardScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scannerColors =
        rememberCardScannerColors(
            config = config
        )

    val scannerLocalization =
        rememberCardScannerLocalization(
            language = config.language
        )

    val scannerStrings =
        rememberCardScannerStrings(
            localizedContext =
                scannerLocalization.localizedContext,
            config = config
        )

    val torchAvailable by
    viewModel
        .torchAvailable
        .collectAsStateWithLifecycle()

    val torchEnabled by
    viewModel
        .torchEnabled
        .collectAsStateWithLifecycle()

    val currentOnCardDetected by
    rememberUpdatedState(onCardDetected)

    val previewView =
        remember(context) {
            PreviewView(context).apply {
                scaleType =
                    PreviewView.ScaleType.FILL_CENTER
            }
        }

    /*
     * Only scanner-behaviour changes restart CameraX.
     * Theme and language changes only recompose the UI.
     */
    DisposableEffect(
        viewModel,
        lifecycleOwner,
        previewView,
        config.scanStabilizationMs,
        config.minCardNumberDetections,
        config.requireExpiryDate
    ) {
        viewModel.startScanner(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            config = config
        )

        onDispose {
            viewModel.stopScanner()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.results.collect { result ->
            currentOnCardDetected(result)
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides
                scannerLocalization.layoutDirection
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    scannerColors.screenBackgroundColor
                )
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    previewView
                }
            )

            ScannerOverlay(
                config = config,
                strings = scannerStrings,
                colors = scannerColors,
                torchAvailable = torchAvailable,
                torchEnabled = torchEnabled,
                onTorchClick = viewModel::toggleTorch,
                onCancel = {
                    viewModel.stopScanner()
                    onCancel()
                }
            )
        }
    }
}

@Composable
private fun ScannerOverlay(
    config: CardScannerConfig,
    strings: ResolvedCardScannerStrings,
    colors: ResolvedCardScannerColors,
    torchAvailable: Boolean,
    torchEnabled: Boolean,
    onTorchClick: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        TopBar(
            titleText = strings.title,
            closeContentDescription = strings.close,
            turnFlashlightOnText = strings.turnFlashlightOn,
            turnFlashlightOffText = strings.turnFlashlightOff,
            colors = colors,
            torchAvailable = torchAvailable,
            torchEnabled = torchEnabled,
            onTorchClick = onTorchClick,
            onCancel = onCancel,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    WindowInsets
                        .statusBars
                        .asPaddingValues()
                )
        )

        CardFrame(
            config = config,
            frameText = strings.frame,
            frameColor = colors.frameColor,
            modifier =
                Modifier.align(
                    Alignment.Center
                )
        )

        BottomInstructions(
            instructionText = strings.instruction,
            backgroundColor =
                colors.instructionBackgroundColor,
            contentColor =
                colors.instructionContentColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    WindowInsets
                        .navigationBars
                        .asPaddingValues()
                )
        )
    }
}

@Composable
private fun TopBar(
    titleText: String,
    closeContentDescription: String,
    turnFlashlightOnText: String,
    turnFlashlightOffText: String,
    colors: ResolvedCardScannerColors,
    torchAvailable: Boolean,
    torchEnabled: Boolean,
    onTorchClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val flashlightContentDescription =
        if (torchEnabled) {
            turnFlashlightOffText
        } else {
            turnFlashlightOnText
        }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                colors.topBarBackgroundColor
            )
            .padding(
                horizontal = 20.dp,
                vertical = 12.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .size(44.dp)
                .background(
                    color =
                        colors.controlBackgroundColor,
                    shape = CircleShape
                )
                .semantics {
                    contentDescription =
                        closeContentDescription
                }
        ) {
            Text(
                text = "\u00D7",
                color =
                    colors.controlContentColor,
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall
            )
        }

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Text(
            text = titleText,
            modifier = Modifier.weight(1f),
            color = colors.topBarContentColor,
            style =
                MaterialTheme
                    .typography
                    .titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        if (torchAvailable) {
            val torchBackgroundColor =
                if (torchEnabled) {
                    colors.activeControlBackgroundColor
                } else {
                    colors.controlBackgroundColor
                }

            val torchContentColor =
                if (torchEnabled) {
                    colors.activeControlContentColor
                } else {
                    colors.controlContentColor
                }

            IconButton(
                onClick = onTorchClick,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = torchBackgroundColor,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector =
                        if (torchEnabled) {
                            Icons.Outlined.FlashOff
                        } else {
                            Icons.Outlined.FlashOn
                        },
                    contentDescription =
                        flashlightContentDescription,
                    tint = torchContentColor
                )
            }
        } else {
            Spacer(
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
private fun CardFrame(
    config: CardScannerConfig,
    frameText: String,
    frameColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(
                config.frameWidthDp.dp
            )
            .height(
                config.frameHeightDp.dp
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 2.dp,
                    color = frameColor,
                    shape =
                        RoundedCornerShape(
                            config
                                .frameCornerRadiusDp
                                .dp
                        )
                )
        )

        Text(
            text = frameText,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = 18.dp,
                    start = 12.dp,
                    end = 12.dp
                ),
            color = frameColor,
            textAlign = TextAlign.Center,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            fontWeight = FontWeight.Medium
        )

        CornerMarks(
            frameColor = frameColor,
            cornerRadiusDp =
                config.frameCornerRadiusDp
        )
    }
}

@Composable
private fun CornerMarks(
    frameColor: Color,
    cornerRadiusDp: Int
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val strokeWidth = 6.dp.toPx()
        val cornerLength = 42.dp.toPx()
        val radius = cornerRadiusDp.dp.toPx()

        // Top left.
        drawLine(
            color = frameColor,
            start = Offset(radius, 0f),
            end = Offset(cornerLength, 0f),
            strokeWidth = strokeWidth
        )

        drawLine(
            color = frameColor,
            start = Offset(0f, radius),
            end = Offset(0f, cornerLength),
            strokeWidth = strokeWidth
        )

        // Top right.
        drawLine(
            color = frameColor,
            start = Offset(
                size.width - radius,
                0f
            ),
            end = Offset(
                size.width - cornerLength,
                0f
            ),
            strokeWidth = strokeWidth
        )

        drawLine(
            color = frameColor,
            start = Offset(
                size.width,
                radius
            ),
            end = Offset(
                size.width,
                cornerLength
            ),
            strokeWidth = strokeWidth
        )

        // Bottom left.
        drawLine(
            color = frameColor,
            start = Offset(
                radius,
                size.height
            ),
            end = Offset(
                cornerLength,
                size.height
            ),
            strokeWidth = strokeWidth
        )

        drawLine(
            color = frameColor,
            start = Offset(
                0f,
                size.height - radius
            ),
            end = Offset(
                0f,
                size.height - cornerLength
            ),
            strokeWidth = strokeWidth
        )

        // Bottom right.
        drawLine(
            color = frameColor,
            start = Offset(
                size.width - radius,
                size.height
            ),
            end = Offset(
                size.width - cornerLength,
                size.height
            ),
            strokeWidth = strokeWidth
        )

        drawLine(
            color = frameColor,
            start = Offset(
                size.width,
                size.height - radius
            ),
            end = Offset(
                size.width,
                size.height - cornerLength
            ),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
private fun BottomInstructions(
    instructionText: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 24.dp,
                vertical = 32.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Bottom
    ) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = instructionText,
                modifier = Modifier.padding(
                    horizontal = 18.dp,
                    vertical = 14.dp
                ),
                color = contentColor,
                textAlign = TextAlign.Center,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )
        }
    }
}

private data class ResolvedCardScannerStrings(
    val title: String,
    val instruction: String,
    val frame: String,
    val turnFlashlightOn: String,
    val turnFlashlightOff: String,
    val close: String
)

private data class ResolvedCardScannerLocalization(
    val localizedContext: Context,
    val layoutDirection: LayoutDirection
)

private data class ResolvedCardScannerColors(
    val screenBackgroundColor: Color,
    val topBarBackgroundColor: Color,
    val topBarContentColor: Color,
    val controlBackgroundColor: Color,
    val controlContentColor: Color,
    val activeControlBackgroundColor: Color,
    val activeControlContentColor: Color,
    val frameColor: Color,
    val instructionBackgroundColor: Color,
    val instructionContentColor: Color
)

@Composable
private fun rememberCardScannerStrings(
    localizedContext: Context,
    config: CardScannerConfig
): ResolvedCardScannerStrings {
    val localeTags =
        localizedContext
            .resources
            .configuration
            .locales
            .toLanguageTags()

    return remember(
        localizedContext,
        localeTags,
        config.titleTextResId,
        config.instructionTextResId,
        config.placeCardTextResId
    ) {
        ResolvedCardScannerStrings(
            title =
                localizedContext.getString(
                    config.titleTextResId
                ),
            instruction =
                localizedContext.getString(
                    config.instructionTextResId
                ),
            frame =
                localizedContext.getString(
                    config.placeCardTextResId
                ),
            turnFlashlightOn =
                localizedContext.getString(
                    R.string.card_scanner_turn_flashlight_on
                ),
            turnFlashlightOff =
                localizedContext.getString(
                    R.string.card_scanner_turn_flashlight_off
                ),
            close =
                localizedContext.getString(
                    R.string.card_scanner_close
                )
        )
    }
}

@Composable
private fun rememberCardScannerLocalization(
    language: CardScannerLanguage
): ResolvedCardScannerLocalization {
    val baseContext = LocalContext.current
    val systemConfiguration = LocalConfiguration.current
    val systemLayoutDirection = LocalLayoutDirection.current

    /*
     * Reading the locale tags makes SYSTEM mode recompose
     * when the device/app locale changes.
     */
    val systemLocaleTags =
        systemConfiguration
            .locales
            .toLanguageTags()

    return remember(
        baseContext,
        language,
        systemLocaleTags,
        systemLayoutDirection
    ) {
        when (language) {
            CardScannerLanguage.SYSTEM -> {
                /*
                 * Do not override the locale. The resources
                 * attached to the scanner Activity decide.
                 */
                ResolvedCardScannerLocalization(
                    localizedContext = baseContext,
                    layoutDirection = systemLayoutDirection
                )
            }

            CardScannerLanguage.ENGLISH,
            CardScannerLanguage.ARABIC -> {
                val languageTag =
                    requireNotNull(
                        language.languageTag
                    ) {
                        "Forced scanner language must have a language tag."
                    }

                val forcedLocale =
                    Locale.forLanguageTag(languageTag)

                val localizedConfiguration =
                    Configuration(
                        baseContext.resources.configuration
                    ).apply {
                        setLocale(forcedLocale)
                        setLayoutDirection(forcedLocale)
                    }

                val localizedContext =
                    baseContext.createConfigurationContext(
                        localizedConfiguration
                    )

                val layoutDirection =
                    if (
                        localizedConfiguration.layoutDirection ==
                        View.LAYOUT_DIRECTION_RTL
                    ) {
                        LayoutDirection.Rtl
                    } else {
                        LayoutDirection.Ltr
                    }

                ResolvedCardScannerLocalization(
                    localizedContext = localizedContext,
                    layoutDirection = layoutDirection
                )
            }
        }
    }
}

@Composable
private fun rememberCardScannerColors(
    config: CardScannerConfig
): ResolvedCardScannerColors {
    val systemIsDark =
        isSystemInDarkTheme()

    val useDarkColors =
        when (config.themeMode) {
            CardScannerThemeMode.SYSTEM ->
                systemIsDark

            CardScannerThemeMode.LIGHT ->
                false

            CardScannerThemeMode.DARK ->
                true
        }

    val sourceColors =
        if (useDarkColors) {
            config.darkColors
        } else {
            config.lightColors
        }

    return remember(sourceColors) {
        sourceColors.toResolvedColors()
    }
}

private fun CardScannerColorScheme.toResolvedColors():
        ResolvedCardScannerColors {
    return ResolvedCardScannerColors(
        screenBackgroundColor =
            screenBackgroundColor.toComposeColor(),
        topBarBackgroundColor =
            topBarBackgroundColor.toComposeColor(),
        topBarContentColor =
            topBarContentColor.toComposeColor(),
        controlBackgroundColor =
            controlBackgroundColor.toComposeColor(),
        controlContentColor =
            controlContentColor.toComposeColor(),
        activeControlBackgroundColor =
            activeControlBackgroundColor.toComposeColor(),
        activeControlContentColor =
            activeControlContentColor.toComposeColor(),
        frameColor =
            frameColor.toComposeColor(),
        instructionBackgroundColor =
            instructionBackgroundColor.toComposeColor(),
        instructionContentColor =
            instructionContentColor.toComposeColor()
    )
}

private fun String.toComposeColor(): Color {
    return Color(toColorInt())
}
