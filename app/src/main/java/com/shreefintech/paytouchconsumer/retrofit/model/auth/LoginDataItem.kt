package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

data class LoginDataItem(
    @field:SerializedName("user")               val user: AuthUserItem?,
    @field:SerializedName("token")              val token: String?,
    @field:SerializedName("token_type")         val tokenType: String?,
    @field:SerializedName("next_step")          val nextStep: String?,
    @field:SerializedName("mpin_required")      val mpinRequired: Boolean?,
    @field:SerializedName("bank_details_added") val bankDetailsAdded: Boolean?,
    @field:SerializedName("pan_added")          val panAdded: Boolean?
)

data class AuthUserItem(
    @field:SerializedName("id")                val id: Int?,
    @field:SerializedName("name")              val name: String?,
    @field:SerializedName("mobile")            val mobile: String?,
    @field:SerializedName("email")             val email: String?,
    @field:SerializedName("referral_code")     val referralCode: String?,
    @field:SerializedName("wallet_balance")    val walletBalance: String?,
    // B2C-286 — short-signup users only
    @field:SerializedName("aadhaar_masked")    val aadhaarMasked: String?
)
