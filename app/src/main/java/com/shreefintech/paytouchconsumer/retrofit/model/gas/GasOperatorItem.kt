package com.shreefintech.paytouchconsumer.retrofit.model.gas

import com.google.gson.annotations.SerializedName

data class GasOperatorItem(
    @field:SerializedName("id")   val id: String?,
    @field:SerializedName("name") val name: String?
)
