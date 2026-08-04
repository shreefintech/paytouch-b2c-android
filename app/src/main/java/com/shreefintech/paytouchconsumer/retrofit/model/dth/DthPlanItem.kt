package com.shreefintech.paytouchconsumer.retrofit.model.dth

import com.google.gson.annotations.SerializedName

data class DthPlanItem(
    @field:SerializedName("plan_type")   val planType: Int?,
    @field:SerializedName("amount")      val amount: Int?,
    @field:SerializedName("description") val description: String?,
    @field:SerializedName("validity")    val validity: String?,
    @field:SerializedName("talktime")    val talktime: Double?,
    @field:SerializedName("data")        val data: String?,
    @field:SerializedName("id")          val id: String?,
    @field:SerializedName("category")    val category: String?
)
