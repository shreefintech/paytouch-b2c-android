package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

data class MpinItem(
    @field:SerializedName("success")  val success: Boolean?,
    @field:SerializedName("message")  val message: String?,
    @field:SerializedName("data")     val data: MpinDataItem?
)

data class MpinDataItem(
    @field:SerializedName("id")         val id: Int?,
    @field:SerializedName("user_id")    val userId: Int?,
    @field:SerializedName("is_active")  val isActive: Boolean?,
    @field:SerializedName("created_at") val createdAt: String?,
    @field:SerializedName("updated_at") val updatedAt: String?
)
