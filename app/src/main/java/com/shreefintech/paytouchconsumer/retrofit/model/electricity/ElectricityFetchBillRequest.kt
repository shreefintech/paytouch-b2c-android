package com.shreefintech.paytouchconsumer.retrofit.model.electricity

import com.google.gson.annotations.SerializedName

data class ElectricityFetchBillRequest(
    @field:SerializedName("connection_number") val connectionNumber: String,
    @field:SerializedName("operator_id")       val operatorId: String,
    @field:SerializedName("circle_id")         val circleId: String
)
