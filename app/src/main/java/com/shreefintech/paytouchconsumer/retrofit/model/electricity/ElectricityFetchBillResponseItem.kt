package com.shreefintech.paytouchconsumer.retrofit.model.electricity

import com.google.gson.annotations.SerializedName

data class ElectricityFetchBillResponseItem(
    @field:SerializedName("success") val success: Boolean?,
    @field:SerializedName("data")    val data: List<ElectricityBillItem>?,
    @field:SerializedName("message") val message: ElectricityFetchBillMessageItem?,
    @field:SerializedName("bill_id") val billId: Int? // needed for processPayment request
)

data class ElectricityFetchBillMessageItem(
    @field:SerializedName("code") val code: String?,
    @field:SerializedName("text") val text: String?
)
