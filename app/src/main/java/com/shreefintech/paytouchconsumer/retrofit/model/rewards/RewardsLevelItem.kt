package com.shreefintech.paytouchconsumer.retrofit.model.rewards

import com.google.gson.annotations.SerializedName

data class RewardsLevelItem(
    @field:SerializedName("tier") val tier: String?,
    @field:SerializedName("sub_level") val subLevel: Int?,
    @field:SerializedName("cashback_pct") val cashbackPct: Double?,
    @field:SerializedName("next_level_amount") val nextLevelAmount: Double?,
    @field:SerializedName("lifetime_paid") val lifetimePaid: Double?,
    @field:SerializedName("referral_count") val referralCount: Int?,
    @field:SerializedName("progress_pct") val progressPct: Int?,
    @field:SerializedName("promo_text") val promoText: String?
)
