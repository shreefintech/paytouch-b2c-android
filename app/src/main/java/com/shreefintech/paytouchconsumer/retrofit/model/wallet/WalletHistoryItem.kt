package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

data class WalletHistoryItem(
    @field:SerializedName("transaction_id") val transactionId: String?,
    @field:SerializedName("balance_before") val balanceBefore: String?,
    @field:SerializedName("amount") val amount: String?,
    @field:SerializedName("balance_after") val balanceAfter: String?,
    @field:SerializedName("type") val type: String?,
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("service_name") val serviceName: String?,
    @field:SerializedName("created_at") val createdAt: String?,
    @field:SerializedName("source") val source: String?
)
