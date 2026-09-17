package com.shreefintech.paytouchconsumer.transactions.model

import androidx.annotation.DrawableRes
import com.shreefintech.paytouchconsumer.enums.CategoryType

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
    val companyName: String,
    val isMobileCategory: Boolean = false,
    val isVehicleCategory: Boolean = false,
    val categoryType: CategoryType = CategoryType.UNKNOWN
)
