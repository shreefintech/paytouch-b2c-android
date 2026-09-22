package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

data class MeDataItem(
    @field:SerializedName("user")      val user: LoginUserItem?,
    @field:SerializedName("next_step") val nextStep: String?
)
