package com.shreefintech.paytouchconsumer.retrofit.model.rewards

import com.google.gson.annotations.SerializedName

data class EarningWalletAmountRequest(
    @field:SerializedName("amount") val amount: Double
)
