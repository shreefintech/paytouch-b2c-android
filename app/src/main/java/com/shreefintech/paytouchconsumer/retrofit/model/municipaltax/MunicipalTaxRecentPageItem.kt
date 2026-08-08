package com.shreefintech.paytouchconsumer.retrofit.model.municipaltax

import com.google.gson.annotations.SerializedName

data class MunicipalTaxRecentPageItem(
    @field:SerializedName("success")      val success:      Boolean?,
    @field:SerializedName("transactions") val transactions: List<MunicipalTaxRecentDataItem>?,
    @field:SerializedName("pagination")   val pagination:   MunicipalTaxPaginationItem?
)

data class MunicipalTaxPaginationItem(
    @field:SerializedName("current_page") val currentPage: Int?,
    @field:SerializedName("last_page")    val lastPage:    Int?,
    @field:SerializedName("per_page")     val perPage:     Int?,
    @field:SerializedName("total")        val total:       Int?
)
