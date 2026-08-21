package com.shreefintech.paytouchconsumer.enums

import androidx.annotation.StringRes
import com.shreefintech.paytouchconsumer.R

enum class StatementPeriod(val apiValue: String, @StringRes val displayNameRes: Int) {
    THREE_MONTHS("three_months", R.string.labelStatementPeriod3Months),
    SIX_MONTHS("six_months", R.string.labelStatementPeriod6Months)
}
