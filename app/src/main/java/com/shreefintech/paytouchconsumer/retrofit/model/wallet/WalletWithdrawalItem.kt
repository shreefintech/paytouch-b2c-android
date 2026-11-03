package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Response of POST wallet/withdrawals and GET wallet/withdrawals/{client_reference}
data class WalletWithdrawalItem(
    @field:SerializedName("client_reference") val clientReference: String?,
    @field:SerializedName("amount")           val amount: String?,
    @field:SerializedName("fee")              val fee: String?,
    @field:SerializedName("total_debited")    val totalDebited: String?,
    @field:SerializedName("status")           val status: String?,
    @field:SerializedName("is_final")         val isFinal: Boolean?,
    @field:SerializedName("bank")             val bank: WalletWithdrawalBankItem?,
    @field:SerializedName("utr")              val utr: String?,
    @field:SerializedName("failure_reason")   val failureReason: String?,
    @field:SerializedName("created_at")       val createdAt: String?,
    @field:SerializedName("completed_at")     val completedAt: String?
)

data class WalletWithdrawalBankItem(
    @field:SerializedName("bank_name")      val bankName: String?,
    @field:SerializedName("account_masked") val accountMasked: String?,
    @field:SerializedName("ifsc")           val ifsc: String?
)
