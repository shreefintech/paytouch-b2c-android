package com.shreefintech.paytouchconsumer.retrofit.model.myaccount

import com.google.gson.annotations.SerializedName

// Response of GET rewards/referral — paginated history in `data` (not shown yet), totals in meta.summary
data class ReferralRewardsItem(
    @field:SerializedName("success") val success: Boolean?,
    @field:SerializedName("message") val message: String?,
    @field:SerializedName("meta")    val meta: ReferralRewardsMetaItem?
)

data class ReferralRewardsMetaItem(
    @field:SerializedName("summary") val summary: ReferralDataItem?
)
