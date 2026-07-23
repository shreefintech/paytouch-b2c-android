package com.shreefintech.paytouchconsumer.retrofit.model

import com.google.gson.annotations.SerializedName

data class LoginItem(
    @field:SerializedName("token")      val token: String?,
    @field:SerializedName("token_type") val tokenType: String?,
    @field:SerializedName("user")       val user: UserItem?
)

data class UserItem(
    @field:SerializedName("id")                       val id: Int?,
    @field:SerializedName("name")                     val name: String?,
    @field:SerializedName("mobile")                   val mobile: String?,
    @field:SerializedName("email")                    val email: String?,
    @field:SerializedName("wallet_balance")           val walletBalance: String?,
    @field:SerializedName("requires_kyc")             val requiresKyc: Boolean?,
    @field:SerializedName("requires_mpin")            val requiresMpin: Boolean?,
    @field:SerializedName("requires_virtual_account") val requiresVirtualAccount: Boolean?
)
