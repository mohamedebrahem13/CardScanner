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
import androidx.compose.foundation.layout.fillMaxSize
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

    private var scannedCardResult by mutableStateOf<CardScannerResult?>(null)

    private val cardScannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val cardData = CardScanner.parseActivityResult(result)

            if (cardData != null) {
                scannedCardResult = cardData
            } else {
                Toast.makeText(this, "Card scan cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCardScanner()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            CardScannerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        ) {
                            Text(text = "Scan Card")
                        }

                        scannedCardResult?.let { result ->
                            Text(
                                text = """ 
                                    Card Number: ${result.cardNumber.orEmpty()}
                                    Expiry Date: ${result.expiryDate.orEmpty()}
                                    Holder Name: ${result.cardHolderName.orEmpty()}
                                """.trimIndent(),
                                modifier = Modifier.padding(top = 24.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openCardScanner() {
        cardScannerLauncher.launch(
            CardScanner.createIntent(
                this,
                CardScannerConfig(
                    themeMode = CardScannerThemeMode.SYSTEM,
                    language = CardScannerLanguage.ARABIC,
                    lightColors =
                        CardScannerColorScheme
                            .lightDefaults()
                            .copy(
                                frameColor = "#FFD32F2F",
                                activeControlBackgroundColor =
                                    "#FFD32F2F",
                                activeControlContentColor =
                                    "#FFFFFFFF"
                            ),

                    darkColors =
                        CardScannerColorScheme
                            .darkDefaults()
                            .copy(
                                frameColor = "#FFFF5252",
                                activeControlBackgroundColor =
                                    "#FFFF5252",
                                activeControlContentColor =
                                    "#FF000000"
                            ),

                    scanStabilizationMs = 400L,
                    minCardNumberDetections = 2,
                    requireExpiryDate = false
                )
            )
        )
    }
}