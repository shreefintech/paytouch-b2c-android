package com.shreefintech.paytouchconsumer.retrofit.model.fastag

import com.google.gson.annotations.SerializedName
import com.shreefintech.paytouchconsumer.retrofit.model.BillPaymentDataItem

data class FastagPaymentItem(
    @field:SerializedName("success")        val success: Boolean?,
    @field:SerializedName("message")        val message: String?,
    @field:SerializedName("payment_status") val paymentStatus: String?,
    @field:SerializedName("data")           val data: BillPaymentDataItem?
)
