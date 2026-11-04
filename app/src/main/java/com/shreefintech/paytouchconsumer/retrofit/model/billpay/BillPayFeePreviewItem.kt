package com.shreefintech.paytouchconsumer.retrofit.model.billpay

import com.google.gson.annotations.SerializedName

// GET billpay/fee-preview — Platform Fee row = fee + gst_amount, Total Payable row = total
data class BillPayFeePreviewItem(
    @field:SerializedName("fee")        val fee: Double? = null,
    @field:SerializedName("gst_amount") val gstAmount: Double? = null,
    @field:SerializedName("total")      val total: Double? = null
)
