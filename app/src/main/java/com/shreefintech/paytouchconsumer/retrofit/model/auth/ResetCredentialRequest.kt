package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

data class ResetCredentialRequest(
    @field:SerializedName("reset_token") val resetToken: String,
    @field:SerializedName("type")        val type: String,
    @field:SerializedName("credential")  val credential: String
)
