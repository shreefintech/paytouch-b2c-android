package com.shreefintech.paytouchconsumer.retrofit.model

import com.google.gson.annotations.SerializedName

data class General<T>(
    @field:SerializedName("data")    val data: T?,
    @field:SerializedName("success") val success: Boolean?,
    @field:SerializedName("meta")    val meta: Any?,
    @field:SerializedName("message") val message: String?
)
