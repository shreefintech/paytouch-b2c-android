package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// One row of GET rewards/bonus or rewards/referral (paginated list in `data`; bonus balance comes from wallet/balance)
// Keys from the backend's RewardEntryResource — `details` is the row title (plain string)
data class BonusRewardItem(
    @field:SerializedName("id")               val id: Int?,
    @field:SerializedName("kind")             val kind: String?,
    @field:SerializedName("amount")           val amount: Double?,
    @field:SerializedName("wallet_after")     val walletAfter: Double?,
    @field:SerializedName("txn_reference")    val txnReference: String?,
    @field:SerializedName("client_reference") val clientReference: String?,
    @field:SerializedName("details")          val details: String?,
    @field:SerializedName("created_at")       val createdAt: String?
) {
    companion object {
        // Kinds that take from the wallet; credit, signup_bonus, cashback, lucky_draw, refund add to it
        private val DEBIT_KINDS = setOf("reversal", "cashback_reversal", "spend")
    }

    // Signed row shape read by WalletTransactionItem.from(BonusRewardItem)
    fun toHistoryItem() = BonusWalletHistoryItem(
        type        = if (kind?.lowercase() in DEBIT_KINDS) "debit" else "credit",
        amount      = amount?.let { kotlin.math.abs(it) },
        reason      = kind,
        description = details?.takeIf { it.isNotBlank() } ?: kind,
        createdAt   = createdAt
    )
}
