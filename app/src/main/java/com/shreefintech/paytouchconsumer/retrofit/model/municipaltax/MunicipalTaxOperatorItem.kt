package com.shreefintech.paytouchconsumer.retrofit.model.municipaltax

import com.google.gson.annotations.SerializedName
data class MunicipalTaxOperatorItem(
    @field:SerializedName("id")   val id:   String?,
    @field:SerializedName("name") val name: String?,
    @field:SerializedName("circle_id") val circleId: String?
)
