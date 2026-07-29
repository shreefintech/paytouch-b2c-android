package com.shreefintech.paytouchconsumer.retrofit

import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.UserProfileItem
import com.shreefintech.paytouchconsumer.retrofit.model.WalletDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.LoginItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MessageItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.RegisterItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityBillItem
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasBillItem
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasProcessPaymentRequest
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasTransactionReportRequest
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasTransactionStatusRequest
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityProcessPaymentRequest
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityTransactionReportRequest
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityTransactionStatusRequest
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityVerifyPaymentDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityVerifyPaymentRequest
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.UnifiedTransactionItem
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
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

    @POST("${AUTH}electricity/verify-payment")
    fun verifyElectricityPayment(
        @Header("Authorization") authorization: String,
        @Body request: ElectricityVerifyPaymentRequest
    ): Call<General<ElectricityVerifyPaymentDataItem>>

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

    // ── Unified Transactions ──────────────────────────────────────────────────

    @GET("${AUTH}transactions")
    fun getTransactions(
        @Header("Authorization") authorization: String,
        @Query("type") type: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Call<General<List<UnifiedTransactionItem>>>
}
