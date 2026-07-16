package com.shreefintech.paytouchconsumer.auth.model

data class LoginItem(
    val mobile: String,
    val password: String = "",
    val mpin: String = "",
    val loginMode: String = "PASSWORD"
)
