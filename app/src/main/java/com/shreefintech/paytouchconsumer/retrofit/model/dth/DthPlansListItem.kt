package com.shreefintech.paytouchconsumer.retrofit.model.dth

import com.google.gson.annotations.SerializedName

data class DthPlansListItem(
    @field:SerializedName("success")     val success: Boolean?,
    @field:SerializedName("message")     val message: String?,
    @field:SerializedName("plans")       val plans: List<DthPlanItem>?,
    @field:SerializedName("operator_id") val operatorId: String?,
    @field:SerializedName("circle_id")   val circleId: String?
)
