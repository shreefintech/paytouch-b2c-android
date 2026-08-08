package com.shreefintech.paytouchconsumer.retrofit.model.dth

import com.google.gson.annotations.SerializedName

data class DthProcessPaymentRequest(
    @field:SerializedName("cn")     val cn: String,
    @field:SerializedName("op")     val op: String,
    @field:SerializedName("amount") val amount: String
)
