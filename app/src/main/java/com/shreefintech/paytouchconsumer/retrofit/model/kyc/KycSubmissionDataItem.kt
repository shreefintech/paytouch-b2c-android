package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycSubmissionDataItem(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("user_virtual_account_id") val userVirtualAccountId: Int?,
    @field:SerializedName("entity_type") val entityType: String?,
    @field:SerializedName("has_gst") val hasGst: Boolean?,
    @field:SerializedName("has_msme") val hasMsme: Boolean?,
    @field:SerializedName("transaction_limit_tier") val transactionLimitTier: String?,
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("section_reviews") val sectionReviews: List<KycSectionReviewItem>?,
    @field:SerializedName("documents") val documents: List<KycDocumentItem>?
)
