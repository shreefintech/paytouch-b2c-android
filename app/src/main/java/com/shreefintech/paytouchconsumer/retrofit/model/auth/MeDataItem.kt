package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

// Response of GET auth/me and POST auth/mpin
data class MeDataItem(
    @field:SerializedName("user")               val user: AuthUserItem?,
    @field:SerializedName("next_step")          val nextStep: String?,
    @field:SerializedName("mpin_required")      val mpinRequired: Boolean?,
    @field:SerializedName("bank_details_added") val bankDetailsAdded: Boolean?,
    @field:SerializedName("aadhaar_added")      val aadhaarAdded: Boolean?,
    @field:SerializedName("pan_added")          val panAdded: Boolean?,
    // Non-null only while next_step is kyc_resubmit_required — drives the Aadhaar re-upload dialog on Home
    @field:SerializedName("kyc_rejection")      val kycRejection: KycRejectionItem?
)

data class KycRejectionItem(
    // Rejected item keys: aadhaar_number, aadhaar_photo, bank_details
    @field:SerializedName("items")  val items: List<String>?,
    @field:SerializedName("reason") val reason: String?
)
