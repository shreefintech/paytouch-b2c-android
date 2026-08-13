package com.shreefintech.paytouchconsumer.enums

enum class ProofType(val apiValue: String, val displayName: String) {
    CANCELLED_CHEQUE("cancelled_cheque", "Cancelled Cheque"),
    BANK_STATEMENT("bank_statement", "Bank Statement")
}
