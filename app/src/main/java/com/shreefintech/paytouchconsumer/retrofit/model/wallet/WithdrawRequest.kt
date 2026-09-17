package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

data class WithdrawRequest(
    @field:SerializedName("amount")       val amount: Double,
    @field:SerializedName("payment_mode") val paymentMode: String?,
    @field:SerializedName("narration")    val narration: String?
)
