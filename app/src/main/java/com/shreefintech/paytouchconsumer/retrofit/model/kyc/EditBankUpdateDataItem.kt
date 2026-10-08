package com.shreefintech.paytouchconsumer.retrofit.model.kyc

import com.google.gson.annotations.SerializedName

data class EditBankUpdateDataItem(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("is_primary") val isPrimary: Boolean?,
    @field:SerializedName("account_holder_name") val accountHolderName: String?,
    @field:SerializedName("account_number") val accountNumber: String?,
    @field:SerializedName("ifsc") val ifsc: String?,
    @field:SerializedName("bank_name") val bankName: String?,
    @field:SerializedName("branch_name") val branchName: String?
)
