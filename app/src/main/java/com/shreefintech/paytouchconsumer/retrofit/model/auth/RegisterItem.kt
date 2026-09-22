package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @field:SerializedName("name")                  val name: String,
    @field:SerializedName("mobile")                val mobile: String,
    @field:SerializedName("email")                 val email: String,
    @field:SerializedName("password")              val password: String,
    @field:SerializedName("password_confirmation") val passwordConfirmation: String,
    @field:SerializedName("referral_code")         val referralCode: String
)
