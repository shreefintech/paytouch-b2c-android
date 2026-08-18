package com.shreefintech.paytouchconsumer.enums

enum class KycSectionStatus(val value: String) {
    PENDING("pending"),
    UNDER_REVIEW("under_review"),
    REJECTED("rejected");

    companion object {
        fun from(value: String?): KycSectionStatus? =
            entries.find { it.value == value }
    }
}
