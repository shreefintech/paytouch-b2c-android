package com.shreefintech.paytouchconsumer.retrofit.model.fastag

import com.google.gson.annotations.SerializedName

data class FastagOperatorItem(
    @field:SerializedName("id")        val id: String?,
    @field:SerializedName("name")      val name: String?,
    @field:SerializedName("biller_id") val billerId: String?,
    @field:SerializedName("circle_id") val circleId: String?
)
