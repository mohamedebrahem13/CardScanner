package com.cardscanner

data class CardScannerResult(
    val cardNumber: String?,
    val expiryDate: String?,
    val cardHolderName: String?
)