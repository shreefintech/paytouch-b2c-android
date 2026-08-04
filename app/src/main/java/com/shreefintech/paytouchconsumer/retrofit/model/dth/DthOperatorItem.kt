package com.shreefintech.paytouchconsumer.retrofit.model.dth

import com.google.gson.annotations.SerializedName

data class DthOperatorItem(
    @field:SerializedName("id")   val id: String?,
    @field:SerializedName("name") val name: String?
)
