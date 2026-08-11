package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycAgreeDataItem(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("entity_type") val entityType: String?,
    @field:SerializedName("agreed_at") val agreedAt: String?
)
