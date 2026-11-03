package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// `data` of GET / PUT mobile/wallet/bank-account — the single withdrawal bank (bank_id 0)
data class WalletBankAccountItem(
    @field:SerializedName("has_bank_account") val hasBankAccount: Boolean?,
    @field:SerializedName("bank_name")        val bankName: String?,
    @field:SerializedName("account_masked")   val accountMasked: String?,
    @field:SerializedName("ifsc")             val ifsc: String?,
    @field:SerializedName("holder_name")      val holderName: String?
)

data class WalletBankAccountRequest(
    @field:SerializedName("holder_name")    val holderName: String,
    @field:SerializedName("account_number") val accountNumber: String,
    @field:SerializedName("ifsc")           val ifsc: String,
    @field:SerializedName("bank_name")      val bankName: String,
    @field:SerializedName("mpin")           val mpin: String
)
