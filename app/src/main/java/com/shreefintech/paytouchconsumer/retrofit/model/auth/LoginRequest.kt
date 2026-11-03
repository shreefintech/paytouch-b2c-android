package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @field:SerializedName("mobile")     val mobile: String,
    @field:SerializedName("credential") val credential: String,
    @field:SerializedName("type")       val type: String
)
