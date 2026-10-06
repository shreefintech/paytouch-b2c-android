package com.shreefintech.paytouchconsumer.retrofit.model.rewards

import com.google.gson.annotations.SerializedName

data class EarningWalletItem(
    @field:SerializedName("opted_in")   val optedIn: Boolean?,
    @field:SerializedName("principal")  val principal: Double?
)
