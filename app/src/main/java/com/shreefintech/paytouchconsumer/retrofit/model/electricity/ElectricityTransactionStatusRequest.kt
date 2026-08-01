package com.shreefintech.paytouchconsumer.retrofit.model.electricity

import com.google.gson.annotations.SerializedName

data class ElectricityTransactionStatusRequest(
    @field:SerializedName("transaction_id") val transactionId: String?,
    @field:SerializedName("page")           val page: Int?,
    @field:SerializedName("per_page")       val perPage: Int?
)
