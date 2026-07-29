package com.shreefintech.paytouchconsumer.retrofit.model.electricity

import com.google.gson.annotations.SerializedName

data class UnifiedTransactionItem(
    @field:SerializedName("id")               val id: Int?,
    @field:SerializedName("type")             val type: String?,
    @field:SerializedName("reference_id")     val referenceId: String?,
    @field:SerializedName("identifier")       val identifier: String?,
    @field:SerializedName("identifier_label") val identifierLabel: String?,
    @field:SerializedName("amount")           val amount: String?,
    @field:SerializedName("platform_fee")     val platformFee: String?,
    @field:SerializedName("total_payable")    val totalPayable: String?,
    @field:SerializedName("status")           val status: String?,
    @field:SerializedName("created_at")       val createdAt: String?,
    @field:SerializedName("updated_at")       val updatedAt: String?,
    @field:SerializedName("extra")            val extra: UnifiedTransactionExtraItem?
)
