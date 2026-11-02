package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

// Response of GET auth/me and POST auth/mpin
data class MeDataItem(
    @field:SerializedName("user")               val user: AuthUserItem?,
    @field:SerializedName("next_step")          val nextStep: String?,
    @field:SerializedName("mpin_required")      val mpinRequired: Boolean?,
    @field:SerializedName("bank_details_added") val bankDetailsAdded: Boolean?,
    @field:SerializedName("aadhaar_added")      val aadhaarAdded: Boolean?,
    @field:SerializedName("pan_added")          val panAdded: Boolean?
)
