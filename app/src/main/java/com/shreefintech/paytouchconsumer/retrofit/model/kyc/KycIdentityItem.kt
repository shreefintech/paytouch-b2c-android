package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycIdentityItem(
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("avatar_url") val avatarUrl: String?,
    @field:SerializedName("mobile") val mobile: String?,
    @field:SerializedName("aadhaar_number") val aadhaarNumber: String?,
    @field:SerializedName("pan_number") val panNumber: String?,
    @field:SerializedName("email") val email: String?
)
