package com.shreefintech.paytouchconsumer.transactions.model

data class BillPaymentStatusItem(
    val transactionId: String,
    val amount: String,
    val status: String,
    val category: String
)
