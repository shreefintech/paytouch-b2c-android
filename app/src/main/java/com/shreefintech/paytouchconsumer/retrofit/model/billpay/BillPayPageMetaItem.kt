package com.shreefintech.paytouchconsumer.retrofit.model.billpay

import com.google.gson.annotations.SerializedName

data class BillPayPageMetaItem(
    @field:SerializedName("current_page") val currentPage: Int? = null,
    @field:SerializedName("last_page")    val lastPage: Int? = null,
    @field:SerializedName("per_page")     val perPage: Int? = null,
    @field:SerializedName("total")        val total: Int? = null
)
