package com.shreefintech.paytouchconsumer.transactions.model

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
    val isMobileCategory: Boolean = false,
    val isVehicleCategory: Boolean = false,
    // Server identifier_label (e.g. "Consumer Number"); null → label from the category flags
    val identifierLabel: String? = null,
    var isExpanded: Boolean = false
)
