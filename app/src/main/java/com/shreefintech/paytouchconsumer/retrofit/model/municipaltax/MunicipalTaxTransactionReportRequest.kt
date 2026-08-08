package com.shreefintech.paytouchconsumer.retrofit.model.municipaltax

import com.google.gson.annotations.SerializedName

data class MunicipalTaxTransactionReportRequest(
    @field:SerializedName("from_date")      val fromDate:     String?,
    @field:SerializedName("to_date")        val toDate:       String?,
    @field:SerializedName("status")         val status:       String?,
    @field:SerializedName("subscriber_no")  val subscriberNo: String?,
    @field:SerializedName("page")           val page:         Int?,
    @field:SerializedName("per_page")       val perPage:      Int?
)
