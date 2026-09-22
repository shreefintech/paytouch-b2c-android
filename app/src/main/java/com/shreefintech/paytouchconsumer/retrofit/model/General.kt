package com.shreefintech.paytouchconsumer.retrofit.model

import com.google.gson.annotations.SerializedName

data class General<T>(
    @field:SerializedName("data")    val data: T?       = null,
    @field:SerializedName("success") val success: Boolean? = null,
    @field:SerializedName("meta")    val meta: Any?     = null,
    @field:SerializedName("message") val message: String?  = null
)
