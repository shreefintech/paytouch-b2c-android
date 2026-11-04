package com.shreefintech.paytouchconsumer.retrofit

import com.shreefintech.paytouchconsumer.retrofit.model.CityItem
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayBillItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayBillerItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayCategoryItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayCircleItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayFeePreviewItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayPayRequest
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayPlanItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayTransactionItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayTransactionPageItem
import com.shreefintech.paytouchconsumer.retrofit.model.StateItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.CreateMpinRequest
import com.shreefintech.paytouchconsumer.retrofit.model.auth.ForgotCredentialRequest
import com.shreefintech.paytouchconsumer.retrofit.model.auth.LoginDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.LoginRequest
import com.shreefintech.paytouchconsumer.retrofit.model.auth.LogoutRequest
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MeDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MessageItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.RegisterRequest
import com.shreefintech.paytouchconsumer.retrofit.model.auth.ResetCredentialRequest
import com.shreefintech.paytouchconsumer.retrofit.model.auth.ResetTokenDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.VerifyOtpRequest
import com.shreefintech.paytouchconsumer.retrofit.model.location.UserLocationRequest
import com.shreefintech.paytouchconsumer.retrofit.model.notification.DeviceTokenRequest
import com.shreefintech.paytouchconsumer.retrofit.model.v2.BankDetailsV2Item
import com.shreefintech.paytouchconsumer.retrofit.model.v2.BankDetailsV2Request
import com.shreefintech.paytouchconsumer.retrofit.model.v2.PanV2Item
import com.shreefintech.paytouchconsumer.retrofit.model.v2.PanV2Request
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.BonusRewardItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.VirtualAccountItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletBalanceItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletEarningAmountRequest
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletEarningItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletStatementItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletTopupFeeItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletWithdrawalFeeItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletBankAccountItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletBankAccountRequest
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletTopupItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletTopupRequest
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletWithdrawalItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletWithdrawalRequest
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.EditBankUpdateDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycAgreeDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycMyAccountItem
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycSignatoryDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycStatusItem
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycSubmissionDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.EditProfileRequest
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.ReferralRewardsItem
import com.shreefintech.paytouchconsumer.retrofit.model.hdfc.HdfcCreateOrderRequest
import com.shreefintech.paytouchconsumer.retrofit.model.hdfc.HdfcOrderItem
import com.shreefintech.paytouchconsumer.retrofit.model.transactions.TransactionHistoryDetailItem
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.EarningWalletActionDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.EarningWalletAmountRequest
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.EarningWalletItem
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.RewardsLevelItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.BonusWalletItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletHistoryPageItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WithdrawDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WithdrawRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    companion object {
        const val AUTH = "api/"
        // Migrated routes (auth, onboarding, KYC) — https://backend-b2c.paytouch.in/api/mobile/
        const val MOBILE = "api/mobile/"
    }

    // ── Session ───────────────────────────────────────────────────────────────

    // Edit Profile — mpin required on every change. Flat: `data` is unused (screen refetches auth/me), and typing
    // it would fail the whole parse if its shape differs (auth/me sends state/city as plain names)
    @PUT("${MOBILE}auth/me")
    fun updateMe(
        @Header("Authorization") authorization: String,
        @Body request: EditProfileRequest
    ): Call<MessageItem>

    @GET("${MOBILE}auth/me")
    fun getMe(
        @Header("Authorization") authorization: String
    ): Call<General<MeDataItem>>

    @POST("${MOBILE}auth/logout")
    fun logout(
        @Header("Authorization") authorization: String,
        @Body body: LogoutRequest
    ): Call<MessageItem>

    // ── Push Notifications ────────────────────────────────────────────────────

    @POST("${MOBILE}device-token")
    fun registerDeviceToken(
        @Header("Authorization") authorization: String?,
        @Body body: DeviceTokenRequest
    ): Call<MessageItem>

    // ── Location ──────────────────────────────────────────────────────────────

    @POST("${MOBILE}location")
    fun sendLocation(
        @Header("Authorization") authorization: String,
        @Body body: UserLocationRequest
    ): Call<MessageItem>

    // ── Authentication ────────────────────────────────────────────────────────

    @POST("${MOBILE}auth/login")
    fun login(
        @Body request: LoginRequest
    ): Call<General<LoginDataItem>>

    @POST("${MOBILE}auth/register")
    fun register(
        @Body request: RegisterRequest
    ): Call<General<LoginDataItem>>

    // ── Forgot Credential (unified OTP flow) ──────────────────────────────────

    @POST("${MOBILE}auth/forgot-credential")
    fun sendForgotCredentialOtp(
        @Body request: ForgotCredentialRequest
    ): Call<MessageItem>

    @POST("${MOBILE}auth/forgot-credential/verify-otp")
    fun verifyForgotCredentialOtp(
        @Body request: VerifyOtpRequest
    ): Call<General<ResetTokenDataItem>>

    @POST("${MOBILE}auth/forgot-credential/reset")
    fun resetCredential(
        @Body request: ResetCredentialRequest
    ): Call<MessageItem>

    // Same response shape as auth/me; 422 if an MPIN is already set
    @POST("${MOBILE}auth/mpin")
    fun createMpin(
        @Header("Authorization") token: String,
        @Body body: CreateMpinRequest
    ): Call<General<MeDataItem>>

    // ── B2C V2 Registration & Onboarding ─────────────────────────────────────

    @Multipart
    @POST("${MOBILE}auth/register/individual")
    fun registerV2(
        @Part("name") name: RequestBody,
        @Part("mobile") mobile: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part("password_confirmation") passwordConfirmation: RequestBody,
        @Part("aadhaar_number") aadhaarNumber: RequestBody,
        @Part aadhaarPhoto: MultipartBody.Part,
        @Part("state_id") stateId: RequestBody,
        @Part("city_id") cityId: RequestBody,
        @Part("referral_code") referralCode: RequestBody,
        @Part("platform") platform: RequestBody
    ): Call<MessageItem>

    // Re-upload after KYC rejection — triggered by top-level `kyc_rejection` on auth/me; resubmit takes only the rejected items
    @Multipart
    @POST("${MOBILE}kyc/individual/resubmit")
    fun reuploadAadhaar(
        @Header("Authorization") authorization: String,
        @Part("aadhaar_number") aadhaarNumber: RequestBody,
        @Part aadhaarPhoto: MultipartBody.Part
    ): Call<MessageItem>

    // aadhaar_required — existing account with no Aadhaar on file; afterwards next_step is bank_details_required.
    // 409 = Aadhaar already on file / KYC approved; 422 = not linked to Dashboard or Aadhaar used by another account
    @Multipart
    @POST("${MOBILE}kyc/individual/aadhaar")
    fun addAadhaar(
        @Header("Authorization") authorization: String,
        @Part("aadhaar_number") aadhaarNumber: RequestBody,
        @Part aadhaarPhoto: MultipartBody.Part,
        @Part("state_id") stateId: RequestBody,
        @Part("city_id") cityId: RequestBody
    ): Call<MessageItem>

    @GET("${MOBILE}address/states")
    fun getAddressStates(): Call<General<List<StateItem>>>

    @GET("${MOBILE}address/states/{state_id}/cities")
    fun getAddressCities(
        @Path("state_id") stateId: String
    ): Call<General<List<CityItem>>>

    @POST("${MOBILE}kyc/individual/bank-details")
    fun addBankDetails(
        @Header("Authorization") authorization: String,
        @Body body: BankDetailsV2Request
    ): Call<BankDetailsV2Item>

    @POST("${MOBILE}kyc/individual/pan")
    fun addPan(
        @Header("Authorization") authorization: String,
        @Body body: PanV2Request
    ): Call<PanV2Item>

    // ── Withdrawal bank (View KYC Details / Edit Bank Details) ───────────────

    @GET("${MOBILE}wallet/bank-account")
    fun getWalletBankAccount(
        @Header("Authorization") authorization: String
    ): Call<General<WalletBankAccountItem>>

    @PUT("${MOBILE}wallet/bank-account")
    fun saveWalletBankAccount(
        @Header("Authorization") authorization: String,
        @Body body: WalletBankAccountRequest
    ): Call<General<WalletBankAccountItem>>

    // TODO(B2C-287): routes below (except Dashboard KYC, already migrated) still use old paths — replace with new routes in separate branches

    // ── Wallet ────────────────────────────────────────────────────────────────

    @GET("${AUTH}bonus-wallet")
    fun getBonusWallet(
        @Header("Authorization") authorization: String
    ): Call<General<BonusWalletItem?>>

    @GET("${AUTH}earning-wallet")
    fun getEarningWallet(
        @Header("Authorization") authorization: String
    ): Call<General<EarningWalletItem?>>

    // Old route — replaced by getCustomerLevel (mobile/rewards/level); no callers left
    @GET("${AUTH}level")
    fun getRewardsLevel(
        @Header("Authorization") authorization: String
    ): Call<General<RewardsLevelItem?>>

    // Old route — replaced by getWalletStatement (Load Wallet recent list + Wallet Transactions)
    @GET("${AUTH}wallet/combined-wallet-history")
    fun getWalletHistory(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Call<General<WalletHistoryPageItem>>

    @POST("${AUTH}wallet/withdraw")
    fun withdrawWallet(
        @Header("Authorization") authorization: String,
        @Body request: WithdrawRequest
    ): Call<General<WithdrawDataItem>>

    @POST("${AUTH}earning-wallet/opt-in")
    fun postEarningWalletOptIn(
        @Header("Authorization") authorization: String
    ): Call<General<EarningWalletActionDataItem>>

    @POST("${AUTH}earning-wallet/lock")
    fun postEarningWalletLock(
        @Header("Authorization") authorization: String,
        @Body request: EarningWalletAmountRequest
    ): Call<General<EarningWalletActionDataItem>>

    @POST("${AUTH}earning-wallet/withdraw")
    fun postEarningWalletWithdraw(
        @Header("Authorization") authorization: String,
        @Body request: EarningWalletAmountRequest
    ): Call<General<EarningWalletActionDataItem>>

    // ── Transaction Detail ────────────────────────────────────────────────────

    @GET("${AUTH}utility-bill/transaction/{id}")
    fun getTransactionHistoryDetail(
        @Header("Authorization") authorization: String,
        @Path("id") transactionId: String
    ): Call<General<TransactionHistoryDetailItem>>

    // ── My Account ───────────────────────────────────────────────────────────

    @GET("${MOBILE}rewards/referral")
    fun getReferralRewards(
        @Header("Authorization") authorization: String
    ): Call<ReferralRewardsItem>

    @GET("${MOBILE}wallet/balance")
    fun getWalletBalance(
        @Header("Authorization") authorization: String
    ): Call<General<WalletBalanceItem>>

    // Load Wallet top-up — replaces hdfc/orders + hdfc/orders/{order_id}/status
    @POST("${MOBILE}wallet/topups")
    fun createWalletTopup(
        @Header("Authorization") authorization: String,
        @Body request: WalletTopupRequest
    ): Call<General<WalletTopupItem>>

    @GET("${MOBILE}wallet/topups/{client_reference}")
    fun getWalletTopup(
        @Header("Authorization") authorization: String,
        @Path("client_reference") clientReference: String
    ): Call<General<WalletTopupItem>>

    @GET("${MOBILE}wallet/topups/fee-preview")
    fun getTopupFeePreview(
        @Header("Authorization") authorization: String,
        @Query("amount") amount: Double,
        @Query("payment_method_type") paymentMethodType: String
    ): Call<General<WalletTopupFeeItem>>

    @GET("${MOBILE}wallet/withdrawals/fee-preview")
    fun getWithdrawalFeePreview(
        @Header("Authorization") authorization: String,
        @Query("amount") amount: Double
    ): Call<General<WalletWithdrawalFeeItem>>

    @GET("${MOBILE}wallet/withdrawals/{client_reference}")
    fun getWalletWithdrawal(
        @Header("Authorization") authorization: String,
        @Path("client_reference") clientReference: String
    ): Call<General<WalletWithdrawalItem>>

    // Wallet ledger — replaces combined-wallet-history; cursor omitted on the first page
    @GET("${MOBILE}wallet/statement")
    fun getWalletStatement(
        @Header("Authorization") authorization: String,
        @Query("wallet") wallet: String,
        @Query("per_page") perPage: Int,
        @Query("cursor") cursor: String?
    ): Call<WalletStatementItem>

    // Load Wallet VA card — replaces wallet/user-data
    @GET("${MOBILE}virtual-account")
    fun getVirtualAccount(
        @Header("Authorization") authorization: String
    ): Call<General<VirtualAccountItem>>

    // Replaces earning-wallet (old route still used by EarningWalletViewModel)
    @GET("${MOBILE}wallet/earning")
    fun getWalletEarning(
        @Header("Authorization") authorization: String
    ): Call<General<WalletEarningItem>>

    // Earning wallet actions — replace earning-wallet/opt-in · lock · withdraw. Only success/message are read,
    // so the action's `data` shape (not shared yet) can't break the parse
    @POST("${MOBILE}wallet/earning/opt-in")
    fun postWalletEarningOptIn(
        @Header("Authorization") authorization: String
    ): Call<MessageItem>

    @POST("${MOBILE}wallet/earning/lock")
    fun postWalletEarningLock(
        @Header("Authorization") authorization: String,
        @Body request: WalletEarningAmountRequest
    ): Call<MessageItem>

    @POST("${MOBILE}wallet/earning/withdraw")
    fun postWalletEarningWithdraw(
        @Header("Authorization") authorization: String,
        @Body request: WalletEarningAmountRequest
    ): Call<MessageItem>

    // Replaces bonus-wallet history — Laravel paginated list in `data`
    @GET("${MOBILE}rewards/bonus")
    fun getBonusRewards(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Call<General<List<BonusRewardItem>>>

    // Referral History rows — same route as getReferralRewards (My Account, reads meta.summary only); this one reads `data`
    @GET("${MOBILE}rewards/referral")
    fun getReferralHistory(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Call<General<List<BonusRewardItem>>>

    // Customer level — Home (level-up check), Rewards, Load Wallet, Earning Wallet; replaces api/level
    // TODO(B2C-298): no response sample (502 "Your level is unavailable" on staging, 2026-11-03) — keeps the old
    //  RewardsLevelItem shape; verify once Dashboard sends levels
    @GET("${MOBILE}rewards/level")
    fun getCustomerLevel(
        @Header("Authorization") authorization: String
    ): Call<General<RewardsLevelItem?>>

    // Wallet withdrawal — replaces wallet/withdraw
    @POST("${MOBILE}wallet/withdrawals")
    fun createWalletWithdrawal(
        @Header("Authorization") authorization: String,
        @Body request: WalletWithdrawalRequest
    ): Call<General<WalletWithdrawalItem>>

    // ── Dashboard KYC ─────────────────────────────────────────────────────────

    @Multipart
    @POST("${AUTH}dashboard-kyc/initiate")
    fun initiateKyc(
        @Header("Authorization") authorization: String,
        @Part("entity_type") entityType: RequestBody
    ): Call<General<KycSubmissionDataItem>>

    @GET("${AUTH}dashboard-kyc/status")
    fun getKycStatus(
        @Header("Authorization") authorization: String
    ): Call<KycStatusItem>

    @Multipart
    @POST("${AUTH}dashboard-kyc/sections/a")
    fun submitKycSectionA(
        @Header("Authorization") authorization: String,
        @Part("has_gst") hasGst: RequestBody
    ): Call<General<KycSubmissionDataItem>>

    @Multipart
    @POST("${AUTH}dashboard-kyc/sections/b/signatory")
    fun submitKycSectionB(
        @Header("Authorization") authorization: String,
        @Part("email") email: RequestBody,
        @Part("mobile") mobile: RequestBody,
        @Part("pan_number") panNumber: RequestBody,
        @Part("aadhaar_number") aadhaarNumber: RequestBody,
        @Part panFile: MultipartBody.Part,
        @Part aadhaarFrontFile: MultipartBody.Part,
        @Part aadhaarBackFile: MultipartBody.Part,
        @Part passportPhotoFile: MultipartBody.Part
    ): Call<General<KycSignatoryDataItem>>

    @Multipart
    @POST("${AUTH}dashboard-kyc/sections/c")
    fun submitKycSectionC(
        @Header("Authorization") authorization: String,
        @Part parts: List<MultipartBody.Part>
    ): Call<General<KycSubmissionDataItem>>

    @POST("${AUTH}dashboard-kyc/agree")
    fun agreeKyc(
        @Header("Authorization") authorization: String,
        @Body body: RequestBody
    ): Call<General<KycAgreeDataItem>>

    @GET("${AUTH}dashboard-kyc/my-account")
    fun getKycMyAccount(
        @Header("Authorization") authorization: String
    ): Call<KycMyAccountItem>

    @Multipart
    @POST("${AUTH}dashboard-kyc/bank-account")
    fun editBankAccount(
        @Header("Authorization") authorization: String,
        @Part parts: List<MultipartBody.Part>
    ): Call<General<EditBankUpdateDataItem>>

    // ── HDFC Payment Gateway ──────────────────────────────────────────────────

    @POST("${AUTH}hdfc/orders")
    fun createHdfcOrder(
        @Header("Authorization") authorization: String,
        @Body request: HdfcCreateOrderRequest
    ): Call<General<HdfcOrderItem>>

    @GET("${AUTH}hdfc/orders/{order_id}/status")
    fun getHdfcOrderStatus(
        @Header("Authorization") authorization: String,
        @Path("order_id") orderId: String
    ): Call<General<HdfcOrderItem>>

    // ── Bill Pay (all categories share these) ─────────────────────────────────

    // Only categories with at least one active biller
    @GET("${MOBILE}billpay/categories")
    fun getBillPayCategories(
        @Header("Authorization") authorization: String
    ): Call<General<List<BillPayCategoryItem>>>

    @GET("${MOBILE}billpay/utility-billers")
    fun getBillPayBillers(
        @Header("Authorization") authorization: String,
        @Query("category") category: String
    ): Call<General<List<BillPayBillerItem>>>

    // Prepaid only
    @GET("${MOBILE}billpay/circles")
    fun getBillPayCircles(
        @Header("Authorization") authorization: String
    ): Call<General<List<BillPayCircleItem>>>

    // circle_code required for prepaid, null (left out) for DTH
    @GET("${MOBILE}billpay/utility-billers/{id}/plans")
    fun getBillPayPlans(
        @Header("Authorization") authorization: String,
        @Path("id") billerId: String,
        @Query("circle_code") circleCode: String?
    ): Call<General<List<BillPayPlanItem>>>

    @POST("${MOBILE}billpay/fetch-bill")
    fun fetchBillPayBill(
        @Header("Authorization") authorization: String,
        @Body body: BillPayFetchBillRequest
    ): Call<General<BillPayBillItem>>

    @GET("${MOBILE}billpay/fee-preview")
    fun getBillPayFeePreview(
        @Header("Authorization") authorization: String,
        @Query("amount") amount: String
    ): Call<General<BillPayFeePreviewItem>>

    // HTTP 201 — the only call that moves money; answers straight away with a status (same shape as billpay/status)
    @POST("${MOBILE}billpay/pay")
    fun payBillPay(
        @Header("Authorization") authorization: String,
        @Body body: BillPayPayRequest
    ): Call<General<BillPayTransactionItem>>

    @GET("${MOBILE}billpay/status/{client_reference}")
    fun getBillPayStatus(
        @Header("Authorization") authorization: String,
        @Path("client_reference") clientReference: String
    ): Call<General<BillPayTransactionItem>>

    // Bills and recharges only, newest first. status: optional server filter; per_page 1–50
    @GET("${MOBILE}transactions")
    fun getBillPayTransactions(
        @Header("Authorization") authorization: String,
        @Query("category") category: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("status") status: String?
    ): Call<BillPayTransactionPageItem>
}
