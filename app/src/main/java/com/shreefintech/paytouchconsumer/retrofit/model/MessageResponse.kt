package com.shreefintech.paytouchconsumer.retrofit.model

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    @field:SerializedName("message") val message: String?,
    @field:SerializedName("success") val success: Boolean?
)
