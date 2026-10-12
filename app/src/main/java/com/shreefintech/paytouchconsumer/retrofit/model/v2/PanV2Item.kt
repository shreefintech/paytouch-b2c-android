package com.shreefintech.paytouchconsumer.retrofit.model.v2

import com.google.gson.annotations.SerializedName

data class PanV2Item(
    @field:SerializedName("success") val success: Boolean?,
    @field:SerializedName("message") val message: String?
)

data class PanV2Request(
    @field:SerializedName("pan") val panNumber: String
)
