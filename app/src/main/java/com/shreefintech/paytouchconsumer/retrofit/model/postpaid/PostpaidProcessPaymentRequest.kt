package com.shreefintech.paytouchconsumer.retrofit.model.postpaid

import com.google.gson.annotations.SerializedName

data class PostpaidProcessPaymentRequest(
    @field:SerializedName("mobile_number")  val mobileNumber: String,
    @field:SerializedName("operator_id")    val operatorId: String,
    @field:SerializedName("circle_id")      val circleId: String,
    @field:SerializedName("amount")         val amount: Double,
    @field:SerializedName("platform_fee")   val platformFee: Double,
    @field:SerializedName("total_payable")  val totalPayable: Double
)
