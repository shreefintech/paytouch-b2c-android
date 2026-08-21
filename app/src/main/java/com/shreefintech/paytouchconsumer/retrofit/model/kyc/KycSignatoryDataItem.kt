package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycSignatoryDataItem(
    @field:SerializedName("person_index") val personIndex: Int?,
    @field:SerializedName("email") val email: String?,
    @field:SerializedName("mobile") val mobile: String?,
    @field:SerializedName("pan_number") val panNumber: String?,
    @field:SerializedName("aadhaar_number") val aadhaarNumber: String?,
    @field:SerializedName("is_mandatory") val isMandatory: Boolean?,
    @field:SerializedName("documents") val documents: List<KycDocumentItem>?
)
