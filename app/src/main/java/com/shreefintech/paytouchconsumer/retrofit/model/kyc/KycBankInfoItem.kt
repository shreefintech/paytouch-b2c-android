package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycBankInfoItem(
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("can_edit") val canEdit: Boolean?,
    @field:SerializedName("accounts") val accounts: List<KycBankAccountDetailItem>?
)
