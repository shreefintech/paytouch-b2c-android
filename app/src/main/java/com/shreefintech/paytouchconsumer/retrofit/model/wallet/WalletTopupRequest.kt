package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Body of POST wallet/topups
data class WalletTopupRequest(
    @field:SerializedName("amount") val amount: Double,
    @field:SerializedName("flow")   val flow: String
)
