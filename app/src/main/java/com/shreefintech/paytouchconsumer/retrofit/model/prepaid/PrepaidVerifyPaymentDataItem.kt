package com.shreefintech.paytouchconsumer.retrofit.model.prepaid

import com.google.gson.annotations.SerializedName

data class PrepaidVerifyPaymentDataItem(
    @field:SerializedName("id")             val id: Int?,
    @field:SerializedName("mobile_no")      val mobileNo: String?,
    @field:SerializedName("operator")       val operator: String?,
    @field:SerializedName("circle")         val circle: String?,
    @field:SerializedName("amount")         val amount: String?,
    @field:SerializedName("txn_id")         val txnId: String?,
    @field:SerializedName("status")         val status: String?,
    @field:SerializedName("created_at")     val createdAt: String?,
    @field:SerializedName("updated_at")     val updatedAt: String?,
    @field:SerializedName("subscriber_no")  val subscriberNo: String?,
    @field:SerializedName("total_payable")  val totalPayable: String?,
    @field:SerializedName("platform_fee")   val platformFee: String?,
    @field:SerializedName("transaction_id") val transactionId: String?,
    @field:SerializedName("subservice")     val subservice: String?,
    @field:SerializedName("service")        val service: String?
)
