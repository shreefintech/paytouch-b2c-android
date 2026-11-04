package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Response of GET wallet/statement — rows in `data`, cursor paging in meta.next_cursor (null = last page).
// Flat (not General<T>) because `meta` is typed here.
data class WalletStatementItem(
    @field:SerializedName("success") val success: Boolean?,
    @field:SerializedName("message") val message: String?,
    @field:SerializedName("data")    val data: List<WalletStatementEntryItem>?,
    @field:SerializedName("meta")    val meta: WalletStatementMetaItem?
)

data class WalletStatementMetaItem(
    @field:SerializedName("next_cursor") val nextCursor: String?
)

// One ledger row; `type` picks the detail call (billpay / topup / withdraw ...) — see the Wallet and KYC API doc
data class WalletStatementEntryItem(
    @field:SerializedName("id")               val id: Long?,
    @field:SerializedName("wallet")           val wallet: String?,
    @field:SerializedName("direction")        val direction: String?,
    @field:SerializedName("amount")           val amount: String?,
    @field:SerializedName("balance_after")    val balanceAfter: String?,
    @field:SerializedName("type")             val type: String?,
    @field:SerializedName("client_reference") val clientReference: String?,
    @field:SerializedName("txn_reference")    val txnReference: String?,
    @field:SerializedName("description")      val description: String?,
    @field:SerializedName("created_at")       val createdAt: String?
)
