package com.task.cardscanner

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cardscanner.CardScanner
import com.cardscanner.CardScannerResult
import com.cardscanner.config.CardScannerColorScheme
import com.cardscanner.config.CardScannerConfig
import com.cardscanner.config.CardScannerLanguage
import com.cardscanner.config.CardScannerThemeMode
import com.task.cardscanner.ui.theme.CardScannerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var scannedCardResult by
    mutableStateOf<CardScannerResult?>(
        null
    )

    /*
     * We keep the config that the user selected
     * until camera permission is granted.
     */
    private var pendingScannerConfig:
            CardScannerConfig? =
        null

    private val cardScannerLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            val cardData =
                CardScanner.parseActivityResult(
                    result
                )

            if (cardData != null) {
                scannedCardResult =
                    cardData
            } else {
                Toast
                    .makeText(
                        this,
                        "Card scan cancelled",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                val config =
                    pendingScannerConfig

                if (config != null) {
                    openCardScanner(
                        config
                    )
                }

            } else {

                Toast
                    .makeText(
                        this,
                        "Camera permission is required",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }

            pendingScannerConfig =
                null
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()

        setContent {

            CardScannerTheme {

                Scaffold(
                    modifier =
                        Modifier.fillMaxSize()
                ) { innerPadding ->

                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    innerPadding
                                )
                                .padding(
                                    24.dp
                                ),
                        horizontalAlignment =
                            Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.Center
                    ) {

                        /*
                         * =====================================
                         * ENGLISH + LIGHT
                         * =====================================
                         */
                        Button(
                            onClick = {
                                requestScanner(
                                    englishLightConfig()
                                )
                            }
                        ) {
                            Text(
                                text =
                                    "English - Light"
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )

                        /*
                         * =====================================
                         * ENGLISH + DARK
                         * =====================================
                         */
                        Button(
                            onClick = {
                                requestScanner(
                                    englishDarkConfig()
                                )
                            }
                        ) {
                            Text(
                                text =
                                    "English - Dark"
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )

                        /*
                         * =====================================
                         * ARABIC + LIGHT
                         * =====================================
                         */
                        Button(
                            onClick = {
                                requestScanner(
                                    arabicLightConfig()
                                )
                            }
                        ) {
                            Text(
                                text =
                                    "Arabic - Light"
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )

                        /*
                         * =====================================
                         * ARABIC + DARK
                         * =====================================
                         */
                        Button(
                            onClick = {
                                requestScanner(
                                    arabicDarkConfig()
                                )
                            }
                        ) {
                            Text(
                                text =
                                    "Arabic - Dark"
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )

                        /*
                         * =====================================
                         * SYSTEM LANGUAGE + SYSTEM THEME
                         * =====================================
                         */
                        Button(
                            onClick = {
                                requestScanner(
                                    systemConfig()
                                )
                            }
                        ) {
                            Text(
                                text =
                                    "System Config"
                            )
                        }

                        scannedCardResult
                            ?.let { result ->

                                Text(
                                    text =
                                        """
                                        Card Number: ${result.cardNumber.orEmpty()}
                                        Expiry Date: ${result.expiryDate.orEmpty()}
                                        Holder Name: ${result.cardHolderName.orEmpty()}
                                        """.trimIndent(),
                                    modifier =
                                        Modifier.padding(
                                            top = 24.dp
                                        ),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyLarge
                                )
                            }
                    }
                }
            }
        }
    }

    /*
     * ============================================
     * PERMISSION + CONFIG
     * ============================================
     */

    private fun requestScanner(
        config: CardScannerConfig
    ) {
        pendingScannerConfig =
            config

        cameraPermissionLauncher.launch(
            Manifest.permission.CAMERA
        )
    }

    /*
     * ============================================
     * OPEN SCANNER
     * ============================================
     */

    private fun openCardScanner(
        config: CardScannerConfig
    ) {
        cardScannerLauncher.launch(
            CardScanner.createIntent(
                this,
                config
            )
        )
    }

    /*
     * ============================================
     * ENGLISH - LIGHT
     * ============================================
     */

    private fun englishLightConfig():
            CardScannerConfig {

        return CardScannerConfig(
            themeMode =
                CardScannerThemeMode.LIGHT,

            language =
                CardScannerLanguage.ENGLISH,

            lightColors =
                CardScannerColorScheme
                    .lightDefaults()
                    .copy(
                        frameColor =
                            "#FFD32F2F",
                        activeControlBackgroundColor =
                            "#FFD32F2F",
                        activeControlContentColor =
                            "#FFFFFFFF"
                    ),

            scanStabilizationMs =
                400L,

            minCardNumberDetections =
                2,

            requireExpiryDate =
                false
        )
    }

    /*
     * ============================================
     * ENGLISH - DARK
     * ============================================
     */

    private fun englishDarkConfig():
            CardScannerConfig {

        return CardScannerConfig(
            themeMode =
                CardScannerThemeMode.DARK,

            language =
                CardScannerLanguage.ENGLISH,

            darkColors =
                CardScannerColorScheme
                    .darkDefaults()
                    .copy(
                        frameColor =
                            "#FFFF5252",
                        activeControlBackgroundColor =
                            "#FFFF5252",
                        activeControlContentColor =
                            "#FF000000"
                    ),

            scanStabilizationMs =
                400L,

            minCardNumberDetections =
                2,

            requireExpiryDate =
                false
        )
    }

    /*
     * ============================================
     * ARABIC - LIGHT
     * ============================================
     */

    private fun arabicLightConfig():
            CardScannerConfig {

        return CardScannerConfig(
            themeMode =
                CardScannerThemeMode.LIGHT,

            language =
                CardScannerLanguage.ARABIC,

            lightColors =
                CardScannerColorScheme
                    .lightDefaults()
                    .copy(
                        frameColor =
                            "#FFD32F2F",
                        activeControlBackgroundColor =
                            "#FFD32F2F",
                        activeControlContentColor =
                            "#FFFFFFFF"
                    ),

            scanStabilizationMs =
                400L,

            minCardNumberDetections =
                2,

            requireExpiryDate =
                false
        )
    }

    /*
     * ============================================
     * ARABIC - DARK
     * ============================================
     */

    private fun arabicDarkConfig():
            CardScannerConfig {

        return CardScannerConfig(
            themeMode =
                CardScannerThemeMode.DARK,

            language =
                CardScannerLanguage.ARABIC,

            darkColors =
                CardScannerColorScheme
                    .darkDefaults()
                    .copy(
                        frameColor =
                            "#FFFF5252",
                        activeControlBackgroundColor =
                            "#FFFF5252",
                        activeControlContentColor =
                            "#FF000000"
                    ),

            scanStabilizationMs =
                400L,

            minCardNumberDetections =
                2,

            requireExpiryDate =
                false
        )
    }

    /*
     * ============================================
     * SYSTEM
     * ============================================
     */

    private fun systemConfig():
            CardScannerConfig {

        return CardScannerConfig(
            themeMode =
                CardScannerThemeMode.SYSTEM,

            language =
                CardScannerLanguage.SYSTEM,

            scanStabilizationMs =
                400L,

            minCardNumberDetections =
                2,

            requireExpiryDate =
                false
        )
    }
}