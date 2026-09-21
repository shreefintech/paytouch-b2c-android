package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

data class ForgotCredentialVerifyItem(
    @field:SerializedName("reset_token") val resetToken: String?
)
