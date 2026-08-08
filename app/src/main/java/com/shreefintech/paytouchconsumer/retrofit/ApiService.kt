package com.shreefintech.paytouchconsumer.retrofit

import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.UserProfileItem
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycSubmissionDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.WalletDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletHistoryPageItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.LoginItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MessageItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.RegisterItem
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthLatestPaymentDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthPlansListItem
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthProcessPaymentRequest
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthTransactionReportRequest
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthTransactionStatusRequest
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityBillItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityFetchBillRequest
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
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanBillItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanLatestPaymentDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanOperatorsDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanProcessPaymentRequest
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanTransactionReportRequest
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanTransactionStatusRequest
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasProcessPaymentRequest
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasTransactionReportRequest
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasTransactionStatusRequest
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasVerifyPaymentDataItem
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
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxFetchBillDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxLatestPaymentDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxProcessPaymentRequest
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxRecentDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxRecentPageItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxTransactionReportRequest
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxTransactionStatusRequest
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidVerifyPaymentDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.AccountInfoItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.ReferralInfoItem
import com.shreefintech.paytouchconsumer.retrofit.model.hdfc.HdfcCreateOrderRequest
import com.shreefintech.paytouchconsumer.retrofit.model.hdfc.HdfcOrderResponseItem
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    companion object {
        const val AUTH = "api/"
    }

    // ── Session ───────────────────────────────────────────────────────────────

    @GET("${AUTH}user")
    fun getUser(
        @Header("Authorization") authorization: String
    ): Call<UserProfileItem>

    // ── Authentication ────────────────────────────────────────────────────────

    @FormUrlEncoded
    @POST("${AUTH}login")
    fun loginWithPassword(
        @Field("mobile") mobile: String,
        @Field("password") password: String
    ): Call<LoginItem>

    @FormUrlEncoded
    @POST("${AUTH}login")
    fun loginWithMpin(
        @Field("mobile") mobile: String,
        @Field("mpin") mpin: String
    ): Call<LoginItem>

    @FormUrlEncoded
    @POST("${AUTH}register")
    fun register(
        @Field("mobile") mobile: String,
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("password_confirmation") passwordConfirmation: String,
        @Field("referral_code") referralCode: String
    ): Call<RegisterItem>

    // ── Forgot Password OTP flow ──────────────────────────────────────────────

    @FormUrlEncoded
    @POST("${AUTH}password/send-otp")
    fun sendPasswordOtp(
        @Field("mobile") mobile: String
    ): Call<MessageItem>

    @FormUrlEncoded
    @POST("${AUTH}password/verify-otp")
    fun verifyPasswordOtp(
        @Field("mobile") mobile: String,
        @Field("otp") otp: String
    ): Call<MessageItem>

    @FormUrlEncoded
    @POST("${AUTH}password/reset")
    fun resetPassword(
        @Field("mobile") mobile: String,
        @Field("new_password") newPassword: String,
        @Field("new_password_confirmation") newPasswordConfirmation: String
    ): Call<MessageItem>

    // ── Forgot MPIN OTP flow ──────────────────────────────────────────────────

    @FormUrlEncoded
    @POST("${AUTH}mpin/send-otp")
    fun sendMpinOtp(
        @Field("mobile") mobile: String
    ): Call<MessageItem>

    @FormUrlEncoded
    @POST("${AUTH}mpin/verify-otp")
    fun verifyMpinOtp(
        @Field("mobile") mobile: String,
        @Field("otp") otp: String
    ): Call<MessageItem>

    @FormUrlEncoded
    @POST("${AUTH}mpin/reset")
    fun resetMpin(
        @Field("mobile") mobile: String,
        @Field("new_mpin") newMpin: String,
        @Field("new_mpin_confirmation") newMpinConfirmation: String
    ): Call<MessageItem>

    // ── Wallet ────────────────────────────────────────────────────────────────

    @GET("${AUTH}wallet/user-data")
    fun getUserWalletData(
        @Header("Authorization") authorization: String
    ): Call<General<WalletDataItem>>

    @GET("${AUTH}wallet/combined-wallet-history")
    fun getWalletHistory(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Call<General<WalletHistoryPageItem>>

    // ── Electricity ───────────────────────────────────────────────────────────

    @GET("${AUTH}electricity/operators")
    fun getElectricityOperators(
        @Header("Authorization") authorization: String
    ): Call<General<List<ElectricityOperatorItem>>>

    @POST("${AUTH}electricity/fetch-bill")
    fun fetchElectricityBill(
        @Header("Authorization") authorization: String,
        @Body request: ElectricityFetchBillRequest
    ): Call<General<List<ElectricityBillItem>>>

    @POST("${AUTH}electricity/process-payment")
    fun processElectricityPayment(
        @Header("Authorization") authorization: String,
        @Body request: ElectricityProcessPaymentRequest
    ): Call<ElectricityPaymentItem>

    @POST("${AUTH}electricity/payment-report")
    fun getElectricityPaymentReport(
        @Header("Authorization") authorization: String,
        @Body request: ElectricityTransactionReportRequest
    ): Call<General<List<ElectricityTransactionReportDataItem>>>

    @POST("${AUTH}electricity/transaction-status")
    fun getElectricityTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: ElectricityTransactionStatusRequest
    ): Call<General<List<ElectricityTransactionReportDataItem>>>

    @GET("${AUTH}electricity/latest-payment")
    fun getElectricityLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<ElectricityVerifyPaymentDataItem>>

    // ── Gas ───────────────────────────────────────────────────────────────────

    @GET("${AUTH}gas/operators")
    fun getGasOperators(
        @Header("Authorization") authorization: String
    ): Call<General<List<GasOperatorItem>>>

    @POST("${AUTH}gas/fetch-bill")
    fun fetchGasBill(
        @Header("Authorization") authorization: String,
        @Body request: GasFetchBillRequest
    ): Call<General<List<GasBillItem>>>

    @POST("${AUTH}gas/process-payment")
    fun processGasPayment(
        @Header("Authorization") authorization: String,
        @Body request: GasProcessPaymentRequest
    ): Call<GasPaymentItem>

    @POST("${AUTH}gas/transaction-status")
    fun getGasTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: GasTransactionStatusRequest
    ): Call<General<List<GasTransactionReportDataItem>>>

    @POST("${AUTH}gas/payment-report")
    fun getGasPaymentReport(
        @Header("Authorization") authorization: String,
        @Body request: GasTransactionReportRequest
    ): Call<General<List<GasTransactionReportDataItem>>>

    @GET("${AUTH}gas/latest-payment")
    fun getGasLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<GasVerifyPaymentDataItem>>

    // ── Mobile Prepaid ────────────────────────────────────────────────────────

    @GET("${AUTH}recharge/operators")
    fun getPrepaidOperators(
        @Header("Authorization") authorization: String
    ): Call<General<List<PrepaidOperatorItem>>>

    @GET("${AUTH}recharge/plans/{operatorId}/{circleId}")
    fun getPrepaidPlans(
        @Header("Authorization") authorization: String,
        @Path("operatorId") operatorId: String,
        @Path("circleId") circleId: String
    ): Call<PrepaidPlansListItem>

    @POST("${AUTH}recharge/process-direct")
    fun processPrepaidPayment(
        @Header("Authorization") authorization: String,
        @Body request: PrepaidProcessDirectRequest
    ): Call<PrepaidPaymentItem>

    @POST("${AUTH}mobile-recharge/transaction-status")
    fun getPrepaidTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: PrepaidTransactionStatusRequest
    ): Call<General<List<PrepaidTransactionDataItem>>>

    @POST("${AUTH}utility/payment-report")
    fun getPrepaidPaymentReport(
        @Header("Authorization") authorization: String,
        @Body request: PrepaidTransactionReportRequest
    ): Call<General<List<PrepaidTransactionDataItem>>>

    @GET("${AUTH}recharge/latest-payment")
    fun getPrepaidLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<PrepaidVerifyPaymentDataItem>>

    // ── Mobile Postpaid ───────────────────────────────────────────────────────

    @GET("${AUTH}mobile-postpaid/operators")
    fun getPostpaidOperators(
        @Header("Authorization") authorization: String
    ): Call<General<List<PostpaidOperatorItem>>>

    @POST("${AUTH}mobile-postpaid/process-payment")
    fun processPostpaidPayment(
        @Header("Authorization") authorization: String,
        @Body request: PostpaidProcessPaymentRequest
    ): Call<PostpaidPaymentItem>

    @POST("${AUTH}mobile-postpaid/transaction-status")
    fun getPostpaidTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: PostpaidTransactionStatusRequest
    ): Call<General<List<PostpaidTransactionReportDataItem>>>

    @POST("${AUTH}mobile-postpaid/payment-report")
    fun getPostpaidPaymentReport(
        @Header("Authorization") authorization: String,
        @Body request: PostpaidTransactionReportRequest
    ): Call<General<List<PostpaidTransactionReportDataItem>>>

    @GET("${AUTH}mobile-postpaid/latest-payment")
    fun getPostpaidLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<PostpaidLatestPaymentDataItem>>

    // ── DTH ───────────────────────────────────────────────────────────────────

    @GET("${AUTH}dth/operators")
    fun getDthOperators(
        @Header("Authorization") authorization: String
    ): Call<General<List<DthOperatorItem>>>

    @GET("${AUTH}dth/plans/{operatorId}")
    fun getDthPlans(
        @Header("Authorization") authorization: String,
        @Path("operatorId") operatorId: String
    ): Call<DthPlansListItem>

    @POST("${AUTH}dth/process-direct")
    fun processDthPayment(
        @Header("Authorization") authorization: String,
        @Body request: DthProcessPaymentRequest
    ): Call<DthPaymentItem>

    @POST("${AUTH}dth/transaction/status")
    fun getDthTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: DthTransactionStatusRequest
    ): Call<General<List<DthTransactionReportDataItem>>>

    @POST("${AUTH}dth/payment-report")
    fun getDthPaymentReport(
        @Header("Authorization") authorization: String,
        @Body request: DthTransactionReportRequest
    ): Call<General<List<DthTransactionReportDataItem>>>

    @GET("${AUTH}dth/latest-payment")
    fun getDthLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<DthLatestPaymentDataItem>>

    // ── FASTag ────────────────────────────────────────────────────────────────

    @GET("${AUTH}fastag/operators")
    fun getFastagOperators(
        @Header("Authorization") authorization: String
    ): Call<General<List<FastagOperatorItem>>>

    @POST("${AUTH}fastag")
    fun processFastagPayment(
        @Header("Authorization") authorization: String,
        @Body request: FastagProcessPaymentRequest
    ): Call<FastagPaymentItem>

    @POST("${AUTH}fastag/transaction/status")
    fun getFastagTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: FastagTransactionStatusRequest
    ): Call<General<List<FastagTransactionReportDataItem>>>

        @GET("${AUTH}fastag")
        fun getFastagPaymentReport(
            @Header("Authorization") authorization: String,
            @Query("from_date")      fromDate:      String?,
            @Query("to_date")        toDate:        String?,
            @Query("status")         status:        String?,
            @Query("vehicle_number") vehicleNumber: String?,
            @Query("page")           page:          Int,
            @Query("per_page")       perPage:       Int
        ): Call<General<FastagTransactionPageItem>>

        @GET("${AUTH}fastag/latest-payment")
        fun getFastagLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<FastagLatestPaymentDataItem>>

    // ── Loan ──────────────────────────────────────────────────────────────────

    @GET("${AUTH}loanrepayment/operators")
    fun getLoanOperators(
        @Header("Authorization") authorization: String
    ): Call<General<LoanOperatorsDataItem>>

    @POST("${AUTH}loanrepayment/fetch-bill")
    fun fetchLoanBill(
        @Header("Authorization") authorization: String,
        @Body request: LoanFetchBillRequest
    ): Call<General<List<LoanBillItem>>>

    @POST("${AUTH}loanrepayment/process-payment")
    fun processLoanPayment(
        @Header("Authorization") authorization: String,
        @Body request: LoanProcessPaymentRequest
    ): Call<LoanPaymentItem>

    @POST("${AUTH}loanrepayment/transaction-status")
    fun getLoanTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: LoanTransactionStatusRequest
    ): Call<General<List<LoanTransactionReportDataItem>>>

    @POST("${AUTH}loanrepayment/payment-report")
    fun getLoanPaymentReport(
        @Header("Authorization") authorization: String,
        @Body request: LoanTransactionReportRequest
    ): Call<General<List<LoanTransactionReportDataItem>>>

    @GET("${AUTH}loanrepayment/latest-payment")
    fun getLoanLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<LoanLatestPaymentDataItem>>

    // ── Municipal Tax ─────────────────────────────────────────────────────────

    @GET("${AUTH}municipal-taxes/operators")
    fun getMunicipalTaxOperators(
        @Header("Authorization") authorization: String
    ): Call<General<List<MunicipalTaxOperatorItem>>>

    @POST("${AUTH}municipal-taxes/fetch-bill")
    fun fetchMunicipalTaxBill(
        @Header("Authorization") authorization: String,
        @Body request: MunicipalTaxFetchBillRequest
    ): Call<General<List<MunicipalTaxFetchBillDataItem>>>

    @POST("${AUTH}municipal-taxes/process-payment")
    fun processMunicipalTaxPayment(
        @Header("Authorization") authorization: String,
        @Body request: MunicipalTaxProcessPaymentRequest
    ): Call<MunicipalTaxPaymentItem>

    @POST("${AUTH}mobile-recharge/transaction-status")
    fun getMunicipalTaxTransactionStatus(
        @Header("Authorization") authorization: String,
        @Body request: MunicipalTaxTransactionStatusRequest
    ): Call<General<List<MunicipalTaxTransactionReportDataItem>>>

    @POST("${AUTH}municipal-taxes/payment-report")
    fun getMunicipalTaxPaymentReport(
        @Header("Authorization") authorization: String,
        @Body request: MunicipalTaxTransactionReportRequest
    ): Call<General<List<MunicipalTaxTransactionReportDataItem>>>

    @GET("${AUTH}municipal-taxes/latest-payment")
    fun getMunicipalTaxLatestPayment(
        @Header("Authorization") authorization: String
    ): Call<General<MunicipalTaxLatestPaymentDataItem>>

    @GET("${AUTH}municipal-taxes/recent-transactions")
    fun getMunicipalTaxRecentTransactions(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Call<MunicipalTaxRecentPageItem>

    // ── My Account ───────────────────────────────────────────────────────────

    @GET("${AUTH}kyc/account-info")
    fun getKycAccountInfo(
        @Header("Authorization") authorization: String,
        @Query("id") id: String
    ): Call<AccountInfoItem>

    @GET("${AUTH}referral-info")
    fun getReferralInfo(
        @Header("Authorization") authorization: String
    ): Call<ReferralInfoItem>

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
    ): Call<General<KycSubmissionDataItem>>

    @Multipart
    @POST("${AUTH}dashboard-kyc/sections/a")
    fun submitKycSectionA(
        @Header("Authorization") authorization: String,
        @Part("has_gst") hasGst: RequestBody,
        @Part("documents[0][document_type]") documentType: RequestBody,
        @Part document: MultipartBody.Part
    ): Call<General<KycSubmissionDataItem>>

    // ── HDFC Payment Gateway ──────────────────────────────────────────────────

    @POST("${AUTH}hdfc/orders")
    fun createHdfcOrder(
        @Header("Authorization") authorization: String,
        @Body request: HdfcCreateOrderRequest
    ): Call<HdfcOrderResponseItem>

    @GET("${AUTH}hdfc/orders/{order_id}/status")
    fun getHdfcOrderStatus(
        @Header("Authorization") authorization: String,
        @Path("order_id") orderId: String
    ): Call<HdfcOrderResponseItem>

    // ── Unified Transactions ──────────────────────────────────────────────────

    @GET("${AUTH}transactions")
    fun getTransactions(
        @Header("Authorization") authorization: String,
        @Query("type") type: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Call<General<List<UnifiedTransactionItem>>>
}
