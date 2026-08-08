package com.shreefintech.paytouchconsumer.retrofit.model.fastag

import com.google.gson.annotations.SerializedName

data class FastagPaymentItem(
    @field:SerializedName("success")        val success: Boolean?,
    @field:SerializedName("message")        val message: String?,
    @field:SerializedName("req_id")         val reqId: String?,
    @field:SerializedName("status")         val status: String?,
    @field:SerializedName("transaction_id") val transactionId: String?
)
