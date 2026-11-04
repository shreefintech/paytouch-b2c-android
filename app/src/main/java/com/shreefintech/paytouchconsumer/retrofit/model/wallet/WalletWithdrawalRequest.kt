package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Body of POST wallet/withdrawals — amount 100 to 200000; request_id is the idempotency key, reused on retry
// TODO(B2C-298): G28 — transfer_mode (IMPS/NEFT/RTGS) + remark (sheet's narration, null = omitted) sent ahead of
//  backend support
data class WalletWithdrawalRequest(
    @field:SerializedName("amount")        val amount: Double,
    @field:SerializedName("mpin")          val mpin: String,
    @field:SerializedName("request_id")    val requestId: String,
    @field:SerializedName("transfer_mode") val transferMode: String,
    @field:SerializedName("remark")        val remark: String?
)
