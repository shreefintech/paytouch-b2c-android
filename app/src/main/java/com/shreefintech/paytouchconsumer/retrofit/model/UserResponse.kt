package com.shreefintech.paytouchconsumer.retrofit.model

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @field:SerializedName("id")                       val id: Int?,
    @field:SerializedName("mobile")                   val mobile: String?,
    @field:SerializedName("email")                    val email: String?,
    @field:SerializedName("wallet_balance")           val walletBalance: String?,
    @field:SerializedName("requires_kyc")             val requiresKyc: Boolean?,
    @field:SerializedName("requires_mpin")            val requiresMpin: Boolean?,
    @field:SerializedName("requires_virtual_account") val requiresVirtualAccount: Boolean?
)
