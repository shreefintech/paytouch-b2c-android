package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycVirtualAccountItem(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("user_id") val userId: Int?,
    @field:SerializedName("bank_acc") val bankAcc: String?,
    @field:SerializedName("vavpa") val vavpa: String?,
    @field:SerializedName("dashboard_kyc_status") val dashboardKycStatus: String?
)
