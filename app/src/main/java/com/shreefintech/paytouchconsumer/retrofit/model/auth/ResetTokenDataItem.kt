package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

data class ResetTokenDataItem(
    @field:SerializedName("reset_token") val resetToken: String?
)
