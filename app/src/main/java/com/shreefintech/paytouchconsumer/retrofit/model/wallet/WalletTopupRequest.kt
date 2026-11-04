package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Body of POST wallet/topups — remark is the sheet's description (null = omitted, Gson skips nulls)
data class WalletTopupRequest(
    @field:SerializedName("amount") val amount: Double,
    @field:SerializedName("flow")   val flow: String,
    @field:SerializedName("remark") val remark: String?
)
