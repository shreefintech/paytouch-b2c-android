package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

data class WithdrawDataItem(
    @field:SerializedName("id")           val id: Int?,
    @field:SerializedName("amount")       val amount: String?,
    @field:SerializedName("status")       val status: String?,
    @field:SerializedName("payment_mode") val paymentMode: String?,
    @field:SerializedName("narration")    val narration: String?,
    @field:SerializedName("request_id")  val requestId: String?
)
