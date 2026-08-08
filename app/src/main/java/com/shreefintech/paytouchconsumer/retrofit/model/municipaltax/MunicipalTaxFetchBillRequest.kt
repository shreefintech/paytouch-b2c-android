package com.shreefintech.paytouchconsumer.retrofit.model.municipaltax

import com.google.gson.annotations.SerializedName

data class MunicipalTaxFetchBillRequest(
    @field:SerializedName("house_number") val houseNumber: String,
    @field:SerializedName("cir")          val cir:         String,
    @field:SerializedName("op")           val op:          String
)
