package com.shreefintech.paytouchconsumer.retrofit.model.electricity

import com.google.gson.annotations.SerializedName

data class ElectricityTransactionReportRequest(
    @field:SerializedName("from_date")   val fromDate: String?,
    @field:SerializedName("to_date")     val toDate: String?,
    @field:SerializedName("status")      val status: String?,
    @field:SerializedName("consumer_no") val consumerNo: String?
)
