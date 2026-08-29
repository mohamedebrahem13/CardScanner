package com.cardscanner.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cardscanner.CardScannerResult
import com.cardscanner.config.CardScannerConfig
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class CardScannerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CONFIG = "extra_config"

        const val EXTRA_CARD_NUMBER = "extra_card_number"
        const val EXTRA_EXPIRY_DATE = "extra_expiry_date"
        const val EXTRA_CARD_HOLDER_NAME = "extra_card_holder_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        val config = getScannerConfig()

        setContent {
            CardScannerScreen(
                config = config,
                onCancel = {
                    setResult(RESULT_CANCELED)
                    finish()
                },
                onCardDetected = { result ->
                    returnResult(result)
                }
            )
        }
    }

    private fun getScannerConfig(): CardScannerConfig {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(
                EXTRA_CONFIG,
                CardScannerConfig::class.java
            ) ?: CardScannerConfig()
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_CONFIG) as? CardScannerConfig
                ?: CardScannerConfig()
        }
    }

    private fun returnResult(result: CardScannerResult) {
        val data = Intent().apply {
            putExtra(EXTRA_CARD_NUMBER, result.cardNumber)
            putExtra(EXTRA_EXPIRY_DATE, result.expiryDate)
            putExtra(EXTRA_CARD_HOLDER_NAME, result.cardHolderName)
        }

        setResult(RESULT_OK, data)
        finish()
    }
}