package com.shreefintech.paytouchconsumer.retrofit.model.municipaltax

import com.google.gson.annotations.SerializedName

data class MunicipalTaxRecentDataItem(
    @field:SerializedName("id")             val id:            Int?,
    @field:SerializedName("operator_name")  val operatorName:  String?,
    @field:SerializedName("customer_name")  val customerName:  String?,
    @field:SerializedName("house_number")   val houseNumber:   String?,
    @field:SerializedName("amount")         val amount:        String?,
    @field:SerializedName("platform_fee")   val platformFee:   String?,
    @field:SerializedName("total_payable")  val totalPayable:  String?,
    @field:SerializedName("transaction_id") val transactionId: String?,
    @field:SerializedName("status")         val status:        String?,
    @field:SerializedName("created_at")     val createdAt:     String?
)
