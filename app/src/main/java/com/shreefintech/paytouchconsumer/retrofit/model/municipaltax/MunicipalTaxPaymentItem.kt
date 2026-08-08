package com.shreefintech.paytouchconsumer.retrofit.model.municipaltax

import com.google.gson.annotations.SerializedName

data class MunicipalTaxPaymentItem(
    @field:SerializedName("success") val success: Boolean?,
    @field:SerializedName("message") val message: String?,
    @field:SerializedName("req_id")  val reqId:   String?,
    @field:SerializedName("status")  val status:  String?
)
