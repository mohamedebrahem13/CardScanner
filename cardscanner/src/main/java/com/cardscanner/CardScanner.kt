package com.cardscanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import com.cardscanner.config.CardScannerConfig
import com.cardscanner.ui.CardScannerActivity

object CardScanner {

    fun createIntent(
        context: Context,
        config: CardScannerConfig = CardScannerConfig()
    ): Intent {
        return Intent(context, CardScannerActivity::class.java).apply {
            putExtra(CardScannerActivity.EXTRA_CONFIG, config)
        }
    }
    fun parseResult(resultCode: Int, data: Intent?): CardScannerResult? {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return null
        }

        return CardScannerResult(
            cardNumber = data.getStringExtra(CardScannerActivity.EXTRA_CARD_NUMBER),
            expiryDate = data.getStringExtra(CardScannerActivity.EXTRA_EXPIRY_DATE),
            cardHolderName = data.getStringExtra(CardScannerActivity.EXTRA_CARD_HOLDER_NAME)
        )
    }

    fun parseActivityResult(result: ActivityResult): CardScannerResult? {
        return parseResult(
            resultCode = result.resultCode,
            data = result.data
        )
    }
}