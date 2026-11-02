package com.shreefintech.paytouchconsumer.retrofit.model.v2

import com.google.gson.annotations.SerializedName

data class BankDetailsV2Item(
    @field:SerializedName("success") val success: Boolean?,
    @field:SerializedName("message") val message: String?
)

data class BankDetailsV2Request(
    @field:SerializedName("account_number") val accountNumber: String,
    @field:SerializedName("ifsc")           val ifsc: String,
    @field:SerializedName("branch")         val branch: String
)
