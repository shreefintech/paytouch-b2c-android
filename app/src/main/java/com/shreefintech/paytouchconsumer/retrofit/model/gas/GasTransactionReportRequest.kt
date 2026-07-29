package com.shreefintech.paytouchconsumer.retrofit.model.gas

import com.google.gson.annotations.SerializedName

data class GasTransactionReportRequest(
    @field:SerializedName("from_date")   val fromDate: String?,
    @field:SerializedName("to_date")     val toDate: String?,
    @field:SerializedName("status")      val status: String?,
    @field:SerializedName("consumer_no") val consumerNo: String?
)
