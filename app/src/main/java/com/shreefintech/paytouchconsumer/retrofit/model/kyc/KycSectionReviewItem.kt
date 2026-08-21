package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycSectionReviewItem(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("section") val section: String?,
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("rejection_note") val rejectionNote: String?,
    @field:SerializedName("reviewed_at") val reviewedAt: String?,
    @field:SerializedName("upload_document_count") val uploadDocumentCount: Int?,
    @field:SerializedName("total_document_count") val totalDocumentCount: Int?
)
