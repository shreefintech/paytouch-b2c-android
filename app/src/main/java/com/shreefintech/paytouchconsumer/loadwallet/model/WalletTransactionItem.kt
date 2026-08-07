package com.shreefintech.paytouchconsumer.loadwallet.model

import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletHistoryItem
import com.shreefintech.paytouchconsumer.utill.Utility

data class WalletTransactionItem(
    val title: String,
    val date: String,
    val amount: String,
    val isCredit: Boolean
) {
    companion object {
        fun from(item: WalletHistoryItem) = WalletTransactionItem(
            title    = item.serviceName ?: "--",
            date     = Utility.formatDate(item.createdAt, "dd MMM yyyy"),
            amount   = Utility.formatAmount(item.amount),
            isCredit = item.type?.uppercase() == "CREDIT"
        )
    }
}
