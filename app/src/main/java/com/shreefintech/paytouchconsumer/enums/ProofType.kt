package com.shreefintech.paytouchconsumer.enums

import androidx.annotation.StringRes
import com.shreefintech.paytouchconsumer.R

enum class ProofType(val apiValue: String, @StringRes val displayNameRes: Int) {
    CANCELLED_CHEQUE("cancelled_cheque", R.string.labelProofCancelledCheque),
    BANK_STATEMENT("bank_statement", R.string.labelProofBankStatement)
}
