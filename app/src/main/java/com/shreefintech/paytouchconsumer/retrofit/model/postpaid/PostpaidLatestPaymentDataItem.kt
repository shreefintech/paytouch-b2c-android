package com.shreefintech.paytouchconsumer.retrofit.model.postpaid

import com.google.gson.annotations.SerializedName

data class PostpaidLatestPaymentDataItem(
    @field:SerializedName("id")                val id: Int?,
    @field:SerializedName("customer_name")     val customerName: String?,
    @field:SerializedName("connection_number") val connectionNumber: String?,
    @field:SerializedName("subscriber_no")     val subscriberNo: String?,
    @field:SerializedName("bill_amount")       val billAmount: String?,
    @field:SerializedName("platform_fee")      val platformFee: String?,
    @field:SerializedName("total_payable")     val totalPayable: String?,
    @field:SerializedName("transaction_id")    val transactionId: String?,
    @field:SerializedName("status")            val status: String?,
    @field:SerializedName("created_at")        val createdAt: String?,
    @field:SerializedName("operator_id")       val operatorId: String?,
    @field:SerializedName("operator_name")     val operatorName: String?,
    @field:SerializedName("subservice")        val subservice: String?,
    @field:SerializedName("service")           val service: String?,
    @field:SerializedName("provider")          val provider: String?,
    @field:SerializedName("circle_id")         val circleId: String?,
    @field:SerializedName("user_id")           val userId: Int?,
    @field:SerializedName("amount")            val amount: String?,
    @field:SerializedName("due_date")          val dueDate: String?,
    @field:SerializedName("bill_period")       val billPeriod: String?,
    @field:SerializedName("updated_at")        val updatedAt: String?
)
