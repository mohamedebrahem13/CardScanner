Card Scanner

Android card scanner library built with CameraX, ML Kit, Jetpack Compose, and Hilt.

It detects card details directly from the camera, including:

Card number with Luhn validation

Expiry date in MM/YY format

Uppercase cardholder name from the lower area of the card

Torch support

Camera focus and exposure handling

Scan stabilization across multiple frames

Light / dark theme support

English / Arabic support

Maven-ready Android AAR distribution

Requirements

Android API 29+

Compile SDK 37+

Java 17

Camera permission

Installation

Once version 1.0.0 is available on Maven Central, add:

dependencies {
    implementation("io.github.mohamedebrahem13:card-scanner:1.0.0")
}

Make sure Maven Central is included:

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

Permissions

Add the camera permission to your app manifest:

<uses-permission android:name="android.permission.CAMERA" />

Your application should request the runtime camera permission before launching the scanner.

Basic Usage

Create a scanner configuration:

val config = CardScannerConfig(
    scanStabilizationMs = 400L,
    minCardNumberDetections = 2,
    requireExpiryDate = false
)

Launch the scanner using the library API:

val intent = CardScanner.createIntent(
    context = context,
    config = config
)

scannerLauncher.launch(intent)

Handle the result:

val scannerLauncher =
    registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->

        val result =
            CardScanner.parseActivityResult(
                activityResult
            )

        result?.let { card ->
            val cardNumber = card.cardNumber
            val expiryDate = card.expiryDate
            val cardHolderName = card.cardHolderName
        }
    }

Compose Example

@Composable
fun ScanCardButton() {
    val context = LocalContext.current

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { activityResult ->

            val result =
                CardScanner.parseActivityResult(
                    activityResult
                )

            result?.let { card ->
                println("Card detected")
                println("Expiry: ${card.expiryDate}")
                println("Holder: ${card.cardHolderName}")
            }
        }

    Button(
        onClick = {
            val config =
                CardScannerConfig(
                    scanStabilizationMs = 400L,
                    minCardNumberDetections = 2,
                    requireExpiryDate = false
                )

            launcher.launch(
                CardScanner.createIntent(
                    context = context,
                    config = config
                )
            )
        }
    ) {
        Text("Scan Card")
    }
}

Avoid logging or storing full payment-card numbers in production logs.

Scanner Behavior

Card Number

The scanner uses ML Kit OCR to detect the card number.

Detected values are validated using the Luhn algorithm before they are accepted.

OCR-confusable characters may be normalized when appropriate, for example:

O -> 0
I -> 1
Z -> 2
S -> 5
B -> 8

A candidate is never accepted unless it passes card-number validation.

Expiry Date

Expiry dates must contain a real /.

Accepted examples:

08/29
08 / 29
08/2029

Rejected examples:

0829
08 29
08-29
08.29

The returned value is normalized to:

MM/YY

Cardholder Name

The scanner looks for the cardholder name only in the bottom area of the physical card.

The holder name is expected to be uppercase.

Accepted:

MOHAMED EBRAHEM
JOHN MICHAEL SMITH

Rejected:

Mohamed Ebrahem
john smith

Known card-network and card-type labels such as VISA, MASTERCARD, DEBIT, CREDIT, PLATINUM, and similar values are ignored.

Configuration

Example:

val config =
    CardScannerConfig(
        themeMode = CardScannerThemeMode.SYSTEM,
        language = CardScannerLanguage.SYSTEM,
        frameWidthDp = 330,
        frameHeightDp = 210,
        frameCornerRadiusDp = 18,
        scanStabilizationMs = 400L,
        minCardNumberDetections = 2,
        requireExpiryDate = false
    )

scanStabilizationMs

Controls how long the scanner waits before returning a stable result.

minCardNumberDetections

Controls how many times the same valid card number must be detected before it can be returned.

requireExpiryDate

When set to true, the scanner does not complete until a valid expiry date containing / is detected.

Result

The scanner returns:

CardScannerResult(
    cardNumber = "...",
    expiryDate = "08/29",
    cardHolderName = "MOHAMED EBRAHEM"
)

Fields such as expiry date and cardholder name may be nullable depending on configuration and OCR visibility.

Technology

The library uses:

CameraX

Google ML Kit Text Recognition

Jetpack Compose

Kotlin Coroutines

Hilt

Android Lifecycle / ViewModel

R8 / ProGuard consumer rules

Maven Publish

Security Notes

Payment-card data is sensitive.

Applications using this library should:

Never log full card numbers

Avoid storing card details unless absolutely required

Mask PAN values in debug and analytics systems

Use secure transport for any card-related network communication

Follow applicable PCI DSS requirements when processing or storing payment-card data

This library performs local OCR and parsing. It does not itself provide payment processing or PCI compliance.

Build From Source

Clone the repository:

git clone https://github.com/mohamedebrahem13/CardScanner.git
cd CardScanner

Build the library:

./gradlew :cardscanner:assembleRelease

Publish to the local Maven test repository:

./gradlew :cardscanner:publishReleasePublicationToLocalTestRepository

The generated Maven artifacts are written under:

cardscanner/build/maven-repository/

Maven Coordinates

Group:    io.github.mohamedebrahem13
Artifact: card-scanner
Version:  1.0.0

Dependency:

implementation("io.github.mohamedebrahem13:card-scanner:1.0.0")

Repository

https://github.com/mohamedebrahem13/CardScanner

License

Licensed under the Apache License 2.0.

See the LICENSE file for details.
