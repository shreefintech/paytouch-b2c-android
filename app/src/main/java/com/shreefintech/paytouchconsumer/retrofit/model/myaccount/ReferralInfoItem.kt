package com.shreefintech.paytouchconsumer.retrofit.model.myaccount

import com.google.gson.annotations.SerializedName

data class ReferralInfoItem(
    @field:SerializedName("success") val success: Boolean?,
    @field:SerializedName("data")    val data: ReferralDataItem?
)
