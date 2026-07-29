package com.shreefintech.paytouchconsumer.retrofit.model.electricity

import com.google.gson.annotations.SerializedName

data class ElectricityOperatorItem(
    @field:SerializedName("id")   val id: String?,
    @field:SerializedName("name") val name: String?
)
