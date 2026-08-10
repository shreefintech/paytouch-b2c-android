package com.shreefintech.paytouchconsumer.retrofit.model.municipaltax

import com.google.gson.annotations.SerializedName

data class MunicipalTaxLatestPaymentDataItem(
    @field:SerializedName("id")             val id:            Int?,
    @field:SerializedName("customer_name")  val customerName:  String?,
    @field:SerializedName("house_number")   val houseNumber:   String?,
    @field:SerializedName("subscriber_no")  val subscriberNo:  String?,
    @field:SerializedName("bill_amount")    val billAmount:    String?,
    @field:SerializedName("platform_fee")   val platformFee:   String?,
    @field:SerializedName("total_payable")  val totalPayable:  String?,
    @field:SerializedName("transaction_id") val transactionId: String?,
    @field:SerializedName("status")         val status:        String?,
    @field:SerializedName("created_at")     val createdAt:     String?,
    @field:SerializedName("operator_id")    val operatorId:    String?,
    @field:SerializedName("operator_name")  val operatorName:  String?,
    @field:SerializedName("service")        val service:       String?,
    @field:SerializedName("amount")         val amount:        String?,
    @field:SerializedName("due_date")       val dueDate:       String?,
    @field:SerializedName("updated_at")     val updatedAt:     String?,
    @field:SerializedName("subservice")       val subService:       String?,
)
