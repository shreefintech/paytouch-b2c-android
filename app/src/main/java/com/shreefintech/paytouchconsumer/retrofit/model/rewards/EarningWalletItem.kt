package com.shreefintech.paytouchconsumer.retrofit.model.rewards

import com.google.gson.annotations.SerializedName

data class EarningWalletItem(
    @field:SerializedName("opted_in")                  val optedIn: Boolean?,
    @field:SerializedName("principal")                 val principal: Double?,
    @field:SerializedName("interest_accrued")          val interestAccrued: Double?,
    @field:SerializedName("aging_started_at")          val agingStartedAt: String?,
    @field:SerializedName("pending_withdrawal_amount") val pendingWithdrawalAmount: Double?,
    @field:SerializedName("withdrawal_available_at")   val withdrawalAvailableAt: String?
)
