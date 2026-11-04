package com.shreefintech.paytouchconsumer.retrofit.model.billpay

import com.google.gson.annotations.SerializedName

data class BillPayCategoryItem(
    @field:SerializedName("category") val category: String? = null,
    @field:SerializedName("label")    val label: String? = null
)
