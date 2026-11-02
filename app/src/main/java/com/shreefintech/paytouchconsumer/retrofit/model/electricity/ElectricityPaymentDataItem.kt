package com.shreefintech.paytouchconsumer.retrofit.model.electricity

import com.google.gson.annotations.SerializedName

data class ElectricityPaymentDataItem(
    @field:SerializedName("reqid")             val reqId: String?,
    @field:SerializedName("payment_id")        val paymentId: Int?,
    @field:SerializedName("connection_number") val connectionNumber: String?,
    @field:SerializedName("operator_id")       val operatorId: String?,
    @field:SerializedName("circle_id")         val circleId: String?,
    @field:SerializedName("bill_amount")       val billAmount: Double?,
    @field:SerializedName("platform_fee")      val platformFee: Double?,
    @field:SerializedName("total_payable")     val totalPayable: Double?,
    @field:SerializedName("status")            val status: String?,
    @field:SerializedName("message")           val message: String?
)
