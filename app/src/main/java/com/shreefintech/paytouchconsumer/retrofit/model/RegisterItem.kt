package com.shreefintech.paytouchconsumer.retrofit.model

import com.google.gson.annotations.SerializedName

data class RegisterItem(
    @field:SerializedName("user")       val user: RegisterUserItem?,
    @field:SerializedName("token")      val token: String?,
    @field:SerializedName("token_type") val tokenType: String?
)

data class RegisterUserItem(
    @field:SerializedName("id")            val id: Int?,
    @field:SerializedName("mobile")        val mobile: String?,
    @field:SerializedName("email")         val email: String?,
    @field:SerializedName("referral_code") val referralCode: String?,
    @field:SerializedName("created_at")    val createdAt: String?,
    @field:SerializedName("updated_at")    val updatedAt: String?
)
