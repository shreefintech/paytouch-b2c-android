package com.shreefintech.paytouchconsumer.retrofit.model.municipaltax

import com.google.gson.annotations.SerializedName

data class MunicipalTaxProcessPaymentRequest(
    @field:SerializedName("house_number")  val houseNumber:  String,
    @field:SerializedName("op")            val op:           String,
    @field:SerializedName("cir")           val cir:          String,
    @field:SerializedName("amount")        val amount:       Double,
    @field:SerializedName("customer_name") val customerName: String,
    @field:SerializedName("due_date")      val dueDate:      String,
    @field:SerializedName("op_name")       val opName:       String
)
