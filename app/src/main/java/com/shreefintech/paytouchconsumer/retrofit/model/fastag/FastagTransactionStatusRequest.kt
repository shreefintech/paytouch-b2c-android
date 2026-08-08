package com.shreefintech.paytouchconsumer.retrofit.model.fastag

import com.google.gson.annotations.SerializedName

data class FastagTransactionStatusRequest(
    @field:SerializedName("vehicle_number") val vehicleNumber: String?,
    @field:SerializedName("transaction_id") val transactionId: String?,
    @field:SerializedName("page")           val page:          Int?,
    @field:SerializedName("per_page")       val perPage:       Int?
)
