package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// `data` of GET billpay/status/{client_reference} — opened from a wallet history row (billpay, recharge,
// platform_fee, refund, cashback). input_params is not mapped: its shape isn't documented yet
data class BillPayStatusItem(
    @field:SerializedName("client_reference") val clientReference: String?,
    @field:SerializedName("status")           val status: String?,
    @field:SerializedName("amount")           val amount: String?,
    @field:SerializedName("charge")           val charge: String?,
    @field:SerializedName("gst_amount")       val gstAmount: String?,
    @field:SerializedName("total_amount")     val totalAmount: String?,
    @field:SerializedName("biller_name")      val billerName: String?,
    @field:SerializedName("provider_ref")     val providerRef: String?,
    @field:SerializedName("failure_reason")   val failureReason: String?,
    @field:SerializedName("created_at")       val createdAt: String?
)
