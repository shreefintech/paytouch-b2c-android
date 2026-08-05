package com.shreefintech.paytouchconsumer.retrofit.model.prepaid

import com.google.gson.annotations.SerializedName

data class PrepaidPlanItem(
    @field:SerializedName("plan_type")   val planType: Int?,
    @field:SerializedName("amount")      val amount: Int?,
    @field:SerializedName("description") val description: String?,
    @field:SerializedName("validity")    val validity: String?,
    @field:SerializedName("talktime")    val talktime: Double?,
    @field:SerializedName("data")        val data: String?
)
