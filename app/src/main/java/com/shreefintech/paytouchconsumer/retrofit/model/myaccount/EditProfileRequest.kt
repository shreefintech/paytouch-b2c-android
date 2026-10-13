package com.shreefintech.paytouchconsumer.retrofit.model.myaccount

import com.google.gson.annotations.SerializedName

data class EditProfileRequest(
    @field:SerializedName("name")     val name: String,
    @field:SerializedName("email")    val email: String,
    @field:SerializedName("state_id") val stateId: Int,
    @field:SerializedName("city_id")  val cityId: Int
)
