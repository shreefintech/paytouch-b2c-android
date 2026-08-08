package com.shreefintech.paytouchconsumer.retrofit.model.fastag

import com.google.gson.annotations.SerializedName

data class FastagTransactionPageItem(
    @field:SerializedName("current_page")  val currentPage: Int?,
    @field:SerializedName("data")          val data:        List<FastagTransactionReportDataItem>?,
    @field:SerializedName("last_page")     val lastPage:    Int?,
    @field:SerializedName("next_page_url") val nextPageUrl: String?,
    @field:SerializedName("total")         val total:       Int?
)
