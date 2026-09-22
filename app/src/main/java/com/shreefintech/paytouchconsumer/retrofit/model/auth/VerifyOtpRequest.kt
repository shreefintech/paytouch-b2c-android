package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

data class VerifyOtpRequest(
    @field:SerializedName("mobile") val mobile: String,
    @field:SerializedName("otp")    val otp: String
)
