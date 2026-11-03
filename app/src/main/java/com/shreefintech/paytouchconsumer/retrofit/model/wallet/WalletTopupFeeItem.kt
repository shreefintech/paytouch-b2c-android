package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Response of GET wallet/topups/fee-preview — no rule matched: fee 0 and credit_amount = amount;
// rules_synced false: Dashboard's rules haven't arrived, so a 0 fee is not reliable
data class WalletTopupFeeItem(
    @field:SerializedName("amount")        val amount: String?,
    @field:SerializedName("fee")           val fee: String?,
    @field:SerializedName("credit_amount") val creditAmount: String?,
    @field:SerializedName("fee_percent")   val feePercent: String?,
    @field:SerializedName("fee_flat")      val feeFlat: String?,
    @field:SerializedName("fee_rule_id")   val feeRuleId: Int?,
    @field:SerializedName("description")   val description: String?,
    @field:SerializedName("rules_synced")  val rulesSynced: Boolean?
)
