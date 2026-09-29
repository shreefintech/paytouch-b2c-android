package com.shreefintech.paytouchconsumer.retrofit.model.notification

import com.google.gson.annotations.SerializedName

data class DeviceTokenRequest(
    @field:SerializedName("fcm_token") val fcmToken: String,
    @field:SerializedName("platform") val platform: String,
    @field:SerializedName("device_id") val deviceId: String
)
