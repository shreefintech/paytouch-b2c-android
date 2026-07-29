package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

data class MessageItem(
    @field:SerializedName("message") val message: String?,
    @field:SerializedName("success") val success: Boolean?
)
