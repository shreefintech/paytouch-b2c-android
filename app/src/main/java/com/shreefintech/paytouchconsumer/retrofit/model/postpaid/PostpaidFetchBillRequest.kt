package com.shreefintech.paytouchconsumer.retrofit.model.postpaid

import com.google.gson.annotations.SerializedName

data class PostpaidFetchBillRequest(
    @field:SerializedName("mobile_number") val mobileNumber: String,
    @field:SerializedName("operator_id") val operatorId: String,
    @field:SerializedName("circle_id") val circleId: String
)
