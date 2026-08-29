package com.cardscanner.config

enum class CardScannerThemeMode {
    /**
     * Follow the client application's current night mode.
     */
    SYSTEM,

    /**
     * Always use the light scanner colors.
     */
    LIGHT,

    /**
     * Always use the dark scanner colors.
     */
    DARK
}