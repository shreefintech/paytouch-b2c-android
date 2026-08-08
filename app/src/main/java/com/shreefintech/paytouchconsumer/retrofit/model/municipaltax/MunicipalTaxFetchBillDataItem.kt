package com.shreefintech.paytouchconsumer.retrofit.model.municipaltax

import com.google.gson.annotations.SerializedName

data class MunicipalTaxFetchBillDataItem(
    @field:SerializedName("billAmount")    val billAmount:    String?,
    @field:SerializedName("billnetamount") val billNetAmount: String?,
    @field:SerializedName("billdate")      val billDate:      String?,
    @field:SerializedName("dueDate")       val dueDate:       String?,
    @field:SerializedName("acceptPayment") val acceptPayment: Boolean?,
    @field:SerializedName("acceptPartPay") val acceptPartPay: Boolean?,
    @field:SerializedName("cellNumber")    val cellNumber:    String?,
    @field:SerializedName("userName")      val userName:      String?
)
