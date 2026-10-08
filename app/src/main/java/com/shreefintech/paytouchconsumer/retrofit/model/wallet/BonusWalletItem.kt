package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

data class BonusWalletItem(
    @field:SerializedName("bonus_wallet")        val bonusWallet: String?,
    @field:SerializedName("in_use")              val inUse: String?,
    @field:SerializedName("max_percent_of_bill") val maxPercentOfBill: Int?,
    @field:SerializedName("history")             val history: List<BonusWalletHistoryItem>?
)

data class BonusWalletHistoryItem(
    @field:SerializedName("type")        val type: String?,
    @field:SerializedName("amount")      val amount: Double?,
    @field:SerializedName("reason")      val reason: String?,
    @field:SerializedName("description") val description: String?,
    @field:SerializedName("created_at")  val createdAt: String?
) {
    val isCredit: Boolean get() = type?.equals("credit", ignoreCase = true) == true
    val isReferral: Boolean get() = reason?.startsWith("referral", ignoreCase = true) == true
}
