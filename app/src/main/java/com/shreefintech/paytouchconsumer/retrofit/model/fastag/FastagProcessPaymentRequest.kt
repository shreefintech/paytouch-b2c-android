package com.shreefintech.paytouchconsumer.retrofit.model.fastag

import com.google.gson.annotations.SerializedName

data class FastagProcessPaymentRequest(
    @field:SerializedName("vehicle_number") val vehicleNumber: String,
    @field:SerializedName("operator")       val operator: String,
    @field:SerializedName("operator_name")  val operatorName: String,
    @field:SerializedName("circle")         val circle: String,
    @field:SerializedName("amount")         val amount: Double,
    @field:SerializedName("platform_fee")   val platformFee: Double,
    @field:SerializedName("total_payable")  val totalPayable: Double
)
