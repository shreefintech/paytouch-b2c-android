package com.shreefintech.paytouchconsumer.retrofit.model.myaccount

import com.google.gson.annotations.SerializedName

data class AccountInfoMemberItem(
    @field:SerializedName("member_code") val memberCode: String?,
    @field:SerializedName("city")        val city: String?,
    @field:SerializedName("status")      val status: String?
)
