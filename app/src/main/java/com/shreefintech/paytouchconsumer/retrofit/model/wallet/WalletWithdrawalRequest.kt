package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Body of POST wallet/withdrawals — optional request_id is not sent (TODO(B2C-298): G17)
data class WalletWithdrawalRequest(
    @field:SerializedName("amount") val amount: Double,
    @field:SerializedName("mpin")   val mpin: String
)
