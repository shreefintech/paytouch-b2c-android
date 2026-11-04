package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName
import com.shreefintech.paytouchconsumer.Constant

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
    // Read by the wallet history detail (GET wallet/topups/{client_reference})
    @field:SerializedName("payment_method_display_name") val paymentMethodDisplayName: String?,
    @field:SerializedName("created_at")       val createdAt: String?,
    // TODO(B2C-298): G3 — not documented; when absent the WebView closes on the backend's JSON return page
    @field:SerializedName("return_url")       val returnUrl: String?
) {
    // Status for PaymentStatusActivity: an open top-up is always pending; once final, success set → SUCCESS,
    // on_hold → pending, anything else → FAILED (shown with failure_reason)
    fun resultStatus(): String {
        val raw = status?.uppercase().orEmpty()
        return when {
            isFinal != true                        -> Constant.TOPUP_STATUS_PENDING
            raw in Constant.TOPUP_SUCCESS_STATUSES -> Constant.TOPUP_STATUS_SUCCESS
            raw == Constant.TOPUP_STATUS_ON_HOLD   -> Constant.TOPUP_STATUS_ON_HOLD
            else                                   -> Constant.TOPUP_STATUS_FAILED
        }
    }
}
