package com.shreefintech.paytouchconsumer.retrofit.model.prepaid

import com.google.gson.annotations.SerializedName

data class PrepaidTransactionReportDataItem(
    @field:SerializedName("id")         val id: Int?,
    @field:SerializedName("mobile_no")  val mobileNo: String?,
    @field:SerializedName("operator")   val operator: String?,
    @field:SerializedName("amount")     val amount: String?,
    @field:SerializedName("txn_id")     val txnId: String?,
    @field:SerializedName("status")     val status: String?,
    @field:SerializedName("created_at") val createdAt: String?,
    @field:SerializedName("updated_at") val updatedAt: String?,
    @field:SerializedName("service")    val service: String?
)
