package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

data class WithdrawDataItem(
    @field:SerializedName("id")                    val id: Int?,
    @field:SerializedName("request_id")            val requestId: String?,
    @field:SerializedName("reference_no")          val referenceNo: String?,
    @field:SerializedName("utr")                   val utr: String?,
    @field:SerializedName("amount")                val amount: String?,
    @field:SerializedName("status")                val status: String?,
    @field:SerializedName("status_label")          val statusLabel: String?,
    @field:SerializedName("status_message")        val statusMessage: String?,
    @field:SerializedName("transfer_mode")         val transferMode: String?,
    @field:SerializedName("bank_name")             val bankName: String?,
    @field:SerializedName("bank_branch")           val bankBranch: String?,
    @field:SerializedName("account_holder")        val accountHolder: String?,
    @field:SerializedName("account_number")        val accountNumber: String?,
    @field:SerializedName("account_number_masked") val accountNumberMasked: String?,
    @field:SerializedName("ifsc")                  val ifsc: String?,
    @field:SerializedName("remark")                val remark: String?,
    @field:SerializedName("requested_at")          val requestedAt: String?,
    @field:SerializedName("requested_at_display")  val requestedAtDisplay: String?,
    @field:SerializedName("updated_at")            val updatedAt: String?,
    @field:SerializedName("completed_at")          val completedAt: String?
)
