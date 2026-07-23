package com.shreefintech.paytouchconsumer.electricity.model

import androidx.annotation.DrawableRes

data class TransactionItem(
    val mobileNumber: String,
    val transactionId: String,
    val amount: String,
    val status: String,
    @DrawableRes val categoryIconRes: Int,
    val username: String,
    val date: String,
    val platformFee: String,
    val totalPayable: String,
    val referenceId: String,
    val userId: String,
    val accountNumber: String,
    val companyName: String
)