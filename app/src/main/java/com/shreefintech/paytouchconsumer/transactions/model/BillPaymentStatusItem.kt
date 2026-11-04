package com.shreefintech.paytouchconsumer.transactions.model

// BillPayActivity → BillPaymentStatusActivity, from the billpay/pay response. category = CategoryType.value
data class BillPaymentStatusItem(
    val clientReference: String,
    val txnReference: String,
    val amount: String,
    val status: String,
    val isFinal: Boolean,
    val category: String
)
