package com.shreefintech.paytouchconsumer.retrofit.model

import com.google.gson.annotations.SerializedName

data class BillPaymentDataItem(
    @field:SerializedName("req_id")         val reqId: String?,
    @field:SerializedName("transaction_id") val transactionId: String?,
    @field:SerializedName("amount")         val amount: Double?,
    @field:SerializedName("platform_fee")   val platformFee: Double?,
    @field:SerializedName("total_payable")  val totalPayable: Double?,
    @field:SerializedName("status")         val status: String?,
    @field:SerializedName("mobile_no")      val mobileNo: String?
)
