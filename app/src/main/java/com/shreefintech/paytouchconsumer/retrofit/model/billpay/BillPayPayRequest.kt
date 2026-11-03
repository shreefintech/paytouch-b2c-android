package com.shreefintech.paytouchconsumer.retrofit.model.billpay

import com.google.gson.annotations.SerializedName

// request_id is an idempotency key (new per attempt, reused on retry of the same attempt) — not a transaction id
data class BillPayPayRequest(
    @field:SerializedName("biller_id")    val billerId: String,
    @field:SerializedName("input_params") val inputParams: Map<String, String>,
    @field:SerializedName("amount")       val amount: String,
    @field:SerializedName("mpin")         val mpin: String,
    @field:SerializedName("request_id")   val requestId: String
)
