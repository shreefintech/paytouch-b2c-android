package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycSubmissionStatusItem(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("entity_type") val entityType: String?,
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("section_a_submitted_at") val sectionASubmittedAt: String?,
    @field:SerializedName("section_b_submitted_at") val sectionBSubmittedAt: String?,
    @field:SerializedName("section_c_submitted_at") val sectionCSubmittedAt: String?,
    @field:SerializedName("agreed_at") val agreedAt: String?,
    @field:SerializedName("last_polled_at") val lastPolledAt: String?,
    @field:SerializedName("last_error") val lastError: String?
)
