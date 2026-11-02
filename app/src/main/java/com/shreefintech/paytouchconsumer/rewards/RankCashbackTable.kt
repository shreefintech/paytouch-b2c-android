package com.shreefintech.paytouchconsumer.rewards

import java.util.Locale

/** Local cashback rate table until the backend adds next.cashback_percent to the API. */
object RankCashbackTable {
    private val MAP = mapOf(
        "bronze_3" to 0.05, "bronze_2" to 0.10, "bronze_1" to 0.15,
        "silver_3" to 0.20, "silver_2" to 0.25, "silver_1" to 0.30,
        "gold_3" to 0.35, "gold_2" to 0.40, "gold_1" to 0.45,
        "platinum" to 0.50
    )

    fun cashbackFor(levelKey: String?): Double? = MAP[levelKey?.lowercase(Locale.ROOT)]
}
