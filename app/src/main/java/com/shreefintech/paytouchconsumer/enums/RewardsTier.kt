package com.shreefintech.paytouchconsumer.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.shreefintech.paytouchconsumer.R

enum class RewardsTier(
    @StringRes val labelRes: Int,
    @DrawableRes val badgeRes: Int
) {
    BRONZE(R.string.labelBronze, R.drawable.ic_shield_bronze),
    SILVER(R.string.labelSilver, R.drawable.ic_shield_silver),
    GOLD(R.string.labelGold, R.drawable.ic_shield_gold),
    PLATINUM(R.string.labelPlatinum, R.drawable.ic_shield_platinum);

    companion object {
        fun from(apiValue: String?): RewardsTier = when (apiValue?.lowercase()) {
            "silver"   -> SILVER
            "gold"     -> GOLD
            "platinum" -> PLATINUM
            else       -> BRONZE
        }
    }
}
