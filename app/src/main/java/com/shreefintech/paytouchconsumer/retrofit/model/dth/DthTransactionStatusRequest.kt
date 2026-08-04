package com.shreefintech.paytouchconsumer.retrofit.model.dth

import com.google.gson.annotations.SerializedName

data class DthTransactionStatusRequest(
    @field:SerializedName("subscriber_no")  val subscriberNo:  String?,
    @field:SerializedName("transaction_id") val transactionId: String?,
    @field:SerializedName("page")           val page:          Int?,
    @field:SerializedName("per_page")       val perPage:       Int?
)
