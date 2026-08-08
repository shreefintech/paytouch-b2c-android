package com.shreefintech.paytouchconsumer.retrofit.model.loan

import com.google.gson.annotations.SerializedName

data class LoanLatestPaymentDataItem(
    @field:SerializedName("id")                val id: Int?,
    @field:SerializedName("customer_name")     val customerName: String?,
    @field:SerializedName("connection_number") val connectionNumber: String?,
    @field:SerializedName("bill_amount")       val billAmount: String?,
    @field:SerializedName("platform_fee")      val platformFee: String?,
    @field:SerializedName("total_payable")     val totalPayable: String?,
    @field:SerializedName("transaction_id")    val transactionId: String?,
    @field:SerializedName("status")            val status: String?,
    @field:SerializedName("created_at")        val createdAt: String?,
    @field:SerializedName("operator_id")       val operatorId: String?,
    @field:SerializedName("operator_name")     val operatorName: String?,
    @field:SerializedName("amount")            val amount: String?,
    @field:SerializedName("user_id")           val userId: Int?,
    @field:SerializedName("updated_at")        val updatedAt: String?
)
