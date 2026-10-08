package com.shreefintech.paytouchconsumer.enums

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.shreefintech.paytouchconsumer.R

enum class RewardsTier(
    @StringRes val labelRes: Int,
    @DrawableRes val badgeRes: Int,
    @DrawableRes val rankShieldRes: Int,
    @ColorRes val rankTintRes: Int
) {
    BRONZE(R.string.labelBronze, R.drawable.ic_shield_bronze, R.drawable.ic_rank_shield_bronze, R.color.rank_tint_bronze),
    SILVER(R.string.labelSilver, R.drawable.ic_shield_silver, R.drawable.ic_rank_shield_silver, R.color.rank_tint_silver),
    GOLD(R.string.labelGold, R.drawable.ic_shield_gold, R.drawable.ic_rank_shield_gold, R.color.rank_tint_gold),
    PLATINUM(R.string.labelPlatinum, R.drawable.ic_shield_platinum, R.drawable.ic_rank_shield_platinum, R.color.rank_tint_platinum);

    companion object {
        fun from(apiValue: String?): RewardsTier = when (apiValue?.lowercase()) {
            "silver"   -> SILVER
            "gold"     -> GOLD
            "platinum" -> PLATINUM
            else       -> BRONZE
        }
    }
}
