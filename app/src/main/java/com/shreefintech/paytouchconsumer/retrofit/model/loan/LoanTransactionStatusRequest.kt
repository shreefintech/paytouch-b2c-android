package com.shreefintech.paytouchconsumer.retrofit.model.loan

import com.google.gson.annotations.SerializedName

data class LoanTransactionStatusRequest(
    @field:SerializedName("transaction_id") val transactionId: String?,
    @field:SerializedName("page")           val page:          Int?,
    @field:SerializedName("per_page")       val perPage:       Int?
)
