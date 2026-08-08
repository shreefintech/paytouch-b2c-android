package com.shreefintech.paytouchconsumer.retrofit.model.electricity

import com.google.gson.annotations.SerializedName

data class UnifiedTransactionExtraItem(
    @field:SerializedName("raw_status")     val rawStatus: String?,
    @field:SerializedName("operator_id")   val operatorId: String?,
    @field:SerializedName("operator")      val operator: String?,
    @field:SerializedName("circle_id")     val circleId: String?,
    @field:SerializedName("customer_name") val customerName: String?,
    @field:SerializedName("due_date")      val dueDate: String?,
    @field:SerializedName("bill_period")   val billPeriod: String?,
    @field:SerializedName("operator_name")   val operatorName: String?
)
