package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Body of POST wallet/earning/lock and wallet/earning/withdraw — minimum 100; optional request_id is not sent
data class WalletEarningAmountRequest(
    @field:SerializedName("amount") val amount: Double,
    @field:SerializedName("mpin")   val mpin: String
)
