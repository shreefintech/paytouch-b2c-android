package com.shreefintech.paytouchconsumer.retrofit.model.rewards

import com.google.gson.annotations.SerializedName

data class EarningWalletActionDataItem(
    @field:SerializedName("earning") val earning: EarningWalletActionEarningItem?,
    @field:SerializedName("main")    val main: Double?
)

data class EarningWalletActionEarningItem(
    @field:SerializedName("opted_in")                  val optedIn: Boolean?,
    @field:SerializedName("principal")                 val principal: Double?,
    @field:SerializedName("pending_withdrawal_amount") val pendingWithdrawalAmount: Double?,
    @field:SerializedName("withdrawal_available_at")   val withdrawalAvailableAt: String?
)
