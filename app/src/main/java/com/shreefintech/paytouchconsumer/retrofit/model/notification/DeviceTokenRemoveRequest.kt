package com.shreefintech.paytouchconsumer.retrofit.model.notification

import com.google.gson.annotations.SerializedName

data class DeviceTokenRemoveRequest(
    @field:SerializedName("fcm_token") val fcmToken: String
)
