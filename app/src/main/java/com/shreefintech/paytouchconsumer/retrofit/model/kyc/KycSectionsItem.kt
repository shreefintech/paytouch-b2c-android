package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycSectionsItem(
    @field:SerializedName("a") val a: String?,
    @field:SerializedName("b") val b: String?,
    @field:SerializedName("c") val c: String?
)
