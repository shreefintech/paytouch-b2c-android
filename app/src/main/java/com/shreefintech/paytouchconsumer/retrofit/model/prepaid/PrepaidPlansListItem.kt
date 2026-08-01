package com.shreefintech.paytouchconsumer.retrofit.model.prepaid

import com.google.gson.annotations.SerializedName

data class PrepaidPlansListItem(
    @field:SerializedName("success")     val success: Boolean?,
    @field:SerializedName("plans")       val plans: List<PrepaidPlanItem>?,
    @field:SerializedName("operator_id") val operatorId: String?,
    @field:SerializedName("circle_id")   val circleId: String?
)
