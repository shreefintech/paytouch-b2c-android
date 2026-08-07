package com.shreefintech.paytouchconsumer.retrofit.model.hdfc

import com.google.gson.annotations.SerializedName

data class HdfcPaymentLinksItem(
    @field:SerializedName("web") val web: String?,
    @field:SerializedName("mobile") val mobile: String?,
    @field:SerializedName("expiry") val expiry: String?
)

data class HdfcOrderItem(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("order_id") val orderId: String?,
    @field:SerializedName("amount") val amount: String?,
    @field:SerializedName("currency") val currency: String?,
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("payment_links") val paymentLinks: HdfcPaymentLinksItem?,
    @field:SerializedName("return_url") val returnUrl: String?,
    @field:SerializedName("wallet_credited_at") val walletCreditedAt: String?
)

data class HdfcOrderResponseItem(
    @field:SerializedName("success") val success: Boolean?,
    @field:SerializedName("message") val message: String?,
    @field:SerializedName("data") val data: HdfcOrderItem?
)
