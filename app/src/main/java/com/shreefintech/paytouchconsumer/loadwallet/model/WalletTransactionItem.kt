package com.shreefintech.paytouchconsumer.loadwallet.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletHistoryDetailPassItem.Source
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.BonusRewardItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletHistoryItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletStatementEntryItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletWithdrawalItem
import com.shreefintech.paytouchconsumer.utill.Utility

data class WalletTransactionItem(
    val title: String,
    val date: String,
    val amount: String,
    val isCredit: Boolean,
    val transactionId: String,
    @DrawableRes val categoryIconRes: Int,
    // Withdrawals rows: "Pending" / "Failed" under the amount; failed rows are grey and unsigned
    @StringRes val statusLabelRes: Int? = null,
    val isFailed: Boolean = false,
    // Wallet history rows open TransactionHistoryDetailActivity with this; null = legacy transactionId
    val detail: WalletHistoryDetailPassItem? = null
) {
    // Opens TransactionHistoryDetailActivity only when there is something to load or show
    val isClickable: Boolean get() = detail != null || transactionId.isNotEmpty()

    companion object {
        // Ledger types → detail call (Wallet History API doc, 2026-11-04); every other type is row-only
        private val BILLPAY_TYPES    = setOf("billpay", "recharge", "platform_fee", "refund")
        private val TOPUP_TYPES      = setOf("topup", "topup_fee")
        private val WITHDRAWAL_TYPES = setOf("withdraw", "withdraw_charge")
        private const val TYPE_CASHBACK = "cashback"

        fun from(item: WalletHistoryItem) = WalletTransactionItem(
            title           = item.serviceName ?: "--",
            date            = Utility.formatDate(item.createdAt, "dd MMM yyyy"),
            amount          = Utility.formatAmount(item.amount),
            isCredit        = item.type?.uppercase() == "CREDIT",
            transactionId   = item.transactionId ?: "",
            categoryIconRes = sourceToIcon(item.serviceName)
        )

        // GET wallet/statement row
        fun from(item: WalletStatementEntryItem) = WalletTransactionItem(
            title           = item.description?.takeIf { it.isNotBlank() } ?: item.type ?: "--",
            date            = Utility.formatDate(item.createdAt, "dd MMM yyyy"),
            amount          = Utility.formatAmount(item.amount),
            isCredit        = item.direction.equals(Constant.WALLET_DIRECTION_CREDIT, ignoreCase = true),
            transactionId   = "",
            categoryIconRes = sourceToIcon(item.description ?: item.type),
            detail          = WalletHistoryDetailPassItem(
                source          = statementSource(item.type, item.clientReference),
                clientReference = item.clientReference,
                txnReference    = item.txnReference,
                description     = item.description,
                amount          = item.amount,
                balanceAfter    = item.balanceAfter,
                createdAt       = item.createdAt
            )
        )

        // GET wallet/withdrawals row — title is "Withdrawal to <bank>", built by the caller (needs a string resource)
        fun from(item: WalletWithdrawalItem, title: String): WalletTransactionItem {
            val status = item.status?.uppercase().orEmpty()
            val isFailed = status in Constant.WITHDRAW_FAILED_STATUSES
            return WalletTransactionItem(
                title           = title,
                date            = Utility.formatDate(item.createdAt, "dd MMM yyyy"),
                amount          = Utility.formatAmount(item.amount),
                isCredit        = false,
                transactionId   = "",
                categoryIconRes = R.drawable.img_load_wallet,
                statusLabelRes  = when {
                    isFailed -> R.string.labelFailed
                    status in Constant.WITHDRAW_SUCCESS_STATUSES -> null
                    else -> R.string.labelPending
                },
                isFailed        = isFailed,
                detail          = WalletHistoryDetailPassItem(
                    source          = if (item.clientReference.isNullOrBlank()) Source.ROW else Source.WITHDRAWAL,
                    clientReference = item.clientReference,
                    txnReference    = item.clientReference,
                    description     = title,
                    amount          = item.amount,
                    balanceAfter    = null,
                    createdAt       = item.createdAt
                )
            )
        }

        // GET rewards/bonus or rewards/referral row — no status call, always row-only detail
        fun from(item: BonusRewardItem): WalletTransactionItem {
            val title = item.details?.takeIf { it.isNotBlank() } ?: kindLabel(item.kind)
            return WalletTransactionItem(
                title           = title,
                date            = Utility.formatDate(item.createdAt, "dd MMM yyyy"),
                amount          = Utility.formatAmount(item.absAmount?.toString()),
                isCredit        = item.isCredit,
                transactionId   = "",
                categoryIconRes = R.drawable.img_load_wallet,
                detail          = WalletHistoryDetailPassItem(
                    source          = Source.ROW,
                    clientReference = item.clientReference,
                    txnReference    = item.txnReference,
                    description     = title,
                    amount          = item.absAmount?.toString(),
                    balanceAfter    = item.walletAfter?.toString(),
                    createdAt       = item.createdAt
                )
            )
        }

        private fun statementSource(type: String?, clientReference: String?): Source {
            if (clientReference.isNullOrBlank()) return Source.ROW
            return when (type?.lowercase()) {
                in BILLPAY_TYPES, TYPE_CASHBACK -> Source.BILLPAY
                in TOPUP_TYPES                  -> Source.TOPUP
                in WITHDRAWAL_TYPES             -> Source.WITHDRAWAL
                else                            -> Source.ROW
            }
        }

        // signup_bonus → "Signup bonus"
        private fun kindLabel(kind: String?): String =
            kind?.replace('_', ' ')?.replaceFirstChar { it.uppercaseChar() } ?: "--"

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
