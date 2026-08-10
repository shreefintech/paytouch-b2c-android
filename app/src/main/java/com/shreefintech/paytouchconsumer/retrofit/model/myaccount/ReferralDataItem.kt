package com.shreefintech.paytouchconsumer.retrofit.model.myaccount

import com.google.gson.annotations.SerializedName

data class ReferralDataItem(
    @field:SerializedName("referral_code")     val referralCode: String?,
    @field:SerializedName("referral_link")     val referralLink: String?,
    @field:SerializedName("total_earnings")    val totalEarnings: String?,
    @field:SerializedName("referral_count")    val referralCount: Int?,
    @field:SerializedName("earning_potential") val earningPotential: String?
)
