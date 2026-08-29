

Card numbers are detected using ML Kit OCR and validated with the Luhn algorithm.

Expiry Date

Expiry dates must contain an actual /.

Accepted

08/29
08 / 29
08/2029

Rejected

0829
08 29
08-29
08.29

Returned values are normalized to:

MM/YY

Cardholder Name

The scanner searches for the cardholder name only in the bottom section of the physical card.

The expected holder name is uppercase.

Accepted

MOHAMED EBRAHEM
JOHN MICHAEL SMITH

Rejected

Mohamed Ebrahem
john smith

Main Configuration Options

themeMode

CardScannerThemeMode.SYSTEM
CardScannerThemeMode.LIGHT
CardScannerThemeMode.DARK

language

CardScannerLanguage.SYSTEM
CardScannerLanguage.ENGLISH
CardScannerLanguage.ARABIC

scanStabilizationMs

Controls how long the scanner waits before returning a stable result.

scanStabilizationMs = 400L

minCardNumberDetections

Controls how many times the same valid card number must be detected.

minCardNumberDetections = 2

requireExpiryDate

When enabled, the scanner waits for a valid expiry date before returning a result.

requireExpiryDate = true

Result

CardScannerResult(
    cardNumber = "...",
    expiryDate = "08/29",
    cardHolderName = "MOHAMED EBRAHEM"
)

Security Notes

Payment-card information is sensitive.

Never log full card numbers

Avoid storing card details unless required

Mask PAN values in logs and analytics

Use secure transport for card-related network operations

Follow applicable PCI DSS requirements

This library performs local OCR and parsing. It does not provide payment processing or PCI compliance by itself.

Technology Stack

CameraX

Google ML Kit Text Recognition

Jetpack Compose

Kotlin Coroutines

Hilt

Android Lifecycle / ViewModel

R8 / ProGuard

Maven Publish

Build From Source

git clone https://github.com/mohamedebrahem13/CardScanner.git
cd CardScanner

Build:

./gradlew :cardscanner:assembleRelease

Publish locally:

./gradlew :cardscanner:publishReleasePublicationToLocalTestRepository

Repository

GitHub:
https://github.com/mohamedebrahem13/CardScanner

License

Licensed under the Apache License 2.0.

See the LICENSE file for details.
