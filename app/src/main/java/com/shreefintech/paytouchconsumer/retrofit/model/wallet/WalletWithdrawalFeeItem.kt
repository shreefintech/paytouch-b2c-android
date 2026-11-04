package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Response of GET wallet/withdrawals/fee-preview — fee is set in Dashboard admin, 0 until set
data class WalletWithdrawalFeeItem(
    @field:SerializedName("amount")          val amount: String?,
    @field:SerializedName("fee")             val fee: String?,
    @field:SerializedName("total")           val total: String?,
    @field:SerializedName("fee_flat")        val feeFlat: String?,
    @field:SerializedName("fee_percent")     val feePercent: String?,
    @field:SerializedName("settings_synced") val settingsSynced: Boolean?
)
