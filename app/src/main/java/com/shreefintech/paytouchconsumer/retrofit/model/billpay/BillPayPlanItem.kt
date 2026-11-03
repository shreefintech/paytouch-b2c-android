package com.shreefintech.paytouchconsumer.retrofit.model.billpay

import com.google.gson.annotations.SerializedName

// Plan objects are provider-defined and passed through — only these keys are read
data class BillPayPlanItem(
    @field:SerializedName("plan_type")   val planType: String? = null,
    @field:SerializedName("amount")      val amount: Double? = null,
    @field:SerializedName("validity")    val validity: String? = null,
    @field:SerializedName("data")        val data: String? = null,
    @field:SerializedName("description") val description: String? = null
)
