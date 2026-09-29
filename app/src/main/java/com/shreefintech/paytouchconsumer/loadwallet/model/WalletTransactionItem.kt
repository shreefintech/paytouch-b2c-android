package com.shreefintech.paytouchconsumer.loadwallet.model

import androidx.annotation.DrawableRes
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletHistoryItem
import com.shreefintech.paytouchconsumer.utill.Utility

data class WalletTransactionItem(
    val title: String,
    val date: String,
    val amount: String,
    val isCredit: Boolean,
    val transactionId: String,
    @DrawableRes val categoryIconRes: Int
) {
    companion object {
        fun from(item: WalletHistoryItem) = WalletTransactionItem(
            title           = item.serviceName ?: "--",
            date            = Utility.formatDate(item.createdAt, "dd MMM yyyy"),
            amount          = Utility.formatAmount(item.amount),
            isCredit        = item.type?.uppercase() == "CREDIT",
            transactionId   = item.transactionId ?: "",
            categoryIconRes = sourceToIcon(item.serviceName)
        )

        @DrawableRes
        private fun sourceToIcon(source: String?): Int {
            val serviceName = source?.lowercase().orEmpty()

            return when {
                "dth" in serviceName -> R.drawable.img_dth
                "electric" in serviceName -> R.drawable.img_electricity
                "gas" in serviceName -> R.drawable.img_gas
                "postpaid" in serviceName -> R.drawable.img_postpaid
                "prepaid" in serviceName -> R.drawable.img_prepaid
                "mobile" in serviceName -> R.drawable.img_prepaid
                "fastag" in serviceName -> R.drawable.img_fastag
                "loan" in serviceName -> R.drawable.img_loan
                "municipal" in serviceName -> R.drawable.img_municipal_tax
                "tax" in serviceName -> R.drawable.img_municipal_tax
                else -> R.drawable.img_load_wallet
            }
        }
    }
}
