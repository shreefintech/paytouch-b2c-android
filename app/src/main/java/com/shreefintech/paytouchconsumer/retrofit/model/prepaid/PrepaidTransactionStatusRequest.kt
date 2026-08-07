package com.shreefintech.paytouchconsumer.retrofit.model.prepaid

import com.google.gson.annotations.SerializedName

data class PrepaidTransactionStatusRequest(
    @field:SerializedName("mobile_no") val mobileNo: String?,
    @field:SerializedName("page")      val page:     Int?,
    @field:SerializedName("per_page")  val perPage:  Int?
)
