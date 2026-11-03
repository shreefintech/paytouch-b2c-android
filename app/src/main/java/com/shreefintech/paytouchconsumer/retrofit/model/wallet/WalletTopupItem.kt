package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Response of POST wallet/topups and GET wallet/topups/{client_reference}
// Fields from the Wallet and KYC API doc sample (2026-11-03); only the fields the app reads are mapped
data class WalletTopupItem(
    @field:SerializedName("client_reference") val clientReference: String?,
    @field:SerializedName("amount")           val amount: String?,
    @field:SerializedName("fee")              val fee: String?,
    @field:SerializedName("credit_amount")    val creditAmount: String?,
    @field:SerializedName("status")           val status: String?,
    @field:SerializedName("is_final")         val isFinal: Boolean?,
    @field:SerializedName("payment_link")     val paymentLink: String?,
    @field:SerializedName("upi_intent_url")   val upiIntentUrl: String?,
    @field:SerializedName("failure_reason")   val failureReason: String?,
    // TODO(B2C-298): G3 — not documented; when absent the payment WebView closes only on back
    @field:SerializedName("return_url")       val returnUrl: String?
)
