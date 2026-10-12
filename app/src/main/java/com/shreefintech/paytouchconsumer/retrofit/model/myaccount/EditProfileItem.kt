package com.shreefintech.paytouchconsumer.retrofit.model.myaccount

import com.google.gson.annotations.SerializedName
import com.shreefintech.paytouchconsumer.retrofit.model.CityItem
import com.shreefintech.paytouchconsumer.retrofit.model.StateItem

data class EditProfileDataItem(
    @field:SerializedName("name")   val name: String?,
    @field:SerializedName("email")  val email: String?,
    @field:SerializedName("mobile") val mobile: String?,
    @field:SerializedName("state")  val state: StateItem?,
    @field:SerializedName("city")   val city: CityItem?
)

data class EditProfileRequest(
    @field:SerializedName("name")     val name: String,
    @field:SerializedName("email")    val email: String,
    @field:SerializedName("state_id") val stateId: Int,
    @field:SerializedName("city_id")  val cityId: Int
)
