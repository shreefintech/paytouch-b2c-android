package com.shreefintech.paytouchconsumer.electricity.model

data class SmsReceiptItem(
    val mobile: String,
    val txnId: String,
    val amount: String,
    val status: String,
    val username: String,
    val date: String,
    val platformFee: String,
    val refId: String,
    val accountNo: String,
    val companyName: String
)
