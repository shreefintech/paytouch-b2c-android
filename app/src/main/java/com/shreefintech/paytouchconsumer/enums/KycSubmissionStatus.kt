package com.shreefintech.paytouchconsumer.enums

enum class KycSubmissionStatus(val value: String) {
    PENDING_KYC("pending_kyc"),
    KYC_SUBMITTED("kyc_submitted"),
    KYC_APPROVED("kyc_approved"),
    KYC_REJECTED("kyc_rejected");

    companion object {
        fun from(value: String?): KycSubmissionStatus? =
            entries.find { it.value == value }
    }
}
