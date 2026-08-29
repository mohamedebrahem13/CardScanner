# Card Scanner

**Android card scanner library for detecting card numbers, expiry dates, and cardholder names using CameraX and Google ML Kit.**

Card Scanner provides a configurable Android scanning experience built with Kotlin, Jetpack Compose, CameraX, ML Kit, and Hilt.

---

## Features

- Card number detection using ML Kit OCR
- Luhn validation for detected card numbers
- Expiry date recognition
- Cardholder name detection
- English language support
- Arabic language support
- Light theme
- Dark theme
- System theme
- Custom scanner colors
- Torch control
- Camera focus support
- Exposure control
- Multi-frame scan stabilization
- Jetpack Compose UI
- R8 / ProGuard support

---

## Requirements

| Requirement | Version |
|---|---|
| Minimum Android SDK | 29 |
| Compile SDK | 37+ |
| Java | 17 |
| Kotlin | Supported Android Kotlin version |

---

## Installation

Add Maven Central to your repositories:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Then add Card Scanner:

```kotlin
dependencies {
    implementation("io.github.mohamedebrahem13:card-scanner:1.0.0")
}
```

---

## Camera Permission

Add the following permission to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

The application using the library must request camera permission at runtime before opening the scanner.

---

## Basic Usage

### Register the scanner launcher

```kotlin
private val cardScannerLauncher =
    registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        val cardData =
            CardScanner.parseActivityResult(result)

        if (cardData != null) {

            val cardNumber =
                cardData.cardNumber

            val expiryDate =
                cardData.expiryDate

            val cardHolderName =
                cardData.cardHolderName
        }
    }
```

### Create a scanner configuration

```kotlin
val config =
    CardScannerConfig(
        themeMode = CardScannerThemeMode.SYSTEM,
        language = CardScannerLanguage.ENGLISH,
        scanStabilizationMs = 400L,
        minCardNumberDetections = 2,
        requireExpiryDate = false
    )
```

### Open the scanner

```kotlin
cardScannerLauncher.launch(
    CardScanner.createIntent(
        this,
        config
    )
)
```

---

# Configuration

Card Scanner supports different languages, themes, and custom colors.

## English - Light Mode

```kotlin
val config =
    CardScannerConfig(
        themeMode =
            CardScannerThemeMode.LIGHT,

        language =
            CardScannerLanguage.ENGLISH,

        lightColors =
            CardScannerColorScheme
                .lightDefaults()
                .copy(
                    frameColor = "#FFD32F2F",
                    activeControlBackgroundColor = "#FFD32F2F",
                    activeControlContentColor = "#FFFFFFFF"
                ),

        scanStabilizationMs = 400L,

        minCardNumberDetections = 2,

        requireExpiryDate = false
    )
```

---

## English - Dark Mode

```kotlin
val config =
    CardScannerConfig(
        themeMode =
            CardScannerThemeMode.DARK,

        language =
            CardScannerLanguage.ENGLISH,

        darkColors =
            CardScannerColorScheme
                .darkDefaults()
                .copy(
                    frameColor = "#FFFF5252",
                    activeControlBackgroundColor = "#FFFF5252",
                    activeControlContentColor = "#FF000000"
                ),

        scanStabilizationMs = 400L,

        minCardNumberDetections = 2,

        requireExpiryDate = false
    )
```

---

## Arabic - Light Mode

```kotlin
val config =
    CardScannerConfig(
        themeMode =
            CardScannerThemeMode.LIGHT,

        language =
            CardScannerLanguage.ARABIC,

        lightColors =
            CardScannerColorScheme
                .lightDefaults()
                .copy(
                    frameColor = "#FFD32F2F",
                    activeControlBackgroundColor = "#FFD32F2F",
                    activeControlContentColor = "#FFFFFFFF"
                ),

        scanStabilizationMs = 400L,

        minCardNumberDetections = 2,

        requireExpiryDate = false
    )
```

---

## Arabic - Dark Mode

```kotlin
val config =
    CardScannerConfig(
        themeMode =
            CardScannerThemeMode.DARK,

        language =
            CardScannerLanguage.ARABIC,

        darkColors =
            CardScannerColorScheme
                .darkDefaults()
                .copy(
                    frameColor = "#FFFF5252",
                    activeControlBackgroundColor = "#FFFF5252",
                    activeControlContentColor = "#FF000000"
                ),

        scanStabilizationMs = 400L,

        minCardNumberDetections = 2,

        requireExpiryDate = false
    )
```

---

## System Language and Theme

The scanner can automatically follow the device language and appearance:

```kotlin
val config =
    CardScannerConfig(
        themeMode =
            CardScannerThemeMode.SYSTEM,

        language =
            CardScannerLanguage.SYSTEM,

        scanStabilizationMs = 400L,

        minCardNumberDetections = 2,

        requireExpiryDate = false
    )
```

---

# Configuration Options

## Theme Mode

Available theme modes:

```kotlin
CardScannerThemeMode.LIGHT

CardScannerThemeMode.DARK

CardScannerThemeMode.SYSTEM
```

| Value | Description |
|---|---|
| `LIGHT` | Always use light mode |
| `DARK` | Always use dark mode |
| `SYSTEM` | Follow device theme |

---

## Language

Available languages:

```kotlin
CardScannerLanguage.ENGLISH

CardScannerLanguage.ARABIC

CardScannerLanguage.SYSTEM
```

| Value | Description |
|---|---|
| `ENGLISH` | Force English scanner UI |
| `ARABIC` | Force Arabic scanner UI |
| `SYSTEM` | Follow device language |

---

## Custom Colors

Light and dark themes can be customized independently.

### Light Colors

```kotlin
lightColors =
    CardScannerColorScheme
        .lightDefaults()
        .copy(
            frameColor = "#FFD32F2F",
            activeControlBackgroundColor =
                "#FFD32F2F",
            activeControlContentColor =
                "#FFFFFFFF"
        )
```

### Dark Colors

```kotlin
darkColors =
    CardScannerColorScheme
        .darkDefaults()
        .copy(
            frameColor = "#FFFF5252",
            activeControlBackgroundColor =
                "#FFFF5252",
            activeControlContentColor =
                "#FF000000"
        )
```

---

## Scan Stabilization

`scanStabilizationMs` controls how long a valid candidate remains stable before the scanner accepts it.

Example:

```kotlin
scanStabilizationMs = 400L
```

---

## Minimum Card Number Detections

`minCardNumberDetections` controls how many times the same valid card number must be detected.

Example:

```kotlin
minCardNumberDetections = 2
```

This can help reduce false OCR results.

---

## Require Expiry Date

Set:

```kotlin
requireExpiryDate = true
```

when the scanner should not complete until a valid expiry date has also been detected.

Use:

```kotlin
requireExpiryDate = false
```

when expiry date detection is optional.

---

# Scanner Result

The scanner returns a `CardScannerResult`.

Example:

```kotlin
val result: CardScannerResult
```

Available values:

```kotlin
result.cardNumber

result.expiryDate

result.cardHolderName
```

Example:

```kotlin
val cardNumber =
    result.cardNumber.orEmpty()

val expiryDate =
    result.expiryDate.orEmpty()

val cardHolderName =
    result.cardHolderName.orEmpty()
```

---

# Card Number Detection

The card number is detected using Google ML Kit OCR.

Before a detected card number is accepted, it is validated using the **Luhn algorithm**.

This helps reject invalid OCR candidates.

---

# Expiry Date Detection

Expiry dates must contain an actual slash.

### Accepted

```text
08/29
08 / 29
08/2029
```

### Rejected

```text
0829
08 29
08-29
08.29
```

The scanner normalizes valid expiry dates to:

```text
MM/YY
```

Example:

```text
08/2029
```

becomes:

```text
08/29
```

---

# Cardholder Name Detection

Cardholder name recognition is performed on the lower section of the card.

The scanner expects uppercase cardholder names.

### Accepted

```text
MOHAMED EBRAHEM
JOHN MICHAEL SMITH
```

### Rejected

```text
Mohamed Ebrahem
john smith
```

Common card-related labels are ignored when detecting the holder name.

Examples include:

```text
VISA
MASTERCARD
DEBIT
CREDIT
PLATINUM
```

---

# Testing Different Configurations

A demo application can provide separate buttons for testing each scanner configuration.

For example:

```kotlin
Button(
    onClick = {
        requestScanner(
            englishLightConfig()
        )
    }
) {
    Text("English - Light")
}
```

```kotlin
Button(
    onClick = {
        requestScanner(
            englishDarkConfig()
        )
    }
) {
    Text("English - Dark")
}
```

```kotlin
Button(
    onClick = {
        requestScanner(
            arabicLightConfig()
        )
    }
) {
    Text("Arabic - Light")
}
```

```kotlin
Button(
    onClick = {
        requestScanner(
            arabicDarkConfig()
        )
    }
) {
    Text("Arabic - Dark")
}
```

```kotlin
Button(
    onClick = {
        requestScanner(
            systemConfig()
        )
    }
) {
    Text("System Config")
}
```

---

# Security

Payment-card information is sensitive.

Applications using Card Scanner should:

- Never log full card numbers
- Mask PAN values in application logs
- Avoid storing card information unless required
- Protect card information while in memory and storage
- Use secure network communication
- Follow applicable PCI DSS requirements

> Card Scanner performs OCR and card-information extraction. It does not provide payment processing or PCI DSS compliance by itself.

---

# Technology Stack

| Technology | Purpose |
|---|---|
| CameraX | Camera preview and frame analysis |
| ML Kit | Text recognition |
| Jetpack Compose | Scanner user interface |
| Kotlin Coroutines | Asynchronous processing |
| Hilt | Dependency injection |
| ViewModel | Scanner state management |
| R8 / ProGuard | Release optimization |
| Maven Publish | Library distribution |

---

# Build From Source

Clone the repository:

```bash
git clone https://github.com/mohamedebrahem13/CardScanner.git
```

Open the project:

```bash
cd CardScanner
```

Build the release library:

```bash
./gradlew :cardscanner:assembleRelease
```

Publish to the local Maven test repository:

```bash
./gradlew :cardscanner:publishReleasePublicationToLocalTestRepository
```

Generated Maven files are located under:

```text
cardscanner/build/maven-repository/
```

---

# Maven Coordinates

```text
Group ID:    io.github.mohamedebrahem13
Artifact ID: card-scanner
Version:     1.0.0
```

Dependency:

```kotlin
implementation(
    "io.github.mohamedebrahem13:card-scanner:1.0.0"
)
```

---

# Repository

[CardScanner on GitHub](https://github.com/mohamedebrahem13/CardScanner)

---

# License

This project is licensed under the **Apache License 2.0**.

See the [LICENSE](LICENSE) file for more information.
