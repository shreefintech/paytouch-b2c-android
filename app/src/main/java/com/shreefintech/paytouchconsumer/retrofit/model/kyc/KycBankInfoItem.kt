package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycBankInfoItem(
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("accounts") val accounts: List<KycBankAccountDetailItem>?
)
