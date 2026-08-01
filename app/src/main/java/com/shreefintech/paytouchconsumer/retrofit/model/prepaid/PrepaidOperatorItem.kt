package com.shreefintech.paytouchconsumer.retrofit.model.prepaid

import com.google.gson.annotations.SerializedName

data class PrepaidOperatorItem(
    @field:SerializedName("id")   val id: String?,
    @field:SerializedName("name") val name: String?,
    @field:SerializedName("code") val code: String?
)
