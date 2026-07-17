package com.shreefintech.paytouchconsumer.auth.model

import com.google.gson.annotations.SerializedName

data class CreateAccountItem(
    @field:SerializedName("id")           val id: String?           = null,
    @field:SerializedName("mobile")       val mobile: String?       = null,
    @field:SerializedName("email")        val email: String?        = null,
    @field:SerializedName("referral_code") val referralCode: String? = null,
    @field:SerializedName("created_at")   val createdAt: String?    = null
)
