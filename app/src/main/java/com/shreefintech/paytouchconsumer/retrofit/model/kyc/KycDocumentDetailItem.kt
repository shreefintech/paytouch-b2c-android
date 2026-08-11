package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycDocumentDetailItem(
    @field:SerializedName("document_type") val documentType: String?,
    @field:SerializedName("label") val label: String?,
    @field:SerializedName("file_url") val fileUrl: String?,
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("rejection_note") val rejectionNote: String?
)
