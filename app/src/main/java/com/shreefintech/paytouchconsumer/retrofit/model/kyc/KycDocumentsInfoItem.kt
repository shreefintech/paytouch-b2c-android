package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycDocumentsInfoItem(
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("items") val items: List<KycDocumentDetailItem>?
)
