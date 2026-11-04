package com.shreefintech.paytouchconsumer.loadwallet.model

data class PaymentStatusItem(
    val orderId: String,
    val amount: String,
    val status: String,
    // Top-up failure_reason — replaces the failed description when present
    val failureReason: String? = null
)
