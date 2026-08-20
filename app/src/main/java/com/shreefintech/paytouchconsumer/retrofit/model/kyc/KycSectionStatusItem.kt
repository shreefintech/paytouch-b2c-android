package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycSectionStatusItem(
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("rejection_notes") val rejectionNotes: List<String?>?
)
