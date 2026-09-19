package com.shreefintech.paytouchconsumer.retrofit.model

import com.google.gson.annotations.SerializedName

data class StateItem(
    @field:SerializedName("id")   val id: String?,
    @field:SerializedName("name") val name: String?
)
