package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

data class ForgotCredentialRequest(
    @field:SerializedName("mobile") val mobile: String,
    @field:SerializedName("type")   val type: String
)
