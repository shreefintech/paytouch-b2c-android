package com.shreefintech.paytouchconsumer.retrofit.model.billpay

import com.google.gson.annotations.SerializedName

/**
 * GET billpay/utility-billers/{id}/plans — provider plan objects (camelCase keys, passed through by the backend).
 * Only these keys are read; the rest (id, planName, planTab, planDetailItemList, planBenefitItemList, …) is ignored.
 */
data class BillPayPlanItem(
    @field:SerializedName("amount")          val amount: Double? = null,
    @field:SerializedName("validity")        val validity: String? = null,
    @field:SerializedName("dataBenefit")     val data: String? = null,
    @field:SerializedName("planDescription") val description: String? = null
)
