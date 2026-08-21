package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycDocumentItem(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("section") val section: String?,
    @field:SerializedName("document_type") val documentType: String?,
    @field:SerializedName("person_index") val personIndex: Int?,
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("digio_status") val digioStatus: String?
)
