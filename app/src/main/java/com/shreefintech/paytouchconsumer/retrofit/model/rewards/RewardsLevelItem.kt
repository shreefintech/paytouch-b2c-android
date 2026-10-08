package com.shreefintech.paytouchconsumer.retrofit.model.rewards

import com.google.gson.annotations.SerializedName

data class RewardsLevelItem(
    @field:SerializedName("level")            val level: String?,
    @field:SerializedName("label")            val label: String?,
    @field:SerializedName("stage")            val stage: String?,
    @field:SerializedName("cashback_percent") val cashbackPercent: Double?,
    @field:SerializedName("cashback_active")  val cashbackActive: Boolean?,
    @field:SerializedName("next")             val next: RewardsLevelNextItem?,
    @field:SerializedName("platinum_days")    val platinumDays: Int?,
    @field:SerializedName("reached_at")       val reachedAt: String?,
    @field:SerializedName("history")          val history: List<RewardsHistoryItem?>?
)

data class RewardsHistoryItem(
    @field:SerializedName("level") val level: String?
)

data class RewardsLevelNextItem(
    @field:SerializedName("level")     val level: String?,
    @field:SerializedName("label")     val label: String?,
    @field:SerializedName("metric")    val metric: String?,
    @field:SerializedName("current")   val current: Double?,
    @field:SerializedName("target")    val target: Double?,
    @field:SerializedName("remaining") val remaining: Double?
)
