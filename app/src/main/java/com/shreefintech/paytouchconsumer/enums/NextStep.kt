package com.shreefintech.paytouchconsumer.enums

enum class NextStep(val value: String) {
    KYC_REQUIRED("kyc_required"),
    PENDING_APPROVAL("pending_approval"),
    MPIN_REQUIRED("mpin_required");

    companion object {
        fun from(value: String?): NextStep? = entries.find { it.value == value }
    }
}
