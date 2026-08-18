package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycSectionsItem(
    @field:SerializedName("a") val a: KycSectionStatusItem?,
    @field:SerializedName("b") val b: KycSectionStatusItem?,
    @field:SerializedName("c") val c: KycSectionStatusItem?
)
