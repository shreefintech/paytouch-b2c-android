package com.shreefintech.paytouchconsumer.retrofit.model.electricity

import com.google.gson.annotations.SerializedName

data class ElectricityBillItem(
    @field:SerializedName("customer_name") val customerName: String?,
    @field:SerializedName("bill_amount")   val billAmount: String?,
    @field:SerializedName("due_date")      val dueDate: String?,
    @field:SerializedName("bill_date")     val billDate: String?
)
