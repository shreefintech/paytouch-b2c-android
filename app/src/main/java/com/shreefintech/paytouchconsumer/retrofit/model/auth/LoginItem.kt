package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

data class LoginItem(
    @field:SerializedName("token")                    val token: String?,
    @field:SerializedName("token_type")               val tokenType: String?,
    @field:SerializedName("user")                     val user: UserItem?,
    @field:SerializedName("requires_kyc")             val requiresKyc: Boolean?,
    @field:SerializedName("requires_mpin")            val requiresMpin: Boolean?,
    @field:SerializedName("requires_virtual_account") val requiresVirtualAccount: Boolean?
)

data class UserItem(
    @field:SerializedName("id")                val id: Int?,
    @field:SerializedName("mobile")            val mobile: String?,
    @field:SerializedName("email")             val email: String?,
    @field:SerializedName("wallet_balance")    val walletBalance: String?,
    @field:SerializedName("total_spent")       val totalSpent: String?,
    @field:SerializedName("email_verified_at") val emailVerifiedAt: String?,
    @field:SerializedName("kyc_completed")     val kycCompleted: Boolean?,
    @field:SerializedName("referral_code")     val referralCode: String?,
    @field:SerializedName("referred_by")       val referredBy: String?,
    @field:SerializedName("referral_earnings") val referralEarnings: String?,
    @field:SerializedName("created_at")        val createdAt: String?,
    @field:SerializedName("updated_at")        val updatedAt: String?
)
