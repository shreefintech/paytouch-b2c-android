package com.shreefintech.paytouchconsumer.retrofit.model.billpay

import com.google.gson.annotations.SerializedName

/**
 * One bill / recharge payment — returned by POST billpay/pay, GET billpay/status/{client_reference}
 * and each row of GET transactions. Not read yet: payment_breakdown, level, tds.
 */
data class BillPayTransactionItem(
    @field:SerializedName("client_reference") val clientReference: String? = null,
    @field:SerializedName("request_id")       val requestId: String? = null,
    @field:SerializedName("biller_id")        val billerId: Int? = null,
    @field:SerializedName("biller_name")      val billerName: String? = null,
    @field:SerializedName("category")         val category: String? = null,
    @field:SerializedName("input_params")     val inputParams: Map<String, String>? = null,
    @field:SerializedName("identifier_label") val identifierLabel: String? = null,
    @field:SerializedName("amount")           val amount: Double? = null,
    @field:SerializedName("status")           val status: String? = null,
    @field:SerializedName("is_final")         val isFinal: Boolean? = null,
    @field:SerializedName("txn_reference")    val txnReference: String? = null,
    @field:SerializedName("provider_ref")     val providerRef: String? = null,
    @field:SerializedName("failure_reason")   val failureReason: String? = null,
    @field:SerializedName("charge")           val charge: Double? = null,
    @field:SerializedName("gst_amount")       val gstAmount: Double? = null,
    @field:SerializedName("total_amount")     val totalAmount: Double? = null,
    @field:SerializedName("created_at")       val createdAt: String? = null,
    @field:SerializedName("updated_at")       val updatedAt: String? = null
)
