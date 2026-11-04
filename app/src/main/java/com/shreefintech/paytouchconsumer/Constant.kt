package com.shreefintech.paytouchconsumer

object Constant {


    // API Base URLs
    // swap BASE_URL to LOCAL_URL for local testing only — never commit that swap
    const val LOCAL_URL = "https://tablet-frying-shy.ngrok-free.dev/"

    const val LIVE_URL = "https://www.paytouch.in/"
    // B2C backend — staging url
    const val B2C_URL = "https://backend-b2c.paytouch.in/"
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

    // Top-level `error_code` on a 403 from money calls (pay, top-up, withdraw)
    const val ERROR_CODE_USER_BLOCKED = "USER_BLOCKED"

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

    // Bill Pay (api/mobile/billpay) — default account param keys when a biller sends input_params: []
    const val BILLPAY_PARAM_CONSUMER_NUMBER = "consumer_number"
    const val BILLPAY_PARAM_MOBILE_NUMBER = "mobile_number"
    const val BILLPAY_PARAM_CIRCLE_CODE = "circle_code"
    // Imported old payments (transactions rows): input_params = {source: "old_b2c", identifier: "<account>"}
    const val BILLPAY_PARAM_IDENTIFIER = "identifier"
    const val BILLPAY_PARAM_SOURCE = "source"
    // Bill Pay status values — final: success / failed / expired; open: pending / processing / unconfirmed
    const val BILLPAY_STATUS_SUCCESS = "success"
    const val BILLPAY_STATUS_FAILED = "failed"
    const val BILLPAY_STATUS_EXPIRED = "expired"
    const val BILLPAY_STATUS_PENDING = "pending"
    const val BILLPAY_STATUS_PROCESSING = "processing"
    const val BILLPAY_STATUS_UNCONFIRMED = "unconfirmed"
    // 403 error_code values handled by the Bill Pay screen (others are handled in ApiHelper)
    const val ERROR_CODE_BANK_DETAILS_REQUIRED = "BANK_DETAILS_REQUIRED"
    const val ERROR_CODE_LOCATION_REQUIRED = "LOCATION_REQUIRED"
    // Status polling after pay
    const val BILLPAY_POLL_INTERVAL_MS = 2500L
    const val BILLPAY_POLL_MAX_ATTEMPTS = 24

    // KYC local files (under filesDir, exposed via file_provider_paths.xml) — each owning
    // Activity deletes its own directory when it finishes so ID documents are not retained.
    const val KYC_SELFIE_DIR = "kyc"
    const val KYC_IDENTITY_DOCS_DIR = "kyc_docs/identity"
    const val KYC_BANK_DOCS_DIR = "kyc_docs/bank"
    const val REG_AADHAAR_DIR = "reg_docs/aadhaar"
    const val KYC_AADHAAR_REUPLOAD_DIR = "reg_docs/aadhaar_reupload"
    const val KYC_IMAGE_MAX_BYTES = 800 * 1024

    // Max decoded bitmap height (PDF pages, preview images) — tall pages / huge images otherwise allocate 100 MB+ and crash on draw
    const val MAX_BITMAP_HEIGHT_PX = 4096

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
    const val EXTRA_RESET_TOKEN = "EXTRA_RESET_TOKEN"
    const val FLOW_RESET_PASSWORD = "RESET_PASSWORD"
    const val FLOW_RESET_MPIN = "RESET_MPIN"

    // `type` field for auth/login and auth/forgot-credential/*
    const val CREDENTIAL_TYPE_PASSWORD = "password"
    const val CREDENTIAL_TYPE_MPIN = "mpin"

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
    const val WITHDRAW_STATUS_SUCCESSFUL = "SUCCESSFUL"
    const val WITHDRAW_STATUS_CHARGED = "CHARGED"
    const val WITHDRAW_STATUS_FAILURE = "FAILURE"
    const val WITHDRAW_STATUS_DECLINED = "DECLINED"
    // Withdrawals history rows: success = plain debit, failed = grey "Failed" (money back in wallet), else "Pending"
    val WITHDRAW_SUCCESS_STATUSES = setOf(WITHDRAW_STATUS_SUCCESS, WITHDRAW_STATUS_SUCCESSFUL, WITHDRAW_STATUS_COMPLETED, WITHDRAW_STATUS_CHARGED)
    val WITHDRAW_FAILED_STATUSES = setOf(
        WITHDRAW_STATUS_FAILED, WITHDRAW_STATUS_FAILURE, WITHDRAW_STATUS_REJECTED, WITHDRAW_STATUS_DECLINED, WITHDRAW_STATUS_REVERSED
    )

    // HDFC order creation
    const val HDFC_ORDER_PURPOSE_WALLET_TOPUP = "wallet_topup"

    // Wallet top-up (POST wallet/topups) `flow` values — Net Banking opens HDFC's payment page, UPI opens a UPI app
    const val TOPUP_FLOW_PAYMENT_PAGE = "payment_page"
    const val TOPUP_FLOW_UPI_INTENT = "upi_intent"
    // Required prefixes of upi_intent_url / payment_link before the app opens them
    const val TOPUP_UPI_URL_PREFIX = "upi://"
    const val TOPUP_PAYMENT_LINK_PREFIX = "https://"
    // GET wallet/statement `wallet` filter (main, referral, bonus, earning) — My Wallet lists show main
    const val WALLET_STATEMENT_MAIN = "main"
    const val WALLET_DIRECTION_CREDIT = "credit"

    // Fee preview payment_method_type values — the user picks the method on HDFC's page
    const val TOPUP_FEE_METHOD_UPI = "UPI"
    const val TOPUP_FEE_METHOD_CARD = "CARD"
    const val TOPUP_FEE_METHOD_NB = "NB"
    // Top-up final status values (compared case-insensitively); initiated / pending / crediting / on_hold show as processing
    const val TOPUP_STATUS_SUCCESS = "SUCCESS"
    const val TOPUP_STATUS_FAILED = "FAILED"
    const val TOPUP_STATUS_PENDING = "PENDING"
    // Paid at HDFC, held for manual review — final but shown as pending
    const val TOPUP_STATUS_ON_HOLD = "ON_HOLD"
    // Final top-up statuses that count as success (Wallet History / Load Wallet API doc, 2026-11-04)
    val TOPUP_SUCCESS_STATUSES = setOf("SUCCESS", "SUCCESSFUL", "COMPLETED", "CHARGED", "CREDITED")

    // Transaction type values returned by /api/transactions/{id}
    const val TRANSACTION_TYPE_HDFC = "hdfc_smartgateway"

    // Image storage — sub-folder of filesDir; must match <files-path> entries in file_provider_paths.xml
    const val DIR_RECEIPTS = "receipts"

    // B2C onboarding flags — cleared on logout via clearSharedPreference()
    // Set from auth/login, auth/me (Splash + Home) and auth/mpin; read with default false when unset (fail closed)
    const val KEY_BANK_DETAILS_ADDED = "BANK_DETAILS_ADDED"
    const val KEY_PAN_ADDED = "PAN_ADDED"

    // Earning Wallet
    const val EARNING_LOCK_DAYS = 30
    const val EARNING_QUICK_LOCK_AMOUNT = 500
    const val EARNING_DEFAULT_LOCK_AMOUNT = 1000
    const val EARNING_MIN_LOCK_AMOUNT = 100

    // Support contact details
    const val SUPPORT_PHONE_1 = "7567525558"
    const val SUPPORT_PHONE_2 = "7567525559"
    const val SUPPORT_PHONE_3 = "7567525557"
    const val SUPPORT_EMAIL = "info@paytouch.in"

}
