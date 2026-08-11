package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycStatusItem(
    @field:SerializedName("success") val success: Boolean?,
    @field:SerializedName("registration_status") val registrationStatus: String?,
    @field:SerializedName("admin_remark") val adminRemark: String?,
    @field:SerializedName("submission") val submission: KycSubmissionStatusItem?,
    @field:SerializedName("virtual_account") val virtualAccount: KycVirtualAccountItem?
)
