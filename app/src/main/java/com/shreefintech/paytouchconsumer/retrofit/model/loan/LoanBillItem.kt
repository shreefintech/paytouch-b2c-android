package com.shreefintech.paytouchconsumer.retrofit.model.loan

import com.google.gson.annotations.SerializedName

data class LoanBillItem(
    @field:SerializedName("billAmount")       val billAmount: String?,
    @field:SerializedName("billnetamount")    val billNetAmount: String?,
    @field:SerializedName("acceptPayment")    val acceptPayment: Boolean?,
    @field:SerializedName("acceptPartPay")    val acceptPartPay: Boolean?,
    @field:SerializedName("cellNumber")       val cellNumber: String?,
    @field:SerializedName("userName")         val userName: String?,
    @field:SerializedName("additionalDetails") val additionalDetails: LoanBillAdditionalDetailsItem?
)

data class LoanBillAdditionalDetailsItem(
    @field:SerializedName("Agreement number") val agreementNumber: String?,
    @field:SerializedName("billNumber")       val billNumber: String?,
    @field:SerializedName("Customer Name")    val customerName: String?
)
