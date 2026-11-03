package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Response of GET virtual-account — VA card on Load Wallet
data class VirtualAccountItem(
    @field:SerializedName("has_virtual_account") val hasVirtualAccount: Boolean?,
    @field:SerializedName("bank_acc")            val bankAcc: String?,
    @field:SerializedName("vpa")                 val vpa: String?,
    @field:SerializedName("ifsc")                val ifsc: String?,
    @field:SerializedName("bank_name")           val bankName: String?,
    @field:SerializedName("is_active")           val isActive: Boolean?,
    @field:SerializedName("kyc_status")          val kycStatus: String?,
    @field:SerializedName("live_balance")        val liveBalance: String?,
    @field:SerializedName("qr_code_url")         val qrCodeUrl: String?
)
