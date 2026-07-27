package com.shreefintech.paytouchconsumer.retrofit.model.electricity

import com.google.gson.annotations.SerializedName

data class ElectricityTransactionReportDataItem(
    @field:SerializedName("id")             val id: Int?,
    @field:SerializedName("subscriber_no")  val subscriberNo: String?,
    @field:SerializedName("operator_id")    val operatorId: String?,
    @field:SerializedName("subservice")     val subservice: String?,
    @field:SerializedName("customer_name")  val customerName: String?,
    @field:SerializedName("amount")         val amount: Double?,
    @field:SerializedName("platform_fee")   val platformFee: Double?,
    @field:SerializedName("total_payable")  val totalPayable: Double?,
    @field:SerializedName("transaction_id") val transactionId: String?,
    @field:SerializedName("status")         val status: String?,
    @field:SerializedName("created_at")     val createdAt: String?,
    @field:SerializedName("ccf")            val ccf: String?
)
