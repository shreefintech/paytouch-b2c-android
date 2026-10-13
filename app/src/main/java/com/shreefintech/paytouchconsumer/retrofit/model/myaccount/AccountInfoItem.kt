package com.shreefintech.paytouchconsumer.retrofit.model.myaccount

import com.google.gson.annotations.SerializedName
import com.shreefintech.paytouchconsumer.retrofit.model.CityItem
import com.shreefintech.paytouchconsumer.retrofit.model.StateItem

data class AccountInfoItem(
    @field:SerializedName("success")        val success: Boolean?,
    @field:SerializedName("name")           val name: String?,
    @field:SerializedName("member")         val member: AccountInfoMemberItem?,
    @field:SerializedName("wallet_balance") val walletBalance: String?,
    @field:SerializedName("contact")        val contact: AccountInfoContactItem?,
    @field:SerializedName("home_address")   val homeAddress: String?,
    @field:SerializedName("address")        val address: AccountInfoAddressItem?,
    @field:SerializedName("membership")     val membership: AccountInfoMembershipItem?
)

data class AccountInfoContactItem(
    @field:SerializedName("mobile") val mobile: String?,
    @field:SerializedName("email")  val email: String?
)

data class AccountInfoAddressItem(
    @field:SerializedName("state") val state: StateItem?,
    @field:SerializedName("city")  val city: CityItem?
)

data class AccountInfoMembershipItem(
    @field:SerializedName("registered_on") val registeredOn: String?,
    @field:SerializedName("activated_on")  val activatedOn: String?
)
