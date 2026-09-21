package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

data class LoginItem(
    @field:SerializedName("user")       val user: UserItem?,
    @field:SerializedName("token")      val token: String?,
    @field:SerializedName("token_type") val tokenType: String?,
    @field:SerializedName("next_step")  val nextStep: String?
)

data class UserItem(
    @field:SerializedName("id")             val id: Int?,
    @field:SerializedName("name")           val name: String?,
    @field:SerializedName("mobile")         val mobile: String?,
    @field:SerializedName("email")          val email: String?,
    @field:SerializedName("referral_code")  val referralCode: String?
)
