package com.shreefintech.paytouchconsumer.retrofit.model.auth

data class CreateMpinRequest(
    val mpin: String,
    val mpin_confirmation: String
)
