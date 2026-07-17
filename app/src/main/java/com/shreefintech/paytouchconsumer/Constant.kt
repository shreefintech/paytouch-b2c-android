package com.shreefintech.paytouchconsumer

object Constant {

    // API Base URLs
    const val BASE_URL       = "https://www.paytouch.in/"
    const val BASE_URL_ADMIN = "https://admin.paytouch.in/"

    // AUTH store keys
    const val KEY_TOKEN        = "TOKEN"
    const val KEY_USER_ID      = "USERID"
    const val KEY_EMAIL        = "EMAIL"
    const val KEY_MOBILE       = "MOBILE"
    const val KEY_TOKEN_TYPE   = "TOKEN_TYPE"
    const val KEY_REFERRAL_CODE = "ReferralCode"

    // app_prefs store keys
    const val KEY_MPIN_CREATED   = "mpin_created"
    const val KEY_VIRTUAL_ACCOUNT = "virtual_account"

    // LOGIN_PREF store keys
    const val KEY_REMEMBER             = "REMEMBER"
    const val KEY_LOGIN_MOBILE         = "MOBILE_LOGIN"
    const val KEY_LOGIN_TYPE_PASSWORD  = "LOGIN_TYPE_PASSWORD"

    // Auth flow type extras
    const val EXTRA_FLOW_TYPE       = "FLOW_TYPE"
    const val FLOW_RESET_PASSWORD   = "RESET_PASSWORD"
    const val FLOW_RESET_MPIN       = "RESET_MPIN"

    // Intent extra keys
    const val EXTRA_TRANSACTION_ID   = "TRANSACTION_ID"
    const val EXTRA_AMOUNT           = "AMOUNT"
    const val EXTRA_OPERATOR         = "OPERATOR"
    const val EXTRA_CONSUMER_NUMBER  = "CONSUMER_NUMBER"
}
