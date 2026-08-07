package com.shreefintech.paytouchconsumer

object Constant {

    // API Base URLs
    const val BASE_URL = "https://www.paytouch.in/"
    const val BASE_URL_ADMIN = "https://admin.paytouch.in/"

    // AUTH store keys
    const val KEY_TOKEN = "TOKEN"
    const val KEY_USER_ID = "USERID"
    const val KEY_EMAIL = "EMAIL"
    const val KEY_MOBILE = "MOBILE"
    const val KEY_TOKEN_TYPE = "TOKEN_TYPE"
    const val KEY_WALLET_BALANCE = "WALLET_BALANCE"


    const val KEY_REFERRAL_CODE = "ReferralCode"

    // External URLs
    const val URL_PLATFORM_TERMS = "https://www.paytouch.in/terms/platform"

    // Circle IDs for bill payment modules
    const val GAS_CIRCLE_ID  = "0"
    const val LOAN_CIRCLE_ID = "0"

    // Auth flow type extras
    const val EXTRA_FLOW_TYPE = "FLOW_TYPE"
    const val EXTRA_MOBILE = "EXTRA_MOBILE"
    const val FLOW_RESET_PASSWORD = "RESET_PASSWORD"
    const val FLOW_RESET_MPIN = "RESET_MPIN"

    // Load Wallet / Payment status extras
    const val EXTRA_FROM_PAYMENT = "from_payment"

}
