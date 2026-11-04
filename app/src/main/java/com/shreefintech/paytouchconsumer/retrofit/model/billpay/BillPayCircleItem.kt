package com.shreefintech.paytouchconsumer.retrofit.model.billpay

import com.google.gson.annotations.SerializedName

// code is sent as circle_code (2 digits)
data class BillPayCircleItem(
    @field:SerializedName("code") val code: String? = null,
    @field:SerializedName("name") val name: String? = null
)
