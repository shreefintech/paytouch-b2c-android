package com.shreefintech.paytouchconsumer.retrofit.model.prepaid

import com.google.gson.annotations.SerializedName

data class PrepaidProcessDirectRequest(
    @field:SerializedName("mobile_no")     val mobileNo: String,
    @field:SerializedName("operator_code") val operatorCode: String,
    @field:SerializedName("circle_code")   val circleCode: String,
    @field:SerializedName("amount")        val amount: Double,
    @field:SerializedName("platform_fee")  val platformFee: Double,
    @field:SerializedName("total_payable") val totalPayable: Double,
    @field:SerializedName("transaction_id") val transactionId: String
)
