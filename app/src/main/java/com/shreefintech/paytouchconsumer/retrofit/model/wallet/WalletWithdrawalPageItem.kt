package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Response of GET wallet/withdrawals — rows are the POST wallet/withdrawals record; Laravel page meta
// TODO(B2C-298): no real response captured yet (doc assumes the paginator shape) — verify data[] and meta
data class WalletWithdrawalPageItem(
    @field:SerializedName("success") val success: Boolean?,
    @field:SerializedName("message") val message: String?,
    @field:SerializedName("data")    val data: List<WalletWithdrawalItem>?,
    @field:SerializedName("meta")    val meta: WalletWithdrawalPageMetaItem?
)

data class WalletWithdrawalPageMetaItem(
    @field:SerializedName("current_page") val currentPage: Int?,
    @field:SerializedName("last_page")    val lastPage: Int?
)
