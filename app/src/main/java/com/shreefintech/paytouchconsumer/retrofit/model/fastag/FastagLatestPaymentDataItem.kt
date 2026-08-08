package com.shreefintech.paytouchconsumer.retrofit.model.fastag

import com.google.gson.annotations.SerializedName

data class FastagLatestPaymentDataItem(
    @field:SerializedName("id")             val id: Int?,
    @field:SerializedName("vehicle_number") val vehicleNumber: String?,
    @field:SerializedName("operator")       val operator: String?,
    @field:SerializedName("operator_name")  val operatorName: String?,
    @field:SerializedName("amount")         val amount: String?,
    @field:SerializedName("platform_fee")   val platformFee: String?,
    @field:SerializedName("total_payable")  val totalPayable: String?,
    @field:SerializedName("transaction_id") val transactionId: String?,
    @field:SerializedName("status")         val status: String?,
    @field:SerializedName("created_at")     val createdAt: String?
)
