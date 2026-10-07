package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class KycBankAccountDetailItem(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("account_number") val accountNumber: String?,
    @field:SerializedName("bank_name") val bankName: String?,
    @field:SerializedName("ifsc") val ifsc: String?,
    @field:SerializedName("branch_name") val branchName: String?,
    @field:SerializedName("status") val status: String?,
    @field:SerializedName("is_primary") val isPrimary: Boolean?,
    @field:SerializedName("account_holder_name") val accountHolderName: String?,
    @field:SerializedName("bank_proof_url") val bankProofUrl: String?
)
