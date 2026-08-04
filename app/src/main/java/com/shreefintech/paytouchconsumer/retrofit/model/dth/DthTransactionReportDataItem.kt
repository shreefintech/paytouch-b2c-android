package com.shreefintech.paytouchconsumer.retrofit.model.dth

import com.google.gson.annotations.SerializedName

data class DthTransactionReportDataItem(
    @field:SerializedName("id")             val id:            Int?,
    @field:SerializedName("mobile_no")      val mobileNo:      String?,
    @field:SerializedName("subscriber_no")  val subscriberNo:  String?,
    @field:SerializedName("operator")       val operator:      String?,
    @field:SerializedName("amount")         val amount:        String?,
    @field:SerializedName("platform_fee")   val platformFee:   String?,
    @field:SerializedName("total_payable")  val totalPayable:  String?,
    @field:SerializedName("transaction_id") val transactionId: String?,
    @field:SerializedName("status")         val status:        String?,
    @field:SerializedName("created_at")     val createdAt:     String?
)
