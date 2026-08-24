package com.shreefintech.paytouchconsumer.retrofit.model.gas

import com.google.gson.annotations.SerializedName

data class GasBillItem(
    @field:SerializedName("user_name")       val customerName: String?,
    @field:SerializedName("bill_net_amount") val billAmount: String?,
    @field:SerializedName("due_date")        val dueDate: String?,
    @field:SerializedName("bill_date")       val billDate: String?,
    @field:SerializedName("connection_number") val cellNumber: String?
)
