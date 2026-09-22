package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycDataItem(
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("entity_type") val entityType: String?,
    @field:SerializedName("current_section") val currentSection: String?,
    @field:SerializedName("sections") val sections: KycSectionsItem?,
    @field:SerializedName("required_fields") val requiredFields: List<String?>?,
    @field:SerializedName("optional_fields") val optionalFields: List<String?>?,
    @field:SerializedName("agreed_at") val agreedAt: String?
)
