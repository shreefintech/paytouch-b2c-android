package com.shreefintech.paytouchconsumer.retrofit.model.gas

import com.google.gson.annotations.SerializedName

data class GasTransactionStatusRequest(
    @field:SerializedName("transaction_id") val transactionId: String?
)
