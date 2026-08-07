package com.shreefintech.paytouchconsumer.retrofit.model.myaccount

import com.google.gson.annotations.SerializedName

data class AccountInfoItem(
    @field:SerializedName("success")       val success: Boolean?,
    @field:SerializedName("kyc_completed") val kycCompleted: Boolean?,
    @field:SerializedName("kyc_data")      val kycData: AccountInfoDataItem?
)
