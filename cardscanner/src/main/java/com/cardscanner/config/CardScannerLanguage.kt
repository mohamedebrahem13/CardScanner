package com.cardscanner.config


/**
 * Controls only the card-scanner UI language.
 *
 * SYSTEM follows the locale available to the scanner Activity.
 * ENGLISH and ARABIC force the scanner resources for that session.
 */
enum class CardScannerLanguage(
    val languageTag: String?
) {
    SYSTEM(null),
    ENGLISH("en"),
    ARABIC("ar")
}