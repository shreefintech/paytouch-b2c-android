package com.shreefintech.paytouchconsumer.retrofit

import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.StateItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.CreateMpinRequest
import com.shreefintech.paytouchconsumer.retrofit.model.auth.ForgotCredentialRequest
import com.shreefintech.paytouchconsumer.retrofit.model.auth.LoginDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.LoginRequest
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MeDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MessageItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MpinItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.RegisterRequest
import com.shreefintech.paytouchconsumer.retrofit.model.auth.ResetCredentialRequest
import com.shreefintech.paytouchconsumer.retrofit.model.auth.ResetTokenDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.VerifyOtpRequest
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthLatestPaymentDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthPlansListItem
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthProcessPaymentRequest
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthTransactionReportRequest
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthTransactionStatusRequest
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityFetchBillResponseItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityProcessPaymentRequest
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityTransactionReportRequest
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityTransactionStatusRequest
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityVerifyPaymentDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.UnifiedTransactionItem
import com.shreefintech.paytouchconsumer.retrofit.model.fastag.FastagLatestPaymentDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.fastag.FastagOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.fastag.FastagPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.fastag.FastagProcessPaymentRequest
import com.shreefintech.paytouchconsumer.retrofit.model.fastag.FastagTransactionPageItem
import com.shreefintech.paytouchconsumer.retrofit.model.fastag.FastagTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.fastag.FastagTransactionStatusRequest
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasBillItem
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasProcessPaymentRequest
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasTransactionReportRequest
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasTransactionStatusRequest
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasVerifyPaymentDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycMyAccountItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanBillItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanLatestPaymentDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanOperatorsDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanProcessPaymentRequest
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanTransactionReportRequest
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanTransactionStatusRequest
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxFetchBillDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxLatestPaymentDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxProcessPaymentRequest
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxRecentPageItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxTransactionReportRequest
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxTransactionStatusRequest
import com.shreefintech.paytouchconsumer.retrofit.model.postpaid.PostpaidFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.postpaid.PostpaidFetchBillResponseItem
import com.shreefintech.paytouchconsumer.retrofit.model.postpaid.PostpaidLatestPaymentDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.postpaid.PostpaidOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.postpaid.PostpaidPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.postpaid.PostpaidProcessPaymentRequest
import com.shreefintech.paytouchconsumer.retrofit.model.postpaid.PostpaidTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.postpaid.PostpaidTransactionReportRequest
import com.shreefintech.paytouchconsumer.retrofit.model.postpaid.PostpaidTransactionStatusRequest
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidPlansListItem
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidProcessDirectRequest
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidTransactionDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidTransactionReportRequest
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidTransactionStatusRequest
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidVerifyPaymentDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.AccountInfoItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.ReferralInfoItem
import com.shreefintech.paytouchconsumer.retrofit.model.hdfc.HdfcCreateOrderRequest
import com.shreefintech.paytouchconsumer.retrofit.model.hdfc.HdfcOrderItem
import com.shreefintech.paytouchconsumer.retrofit.model.transactions.TransactionHistoryDetailItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletHistoryPageItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WithdrawDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WithdrawRequest
import com.shreefintech.paytouchconsumer.retrofit.model.WalletDataItem
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    companion object {
        const val CHANNEL = "mobile/"
    }

    // ── Session ───────────────────────────────────────────────────────────────

    @GET("${CHANNEL}auth/me")
    fun getMe(
        @Header("Authorization") authorization: String
    ): Call<General<MeDataItem>>

    @POST("${CHANNEL}auth/logout")
    fun logout(
        @Header("Authorization") authorization: String
    ): Call<MessageItem>

    // ── Authentication ────────────────────────────────────────────────────────

    @POST("${CHANNEL}auth/login")
    fun login(
        @Body request: LoginRequest
    ): Call<General<LoginDataItem>>

    @POST("${CHANNEL}auth/register")
    fun register(
        @Body request: RegisterRequest
    ): Call<General<LoginDataItem>>

    // ── Forgot Credential (unified OTP flow) ──────────────────────────────────

    @POST("${CHANNEL}auth/forgot-credential")
    fun sendForgotCredentialOtp(
        @Body request: ForgotCredentialRequest
    ): Call<MessageItem>

    @POST("${CHANNEL}auth/forgot-credential/verify-otp")
    fun verifyForgotCredentialOtp(
        @Body request: VerifyOtpRequest
    ): Call<General<ResetTokenDataItem>>

    @POST("${CHANNEL}auth/forgot-credential/reset")
    fun resetCredential(
        @Body request: ResetCredentialRequest
    ): Call<MessageItem>

    // TODO(B2C-147): createMpin endpoint path unconfirmed — update when backend finalises
    @POST("${CHANNEL}auth/mpin/create")
    fun createMpin(
        @Header("Authorization") token: String,
        @Body body: CreateMpinRequest
    ): Call<MpinItem>

    // ── Wallet ────────────────────────────────────────────────────────────────

    @GET("${CHANNEL}wallet/user-data")
    fun getUserWalletData(
        @Header("Authorization") authorization: String
    ): Call<General<WalletDataItem>>

    @GET("${CHANNEL}wallet/combined-wallet-history")
    fun getWalletHistory(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Call<General<WalletHistoryPageItem>>

    @POST("${CHANNEL}wallet/withdraw")
    fun withdrawWallet(
        @Header("Authorization") authorization: String,
        @Body request: WithdrawRequest
    ): Call<General<WithdrawDataItem>>

    // ── Transaction Detail ────────────────────────────────────────────────────

    @GET("${CHANNEL}utility-bill/transaction/{id}")
    fun getTransactionHistoryDetail(
        @Header("Authorization") authorization: String,
        @Path("id") transactionId: String
    ): Call<General<TransactionHistoryDetailItem>>

    // ── Electricity ───────────────────────────────────────────────────────────

    @GET("${CHANNEL}electricity/operators")
    fun getElectricityOperators(
        @Header("Authorization") authorization: String
    ): Call<General<List<ElectricityOperatorItem>>>

    @POST("${CHANNEL}electricity/fetch-bill")
    fun fetchElectricityBill(
        @Header("Authorization") authorization: String,
        @Body request: ElectricityFetchBillRequest
    ): Call<ElectricityFetchBillResponseItem>

    @POST("${CHANNEL}electricity/process-payment")
    fun processElectricityPayment(
        @Header("Authorization") authorization: String,
        @Body request: ElectricityProcessPaymentRequest
    ): Call<ElectricityPaymentItem>

    @POST("${CHANNEL}electricity/payment-report")
    fun getElectricityPaymentReport(
        @Header("Authorization") authorization: String,
        @Body request: ElectricityTransactionReportRequest
    ): Call<General<List<ElectricityTransactionReportDataItem>>>

    @POST("${CHANNEL}electricity/transaction-status")
    fun getElectricityTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: ElectricityTransactionStatusRequest
    ): Call<General<List<ElectricityTransactionReportDataItem>>>

    @GET("${CHANNEL}electricity/latest-payment")
    fun getElectricityLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<ElectricityVerifyPaymentDataItem>>

    // ── Gas ───────────────────────────────────────────────────────────────────

    @GET("${CHANNEL}gas/operators")
    fun getGasOperators(
        @Header("Authorization") authorization: String
    ): Call<General<List<GasOperatorItem>>>

    @POST("${CHANNEL}gas/fetch-bill")
    fun fetchGasBill(
        @Header("Authorization") authorization: String,
        @Body request: GasFetchBillRequest
    ): Call<General<GasBillItem>>

    @POST("${CHANNEL}gas/process-payment")
    fun processGasPayment(
        @Header("Authorization") authorization: String,
        @Body request: GasProcessPaymentRequest
    ): Call<GasPaymentItem>

    @POST("${CHANNEL}gas/transaction-status")
    fun getGasTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: GasTransactionStatusRequest
    ): Call<General<List<GasTransactionReportDataItem>>>

    @POST("${CHANNEL}gas/payment-report")
    fun getGasPaymentReport(
        @Header("Authorization") authorization: String,
        @Body request: GasTransactionReportRequest
    ): Call<General<List<GasTransactionReportDataItem>>>

    @GET("${CHANNEL}gas/latest-payment")
    fun getGasLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<GasVerifyPaymentDataItem>>

    // ── General ───────────────────────────────────────────────────────────────

    @GET("${CHANNEL}states")
    fun getStates(
        @Header("Authorization") authorization: String
    ): Call<General<List<StateItem>>>

    // ── Mobile Prepaid ────────────────────────────────────────────────────────

    @GET("${CHANNEL}recharge/operators")
    fun getPrepaidOperators(
        @Header("Authorization") authorization: String
    ): Call<General<List<PrepaidOperatorItem>>>

    @GET("${CHANNEL}recharge/plans/{operatorId}/{circleId}")
    fun getPrepaidPlans(
        @Header("Authorization") authorization: String,
        @Path("operatorId") operatorId: String,
        @Path("circleId") circleId: String
    ): Call<PrepaidPlansListItem>

    @POST("${CHANNEL}recharge/process-direct")
    fun processPrepaidPayment(
        @Header("Authorization") authorization: String,
        @Body request: PrepaidProcessDirectRequest
    ): Call<PrepaidPaymentItem>

    @POST("${CHANNEL}mobile-recharge/transaction-status")
    fun getPrepaidTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: PrepaidTransactionStatusRequest
    ): Call<General<List<PrepaidTransactionDataItem>>>

    @POST("${CHANNEL}utility/payment-report")
    fun getPrepaidPaymentReport(
        @Header("Authorization") authorization: String,
        @Body request: PrepaidTransactionReportRequest
    ): Call<General<List<PrepaidTransactionDataItem>>>

    @GET("${CHANNEL}recharge/latest-payment")
    fun getPrepaidLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<PrepaidVerifyPaymentDataItem>>

    // ── Mobile Postpaid ───────────────────────────────────────────────────────

    @GET("${CHANNEL}mobile-postpaid/operators")
    fun getPostpaidOperators(
        @Header("Authorization") authorization: String
    ): Call<General<List<PostpaidOperatorItem>>>

    @POST("${CHANNEL}mobile-postpaid/fetch-bill")
    fun fetchPostpaidBill(
        @Header("Authorization") authorization: String,
        @Body request: PostpaidFetchBillRequest
    ): Call<PostpaidFetchBillResponseItem>

    @POST("${CHANNEL}mobile-postpaid/process-payment")
    fun processPostpaidPayment(
        @Header("Authorization") authorization: String,
        @Body request: PostpaidProcessPaymentRequest
    ): Call<PostpaidPaymentItem>

    @POST("${CHANNEL}mobile-postpaid/transaction-status")
    fun getPostpaidTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: PostpaidTransactionStatusRequest
    ): Call<General<List<PostpaidTransactionReportDataItem>>>

    @POST("${CHANNEL}mobile-postpaid/payment-report")
    fun getPostpaidPaymentReport(
        @Header("Authorization") authorization: String,
        @Body request: PostpaidTransactionReportRequest
    ): Call<General<List<PostpaidTransactionReportDataItem>>>

    @GET("${CHANNEL}mobile-postpaid/latest-payment")
    fun getPostpaidLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<PostpaidLatestPaymentDataItem>>

    // ── DTH ───────────────────────────────────────────────────────────────────

    @GET("${CHANNEL}dth/operators")
    fun getDthOperators(
        @Header("Authorization") authorization: String
    ): Call<General<List<DthOperatorItem>>>

    @GET("${CHANNEL}dth/plans/{operatorId}")
    fun getDthPlans(
        @Header("Authorization") authorization: String,
        @Path("operatorId") operatorId: String
    ): Call<DthPlansListItem>

    @POST("${CHANNEL}dth/process-direct")
    fun processDthPayment(
        @Header("Authorization") authorization: String,
        @Body request: DthProcessPaymentRequest
    ): Call<DthPaymentItem>

    @POST("${CHANNEL}dth/transaction/status")
    fun getDthTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: DthTransactionStatusRequest
    ): Call<General<List<DthTransactionReportDataItem>>>

    @POST("${CHANNEL}dth/payment-report")
    fun getDthPaymentReport(
        @Header("Authorization") authorization: String,
        @Body request: DthTransactionReportRequest
    ): Call<General<List<DthTransactionReportDataItem>>>

    @GET("${CHANNEL}dth/latest-payment")
    fun getDthLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<DthLatestPaymentDataItem>>

    // ── FASTag ────────────────────────────────────────────────────────────────

    @GET("${CHANNEL}fastag/operators")
    fun getFastagOperators(
        @Header("Authorization") authorization: String
    ): Call<General<List<FastagOperatorItem>>>

    @POST("${CHANNEL}fastag")
    fun processFastagPayment(
        @Header("Authorization") authorization: String,
        @Body request: FastagProcessPaymentRequest
    ): Call<FastagPaymentItem>

    @POST("${CHANNEL}fastag/transaction/status")
    fun getFastagTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: FastagTransactionStatusRequest
    ): Call<General<List<FastagTransactionReportDataItem>>>

    @GET("${CHANNEL}fastag")
    fun getFastagPaymentReport(
        @Header("Authorization") authorization: String,
        @Query("from_date") fromDate: String?,
        @Query("to_date") toDate: String?,
        @Query("status") status: String?,
        @Query("vehicle_number") vehicleNumber: String?,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Call<General<FastagTransactionPageItem>>

    @GET("${CHANNEL}fastag/latest-payment")
    fun getFastagLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<FastagLatestPaymentDataItem>>

    // ── Loan ──────────────────────────────────────────────────────────────────

    @GET("${CHANNEL}loanrepayment/operators")
    fun getLoanOperators(
        @Header("Authorization") authorization: String
    ): Call<General<LoanOperatorsDataItem>>

    @POST("${CHANNEL}loanrepayment/fetch-bill")
    fun fetchLoanBill(
        @Header("Authorization") authorization: String,
        @Body request: LoanFetchBillRequest
    ): Call<General<List<LoanBillItem>>>

    @POST("${CHANNEL}loanrepayment/process-payment")
    fun processLoanPayment(
        @Header("Authorization") authorization: String,
        @Body request: LoanProcessPaymentRequest
    ): Call<LoanPaymentItem>

    @POST("${CHANNEL}loanrepayment/transaction-status")
    fun getLoanTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: LoanTransactionStatusRequest
    ): Call<General<List<LoanTransactionReportDataItem>>>

    @POST("${CHANNEL}loanrepayment/payment-report")
    fun getLoanPaymentReport(
        @Header("Authorization") authorization: String,
        @Body request: LoanTransactionReportRequest
    ): Call<General<List<LoanTransactionReportDataItem>>>

    @GET("${CHANNEL}loanrepayment/latest-payment")
    fun getLoanLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<LoanLatestPaymentDataItem>>

    // ── Municipal Tax ─────────────────────────────────────────────────────────

    @GET("${CHANNEL}municipal-taxes/operators")
    fun getMunicipalTaxOperators(
        @Header("Authorization") authorization: String
    ): Call<General<List<MunicipalTaxOperatorItem>>>

    @POST("${CHANNEL}municipal-taxes/fetch-bill")
    fun fetchMunicipalTaxBill(
        @Header("Authorization") authorization: String,
        @Body request: MunicipalTaxFetchBillRequest
    ): Call<General<List<MunicipalTaxFetchBillDataItem>>>

    @POST("${CHANNEL}municipal-taxes/process-payment")
    fun processMunicipalTaxPayment(
        @Header("Authorization") authorization: String,
        @Body request: MunicipalTaxProcessPaymentRequest
    ): Call<MunicipalTaxPaymentItem>

    // Confirmed backend quirk: municipal tax transaction-status routes through mobile-recharge endpoint
    @POST("${CHANNEL}mobile-recharge/transaction-status")
    fun getMunicipalTaxTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: MunicipalTaxTransactionStatusRequest
    ): Call<General<List<MunicipalTaxTransactionReportDataItem>>>

    @POST("${CHANNEL}municipal-taxes/payment-report")
    fun getMunicipalTaxPaymentReport(
        @Header("Authorization") authorization: String,
        @Body request: MunicipalTaxTransactionReportRequest
    ): Call<General<List<MunicipalTaxTransactionReportDataItem>>>

    @GET("${CHANNEL}municipal-taxes/latest-payment")
    fun getMunicipalTaxLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<MunicipalTaxLatestPaymentDataItem>>

    @GET("${CHANNEL}municipal-taxes/recent-transactions")
    fun getMunicipalTaxRecentTransactions(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Call<MunicipalTaxRecentPageItem>

    // ── My Account ───────────────────────────────────────────────────────────

    @GET("${CHANNEL}dashboard-kyc/account-overview")
    fun getAccountOverview(
        @Header("Authorization") authorization: String
    ): Call<AccountInfoItem>

    @GET("${CHANNEL}referral-info")
    fun getReferralInfo(
        @Header("Authorization") authorization: String
    ): Call<ReferralInfoItem>

    // ── KYC ───────────────────────────────────────────────────────────────────

    @POST("${CHANNEL}kyc/initiate")
    fun initiateKyc(
        @Header("Authorization") authorization: String
    ): Call<General<KycDataItem>>

    @GET("${CHANNEL}kyc/status")
    fun getKycStatus(
        @Header("Authorization") authorization: String
    ): Call<General<KycDataItem>>

    @Multipart
    @POST("${CHANNEL}kyc/sections/a")
    fun submitKycSectionA(
        @Header("Authorization") authorization: String,
        @Part("has_gst") hasGst: RequestBody
    ): Call<General<KycDataItem>>

    @Multipart
    @POST("${CHANNEL}kyc/sections/b/signatory")
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
    ): Call<General<KycDataItem>>

    @Multipart
    @POST("${CHANNEL}kyc/sections/c")
    fun submitKycSectionC(
        @Header("Authorization") authorization: String,
        @Part parts: List<MultipartBody.Part>
    ): Call<General<KycDataItem>>

    @POST("${CHANNEL}kyc/agree")
    fun agreeKyc(
        @Header("Authorization") authorization: String
    ): Call<General<KycDataItem>>

    @GET("${CHANNEL}kyc/my-account")
    fun getKycMyAccount(
        @Header("Authorization") authorization: String
    ): Call<KycMyAccountItem>

    // ── HDFC Payment Gateway ──────────────────────────────────────────────────

    @POST("${CHANNEL}hdfc/orders")
    fun createHdfcOrder(
        @Header("Authorization") authorization: String,
        @Body request: HdfcCreateOrderRequest
    ): Call<General<HdfcOrderItem>>

    @GET("${CHANNEL}hdfc/orders/{order_id}/status")
    fun getHdfcOrderStatus(
        @Header("Authorization") authorization: String,
        @Path("order_id") orderId: String
    ): Call<General<HdfcOrderItem>>

    // ── Unified Transactions ──────────────────────────────────────────────────

    @GET("${CHANNEL}transactions")
    fun getTransactions(
        @Header("Authorization") authorization: String,
        @Query("type") type: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Call<General<List<UnifiedTransactionItem>>>
}
