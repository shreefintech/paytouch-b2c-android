package com.shreefintech.paytouchconsumer.retrofit.model.postpaid

import com.google.gson.annotations.SerializedName

data class PostpaidTransactionReportRequest(
    @field:SerializedName("from_date")         val fromDate:         String?,
    @field:SerializedName("to_date")           val toDate:           String?,
    @field:SerializedName("status")            val status:           String?,
    @field:SerializedName("connection_number") val connectionNumber: String?,
    @field:SerializedName("page")              val page:             Int?,
    @field:SerializedName("per_page")          val perPage:          Int?
)
