package com.shreefintech.paytouchconsumer.retrofit.model.gas

import com.google.gson.annotations.SerializedName

data class GasFetchBillRequest(
    @field:SerializedName("connection_number") val consumerNumber: String,
    @field:SerializedName("operator_id")     val operatorId: String,
    @field:SerializedName("circle_id")       val circleId: String
)
