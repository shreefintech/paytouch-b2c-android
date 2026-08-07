package com.shreefintech.paytouchconsumer.retrofit.model.postpaid

import com.google.gson.annotations.SerializedName

data class PostpaidTransactionReportDataItem(
    @field:SerializedName("id")                val id:               Int?,
    @field:SerializedName("connection_number") val connectionNumber: String?,
    @field:SerializedName("operator_id")       val operatorId:       String?,
    @field:SerializedName("customer_name")     val customerName:     String?,
    @field:SerializedName("bill_amount")       val billAmount:       String?,
    @field:SerializedName("platform_fee")      val platformFee:      String?,
    @field:SerializedName("total_payable")     val totalPayable:     String?,
    @field:SerializedName("status")            val status:           String?,
    @field:SerializedName("transaction_id")    val transactionId:    String?,
    @field:SerializedName("service_type")      val serviceType:      String?,
    @field:SerializedName("created_at")        val createdAt:        String?
)
