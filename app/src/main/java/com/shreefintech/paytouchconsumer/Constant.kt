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
    // True when KEY_FCM_TOKEN was registered with a bearer token (logged-in user).
    // Cleared on logout so the next login re-registers as authenticated.
    const val KEY_FCM_TOKEN_AUTHED = "fcm_token_authed"
    const val FCM_PLATFORM_ANDROID = "android"
    const val FCM_LANGUAGE_DEFAULT = "en"

    // Location — true once the system location permission popup has been shown at least once.
    // Distinguishes "never asked" from "permanently denied" (both report no rationale).
    const val KEY_LOCATION_ASKED = "LOCATION_ASKED"

    // KYC selfie — true once the user explicitly tapped "Deny" on the camera popup.
    // A later denial with no rationale then means "Don't ask again" rather than a tap-outside dismiss.
    const val KEY_CAMERA_DENIED = "CAMERA_DENIED"

    // In-app review — device-level, kept through logout (see SharedPreferenceHelper.clearSharedPreference)
    const val KEY_REVIEW_SUCCESS_COUNT = "REVIEW_SUCCESS_COUNT"   // successful bill payments counted so far
    const val KEY_REVIEW_LAST_TXN_ID = "REVIEW_LAST_TXN_ID"       // last counted transaction — stops double counting on retry
    const val KEY_REVIEW_LAST_ASKED_AT = "REVIEW_LAST_ASKED_AT"   // epoch millis of the last review flow launch
    const val REVIEW_MIN_SUCCESS_PAYMENTS = 3
    const val REVIEW_MIN_INSTALL_DAYS = 3L
    const val REVIEW_COOLDOWN_DAYS = 90L
    const val REVIEW_DELAY_MS = 1500L

    // Bill payment receipt — success status (compared case-insensitively)
    const val PAYMENT_STATUS_SUCCESS = "success"

    // KYC local files (under filesDir, exposed via file_provider_paths.xml) — each owning
    // Activity deletes its own directory when it finishes so ID documents are not retained.
    const val KYC_SELFIE_DIR = "kyc"
    const val KYC_IDENTITY_DOCS_DIR = "kyc_docs/identity"
    const val KYC_BANK_DOCS_DIR = "kyc_docs/bank"
    const val KYC_IMAGE_MAX_BYTES = 800 * 1024

    // External URLs
    const val URL_PLATFORM_TERMS = "https://www.paytouch.in/terms/platform"
    const val URL_GOOGLE_DOC_VIEWER = "https://docs.google.com/gviewer?embedded=true&url="

    // Circle IDs for bill payment modules
    const val LOAN_CIRCLE_ID = "0"
    const val POSTPAID_CIRCLE_ID = "00"

    // App lock — time away before the phone's screen lock is asked again
    const val APP_LOCK_GRACE_MS = 60 * 1000L                // normal app switch
    const val APP_LOCK_EXTERNAL_GRACE_MS = 5 * 60 * 1000L   // camera, file picker, UPI app, payment gateway, share, browser

    // Rewards level-up — last level seen by the user; cleared on logout via clearSharedPreference()
    const val KEY_LAST_SEEN_LEVEL = "LAST_SEEN_LEVEL"

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

    // Wallet withdrawal — status values (compared case-insensitively)
    const val WITHDRAW_STATUS_SUCCESS = "SUCCESS"
    const val WITHDRAW_STATUS_COMPLETED = "COMPLETED"
    const val WITHDRAW_STATUS_FAILED = "FAILED"
    const val WITHDRAW_STATUS_REJECTED = "REJECTED"
    const val WITHDRAW_STATUS_REVERSED = "REVERSED"

    // HDFC order creation
    const val HDFC_ORDER_PURPOSE_WALLET_TOPUP = "wallet_topup"

    // Transaction type values returned by /api/transactions/{id}
    const val TRANSACTION_TYPE_HDFC = "hdfc_smartgateway"

    // Image storage — sub-folder of filesDir; must match <files-path> entries in file_provider_paths.xml
    const val DIR_RECEIPTS = "receipts"

    // Earning Wallet
    const val EARNING_LOCK_DAYS = 30
    const val EARNING_DEFAULT_LOCK_AMOUNT = 1000

    // Support contact details
    const val SUPPORT_PHONE_1 = "7567525558"
    const val SUPPORT_PHONE_2 = "7567525559"
    const val SUPPORT_PHONE_3 = "7567525557"
    const val SUPPORT_EMAIL = "info@paytouch.in"

}
