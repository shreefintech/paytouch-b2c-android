package com.shreefintech.paytouchconsumer.retrofit.model.prepaid

import com.google.gson.annotations.SerializedName

data class PrepaidTransactionReportRequest(
    @field:SerializedName("from_date")  val fromDate:  String?,
    @field:SerializedName("to_date")    val toDate:    String?,
    @field:SerializedName("status")     val status:    String?,
    @field:SerializedName("mobile_no")  val mobileNo:  String?,
    @field:SerializedName("page")       val page:      Int?,
    @field:SerializedName("per_page")   val perPage:   Int?
)
