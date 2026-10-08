package com.shreefintech.paytouchconsumer.retrofit.model.rewards

import com.google.gson.annotations.SerializedName

data class ReferralWalletItem(
    @field:SerializedName("referral_wallet")  val referralWallet: String?,
    @field:SerializedName("level")            val level: RewardsLevelItem?,
    @field:SerializedName("referral_code")    val referralCode: String?,
    @field:SerializedName("referral_link")    val referralLink: String?,
    @field:SerializedName("referral_count")   val referralCount: Int?,
    @field:SerializedName("total_earnings")   val totalEarnings: String?
)
