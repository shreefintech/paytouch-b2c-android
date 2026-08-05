package com.shreefintech.paytouchconsumer.retrofit.model.gas

import com.google.gson.annotations.SerializedName

data class GasProcessPaymentRequest(
    @field:SerializedName("connection_number") val connectionNumber: String,
    @field:SerializedName("operator_id")     val operatorId: String,
    @field:SerializedName("circle_id")       val circleId: String,
    @field:SerializedName("amount")          val amount: Double,
    @field:SerializedName("platform_fee")    val platformFee: Double,
    @field:SerializedName("total_payable")   val totalPayable: Double,
    @field:SerializedName("transaction_id")  val transactionId: String
)
