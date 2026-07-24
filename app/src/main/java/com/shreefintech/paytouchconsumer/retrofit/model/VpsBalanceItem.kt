package com.shreefintech.paytouchconsumer.retrofit.model

import com.google.gson.annotations.SerializedName

data class VpsBalanceItem(
    @field:SerializedName("balance") val balance: String?
)
