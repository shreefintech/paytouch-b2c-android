package com.shreefintech.paytouchconsumer.retrofit.model.gas

import com.google.gson.annotations.SerializedName

data class GasPaymentItem(
    @field:SerializedName("success")        val success: Boolean?,
    @field:SerializedName("message")        val message: String?,
    @field:SerializedName("req_id")         val reqId: String?,
    @field:SerializedName("amount")         val amount: Double?,
    @field:SerializedName("platform_fee")   val platformFee: Double?,
    @field:SerializedName("total_payable")  val totalPayable: Double?,
    @field:SerializedName("status")         val status: String?,
    @field:SerializedName("transaction_id") val transactionId: String?
)
