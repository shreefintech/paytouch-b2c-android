package com.shreefintech.paytouchconsumer

object Constant {


    // API Base URLs
    // swap BASE_URL to LOCAL_URL for local testing only — never commit that swap
    const val LOCAL_URL = "https://tablet-frying-shy.ngrok-free.dev/"

    const val LIVE_URL = "https://www.paytouch.in/"
    const val BASE_URL = LIVE_URL
    const val BASE_URL_ADMIN = "https://admin.paytouch.in/"

    // AUTH store keys
    const val KEY_TOKEN = "TOKEN"
    const val KEY_USER_ID = "USERID"
    const val KEY_EMAIL = "EMAIL"
    const val KEY_MOBILE = "MOBILE"
    const val KEY_TOKEN_TYPE = "TOKEN_TYPE"
    const val KEY_WALLET_BALANCE = "WALLET_BALANCE"
    const val KEY_PENDING_ORDER_ID = "PENDING_ORDER_ID"
    const val KEY_PENDING_AMOUNT = "PENDING_AMOUNT"


    const val KEY_REFERRAL_CODE = "ReferralCode"

    // Push notifications — last FCM token accepted by the backend
    const val KEY_FCM_TOKEN = "FCM_TOKEN"
    const val FCM_PLATFORM_ANDROID = "android"

    // Location — true once the system location permission popup has been shown at least once.
    // Distinguishes "never asked" from "permanently denied" (both report no rationale).
    const val KEY_LOCATION_ASKED = "LOCATION_ASKED"

    // KYC selfie — true once the user explicitly tapped "Deny" on the camera popup.
    // A later denial with no rationale then means "Don't ask again" rather than a tap-outside dismiss.
    const val KEY_CAMERA_DENIED = "CAMERA_DENIED"

    // External URLs
    const val URL_PLATFORM_TERMS = "https://www.paytouch.in/terms/platform"
    const val URL_GOOGLE_DOC_VIEWER = "https://docs.google.com/gviewer?embedded=true&url="

    // Circle IDs for bill payment modules
    const val LOAN_CIRCLE_ID = "0"

    // Session timeout
    const val KEY_LAST_INTERACTION = "LAST_INTERACTION"
    const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L

    // Auth flow type extras
    const val EXTRA_FLOW_TYPE = "FLOW_TYPE"
    const val EXTRA_MOBILE = "EXTRA_MOBILE"
    const val FLOW_RESET_PASSWORD = "RESET_PASSWORD"
    const val FLOW_RESET_MPIN = "RESET_MPIN"

    // Load Wallet / Payment status extras
    const val EXTRA_FROM_PAYMENT = "from_payment"

    // HDFC Payment Gateway — order status codes
    const val HDFC_STATUS_CHARGED = "CHARGED"
    const val HDFC_STATUS_AUTHORIZED = "AUTHORIZED"
    const val HDFC_STATUS_NEW = "NEW"
    const val HDFC_STATUS_PENDING_VBV = "PENDING_VBV"
    const val HDFC_STATUS_AUTHORIZING = "AUTHORIZING"
    const val HDFC_STATUS_STARTED = "STARTED"
    const val HDFC_STATUS_JUSPAY_DECLINED = "JUSPAY_DECLINED"
    const val HDFC_STATUS_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED"
    const val HDFC_STATUS_AUTHORIZATION_FAILED = "AUTHORIZATION_FAILED"
    const val HDFC_STATUS_AUTO_REFUNDED = "AUTO_REFUNDED"

    // HDFC order creation
    const val HDFC_ORDER_PURPOSE_WALLET_TOPUP = "wallet_topup"

    // Transaction type values returned by /api/transactions/{id}
    const val TRANSACTION_TYPE_HDFC = "hdfc_smartgateway"

}
