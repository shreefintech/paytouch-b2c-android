package com.shreefintech.paytouchconsumer.electricity.model

import androidx.annotation.DrawableRes

data class RecentTransactionItem(
    val categoryName: String,
    val accountHolderName: String,
    val date: String,
    val status: String,
    val amount: String,
    val accountNumber: String,
    val reference: String,
    @DrawableRes val categoryIconRes: Int,
    var isExpanded: Boolean = false
)