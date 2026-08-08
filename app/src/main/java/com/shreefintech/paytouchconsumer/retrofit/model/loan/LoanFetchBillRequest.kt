package com.shreefintech.paytouchconsumer.retrofit.model.loan

import com.google.gson.annotations.SerializedName

data class LoanFetchBillRequest(
    @field:SerializedName("consumer_number") val consumerNumber: String,
    @field:SerializedName("operator_id")     val operatorId: String,
    @field:SerializedName("circle_id")       val circleId: String
)
