package com.cardscanner.config

import androidx.annotation.StringRes
import com.cardscanner.R
import java.io.Serializable

data class CardScannerConfig(
    /**
     * Determines whether the scanner follows the system
     * theme or forces light/dark mode.
     */
    val themeMode: CardScannerThemeMode =
        CardScannerThemeMode.SYSTEM,

    /**
     * SYSTEM uses the locale currently applied to the scanner Activity.
     * ENGLISH and ARABIC force only the scanner strings/layout direction.
     */
    val language: CardScannerLanguage =
        CardScannerLanguage.SYSTEM,

    val lightColors: CardScannerColorScheme =
        CardScannerColorScheme.lightDefaults(),

    val darkColors: CardScannerColorScheme =
        CardScannerColorScheme.darkDefaults(),

    val frameWidthDp: Int = 330,

    val frameHeightDp: Int = 210,

    val frameCornerRadiusDp: Int = 18,

    val scanStabilizationMs: Long = 1800L,

    val minCardNumberDetections: Int = 3,

    val requireExpiryDate: Boolean = false,

    @field:StringRes
    val instructionTextResId: Int =
        R.string.card_scanner_instruction,

    @field:StringRes
    val titleTextResId: Int =
        R.string.card_scanner_title,

    @field:StringRes
    val placeCardTextResId: Int =
        R.string.card_scanner_place_card
) : Serializable {

    init {
        require(frameWidthDp > 0) {
            "frameWidthDp must be greater than zero."
        }

        require(frameHeightDp > 0) {
            "frameHeightDp must be greater than zero."
        }

        require(frameCornerRadiusDp >= 0) {
            "frameCornerRadiusDp cannot be negative."
        }

        require(scanStabilizationMs >= 0L) {
            "scanStabilizationMs cannot be negative."
        }

        require(minCardNumberDetections > 0) {
            "minCardNumberDetections must be greater than zero."
        }
    }
}