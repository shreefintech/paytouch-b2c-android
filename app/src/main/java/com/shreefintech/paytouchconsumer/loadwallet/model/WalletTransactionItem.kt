package com.shreefintech.paytouchconsumer.loadwallet.model

data class WalletTransactionItem(
    val title: String,
    val date: String,
    val amount: String,
    val isCredit: Boolean
)
