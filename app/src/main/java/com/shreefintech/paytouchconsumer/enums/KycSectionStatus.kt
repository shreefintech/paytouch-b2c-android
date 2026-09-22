package com.shreefintech.paytouchconsumer.enums

enum class KycSectionStatus(val value: String) {
    PENDING("pending"),
    SUBMITTED("submitted"),
    APPROVED("approved"),
    REJECTED("rejected");

    companion object {
        fun from(value: String?): KycSectionStatus? =
            entries.find { it.value == value }
    }
}
