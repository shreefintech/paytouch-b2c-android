package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

data class WalletHistoryPageItem(
    @field:SerializedName("current_page") val currentPage: Int?,
    @field:SerializedName("data") val data: List<WalletHistoryItem>?,
    @field:SerializedName("last_page") val lastPage: Int?,
    @field:SerializedName("total") val total: Int?,
    @field:SerializedName("per_page") val perPage: Int?,
    @field:SerializedName("from") val from: Int?,
    @field:SerializedName("to") val to: Int?,
    @field:SerializedName("next_page_url") val nextPageUrl: String?,
    @field:SerializedName("prev_page_url") val prevPageUrl: String?
)
