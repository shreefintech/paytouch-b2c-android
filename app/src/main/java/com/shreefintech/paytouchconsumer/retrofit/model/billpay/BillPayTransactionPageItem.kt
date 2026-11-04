package com.shreefintech.paytouchconsumer.retrofit.model.billpay

import com.google.gson.annotations.SerializedName

// GET transactions — rows in data, paging at the top level (meta)
data class BillPayTransactionPageItem(
    @field:SerializedName("success") val success: Boolean? = null,
    @field:SerializedName("message") val message: String? = null,
    @field:SerializedName("data")    val data: List<BillPayTransactionItem>? = null,
    @field:SerializedName("meta")    val meta: BillPayPageMetaItem? = null
)
