package com.shreefintech.paytouchconsumer.retrofit.model.hdfc

import com.google.gson.annotations.SerializedName

data class HdfcCreateOrderRequest(
    @field:SerializedName("amount")      val amount: Double,
    @field:SerializedName("description") val description: String,
    @field:SerializedName("purpose")     val purpose: String = "wallet_topup"
)
