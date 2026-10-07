package com.shreefintech.paytouchconsumer.rewards

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.shreefintech.paytouchconsumer.R
import java.text.NumberFormat
import java.util.Locale

/** Visuals per stage. */
enum class RankTier(
    @DrawableRes val shield: Int,
    @ColorRes val tint: Int
) {
    BRONZE(R.drawable.ic_rank_shield_bronze, R.color.rank_tint_bronze),
    SILVER(R.drawable.ic_rank_shield_silver, R.color.rank_tint_silver),
    GOLD(R.drawable.ic_rank_shield_gold, R.color.rank_tint_gold),
    PLATINUM(R.drawable.ic_rank_shield_platinum, R.color.rank_tint_platinum);

    companion object {
        fun from(stage: String?): RankTier = when (stage?.lowercase(Locale.ROOT)) {
            "silver" -> SILVER
            "gold" -> GOLD
            "platinum" -> PLATINUM
            else -> BRONZE
        }
    }
}

object RankFormat {

    /**
     * The API does not send next.cashback_percent yet, so the app keeps this map.
     * Remove it once the backend adds the field.
     */
    private val CASHBACK_BY_LEVEL = mapOf(
        "bronze_3" to 0.05, "bronze_2" to 0.10, "bronze_1" to 0.15,
        "silver_3" to 0.20, "silver_2" to 0.25, "silver_1" to 0.30,
        "gold_3" to 0.35, "gold_2" to 0.40, "gold_1" to 0.45,
        "platinum" to 0.50
    )

    fun cashbackFor(levelKey: String?): Double? = CASHBACK_BY_LEVEL[levelKey?.lowercase(Locale.ROOT)]

    /** 0.2 -> "0.20" (always 2 decimals, like the design). */
    fun percent(value: Double): String = String.format(Locale.US, "%.2f", value)

    private val inr = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    /** 2300.0 -> "2,300" */
    fun rupees(value: Double): String = inr.format(value)

    /** 7.0 -> "7" */
    fun count(value: Double): String = value.toLong().toString()
}
