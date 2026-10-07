package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

data class LogoutRequest(
    @field:SerializedName("fcm_token") val fcmToken: String?
)
