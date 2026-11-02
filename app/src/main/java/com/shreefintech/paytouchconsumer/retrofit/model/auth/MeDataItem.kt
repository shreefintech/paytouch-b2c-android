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
    // Non-null only while a KYC rejection is open — drives the Aadhaar re-upload dialog on Home
    @field:SerializedName("kyc_rejection")      val kycRejection: Any?
) {
    // kyc_rejection = { items: ["aadhaar_number", "aadhaar_photo", "bank_details"], reason }; kept untyped so a
    // shape change can't fail the whole auth/me parse
    val kycRejectedItems: List<String>
        get() = ((kycRejection as? Map<*, *>)?.get("items") as? List<*>)?.filterIsInstance<String>().orEmpty()
}
