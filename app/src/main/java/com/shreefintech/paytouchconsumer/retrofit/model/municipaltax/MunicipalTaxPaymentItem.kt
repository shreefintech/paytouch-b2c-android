package com.shreefintech.paytouchconsumer.retrofit.model.municipaltax

import com.google.gson.annotations.SerializedName
import com.shreefintech.paytouchconsumer.retrofit.model.BillPaymentDataItem

data class MunicipalTaxPaymentItem(
    @field:SerializedName("success")        val success: Boolean?,
    @field:SerializedName("message")        val message: String?,
    @field:SerializedName("payment_status") val paymentStatus: String?,
    @field:SerializedName("data")           val data: BillPaymentDataItem?
)
