package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// One row of GET rewards/bonus or rewards/referral (paginated list in `data`; bonus balance comes from wallet/balance)
data class BonusRewardItem(
    @field:SerializedName("id")               val id: Int?,
    @field:SerializedName("kind")             val kind: String?,
    @field:SerializedName("amount")           val amount: Double?,
    @field:SerializedName("wallet_after")     val walletAfter: Double?,
    @field:SerializedName("txn_reference")    val txnReference: String?,
    @field:SerializedName("client_reference") val clientReference: String?,
    @field:SerializedName("details")          val details: BonusRewardDetailsItem?,
    @field:SerializedName("created_at")       val createdAt: String?
) {
    // Maps onto the row shape ReferralHistoryAdp already renders
    // TODO(B2C-298): referral rows are empty on staging — a negative amount meaning debit is assumed
    fun toHistoryItem() = BonusWalletHistoryItem(
        type        = if ((amount ?: 0.0) < 0) "debit" else "credit",
        amount      = amount?.let { kotlin.math.abs(it) },
        reason      = kind,
        description = details?.note ?: kind,
        createdAt   = createdAt
    )
}

data class BonusRewardDetailsItem(
    @field:SerializedName("note") val note: String?
)
