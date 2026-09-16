package com.shreefintech.paytouchconsumer.retrofit.model.transactions

import com.google.gson.annotations.SerializedName

data class TransactionHistoryDetailItem(
    @field:SerializedName("id")                          val id: Int?,
    @field:SerializedName("type")                        val type: String?,
    @field:SerializedName("status")                      val status: String?,
    @field:SerializedName("amount")                      val amount: String?,
    @field:SerializedName("platform_fee")                val platformFee: String?,
    @field:SerializedName("total_payable")               val totalPayable: String?,
    @field:SerializedName("reference_id")                val referenceId: String?,
    @field:SerializedName("identifier")                  val identifier: String?,
    @field:SerializedName("identifier_label")            val identifierLabel: String?,
    @field:SerializedName("created_at")                  val createdAt: String?,
    // PYTCH bill payment fields
    @field:SerializedName("transaction_id")              val transactionId: String?,
    // HDFC wallet topup fields
    @field:SerializedName("order_id")                    val orderId: String?,
    @field:SerializedName("payment_method_display_name") val paymentMethodDisplayName: String?,
    @field:SerializedName("rrn")                         val rrn: String?
)
