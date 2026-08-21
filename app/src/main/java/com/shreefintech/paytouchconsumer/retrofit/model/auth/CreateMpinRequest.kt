package com.shreefintech.paytouchconsumer.retrofit.model.auth

import com.google.gson.annotations.SerializedName

data class CreateMpinRequest(
    @field:SerializedName("mpin")
    val mpin: String,

    @field:SerializedName("mpin_confirmation")
    val mpinConfirmation: String
)