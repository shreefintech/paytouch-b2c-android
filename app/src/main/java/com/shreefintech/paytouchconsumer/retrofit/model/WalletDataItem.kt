package com.shreefintech.paytouchconsumer.retrofit.model

import com.google.gson.annotations.SerializedName

data class WalletDataItem(
    @field:SerializedName("walletBalance") val walletBalance: String?
)
