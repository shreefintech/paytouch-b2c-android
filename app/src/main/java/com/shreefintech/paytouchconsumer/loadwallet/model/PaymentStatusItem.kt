package com.shreefintech.paytouchconsumer.loadwallet.model

data class PaymentStatusItem(
    val orderId: String,
    val amount: String,
    val status: String
)
