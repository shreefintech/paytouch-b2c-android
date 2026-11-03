package com.shreefintech.paytouchconsumer.enums

// MPIN setup is not a next_step value — it is driven by the separate `mpin_required` flag
enum class NextStep(val value: String) {
    KYC_REQUIRED("kyc_required"),
    AADHAAR_REQUIRED("aadhaar_required"),
    KYC_RESUBMIT_REQUIRED("kyc_resubmit_required"),
    PENDING_APPROVAL("pending_approval"),
    REGISTRATION_REJECTED("registration_rejected"),
    BANK_DETAILS_REQUIRED("bank_details_required"),
    COMPLETE("complete");

    val isKycStep: Boolean
        get() = this == KYC_REQUIRED || this == AADHAAR_REQUIRED || this == KYC_RESUBMIT_REQUIRED ||
                this == PENDING_APPROVAL || this == REGISTRATION_REJECTED

    // Steps that Home resolves with the Aadhaar dialog (kyc_required opens KycActivity instead)
    val needsAadhaar: Boolean
        get() = this == AADHAAR_REQUIRED || this == KYC_RESUBMIT_REQUIRED || this == REGISTRATION_REJECTED

    companion object {
        fun from(value: String?): NextStep? = entries.find { it.value == value }
    }
}
