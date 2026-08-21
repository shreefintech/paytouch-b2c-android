package com.shreefintech.paytouchconsumer.retrofit.model

import com.google.gson.annotations.SerializedName

data class WalletDataItem(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("name") val name: String?,
    @field:SerializedName("email") val email: String?,
    @field:SerializedName("wallet_balance") val walletBalance: String?,
    @field:SerializedName("live_bank_balance") val liveBankBalance: Double?,
    @field:SerializedName("mobile") val mobile: String?,
    @field:SerializedName("virtual_account_number") val virtualAccountNumber: String?,
    @field:SerializedName("vpa") val vpa: String?,
    @field:SerializedName("ifsc") val ifsc: String?,
    @field:SerializedName("has_virtual_account") val hasVirtualAccount: Boolean?,
    @field:SerializedName("can_create_virtual_account") val canCreateVirtualAccount: Boolean?,
    @field:SerializedName("wallet") val wallet: WalletInfoItem?
)

data class WalletInfoItem(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("wallet_id") val walletId: String?,
    @field:SerializedName("balance") val balance: String?,
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("is_kyc_verified") val isKycVerified: Boolean?,
    @field:SerializedName("daily_limit") val dailyLimit: String?,
    @field:SerializedName("monthly_limit") val monthlyLimit: String?,
    @field:SerializedName("mobikwik_wallet_id") val mobikwikWalletId: String?
)
