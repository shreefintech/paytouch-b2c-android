package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Body of POST wallet/withdrawals — amount 100 to 200000; request_id is the idempotency key, reused on retry
data class WalletWithdrawalRequest(
    @field:SerializedName("amount")     val amount: Double,
    @field:SerializedName("mpin")       val mpin: String,
    @field:SerializedName("request_id") val requestId: String
)
