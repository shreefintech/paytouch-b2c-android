package com.shreefintech.paytouchconsumer.retrofit.model.billpay

import com.google.gson.annotations.SerializedName

data class BillPayFetchBillRequest(
    @field:SerializedName("biller_id")    val billerId: String,
    @field:SerializedName("input_params") val inputParams: Map<String, String>
)
