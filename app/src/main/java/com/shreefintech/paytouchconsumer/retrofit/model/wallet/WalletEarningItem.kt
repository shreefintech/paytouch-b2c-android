package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Response of GET wallet/earning — field list from the Wallet and KYC API doc (2026-11-03); amounts arrive as strings
data class WalletEarningItem(
    @field:SerializedName("opted_in")                  val optedIn: Boolean?,
    @field:SerializedName("principal")                 val principal: String?,
    @field:SerializedName("interest_accrued")          val interestAccrued: String?,
    @field:SerializedName("aging_started_at")          val agingStartedAt: String?,
    @field:SerializedName("pending_withdrawal_amount") val pendingWithdrawalAmount: String?,
    @field:SerializedName("withdrawal_available_at")   val withdrawalAvailableAt: String?,
    @field:SerializedName("pan_added")                 val panAdded: Boolean?,
    @field:SerializedName("interest_starts_at")        val interestStartsAt: String?,
    @field:SerializedName("interest_eligible")         val interestEligible: Boolean?
)
