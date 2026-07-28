package com.shreefintech.paytouchconsumer.retrofit.model.gas

import com.google.gson.annotations.SerializedName

data class GasBillItem(
    @field:SerializedName("customer_name") val customerName: String?,
    @field:SerializedName("bill_amount")   val billAmount: String?,
    @field:SerializedName("due_date")      val dueDate: String?,
    @field:SerializedName("bill_date")     val billDate: String?,
    @field:SerializedName("cell_number")   val cellNumber: String?
)
