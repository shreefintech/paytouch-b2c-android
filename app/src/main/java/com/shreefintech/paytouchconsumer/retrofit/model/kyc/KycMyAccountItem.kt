package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycMyAccountItem(
    @field:SerializedName("success") val success: Boolean?,
    @field:SerializedName("has_kyc_data") val hasKycData: Boolean?,
    @field:SerializedName("identity") val identity: KycIdentityItem?,
    @field:SerializedName("bank") val bank: KycBankInfoItem?,
    @field:SerializedName("documents") val documents: KycDocumentsInfoItem?
)
