package com.shreefintech.paytouchconsumer.retrofit

import com.shreefintech.paytouchconsumer.retrofit.model.CityItem
import com.shreefintech.paytouchconsumer.retrofit.model.General
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

/** Auth + onboarding endpoints on the B2C backend ([com.shreefintech.paytouchconsumer.Constant.BASE_URL_AUTH]). */
interface ApiAuthService {

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
        @Header("Authorization") authorization: String,
        @Body body: LogoutRequest
    ): Call<MessageItem>

    // ── Push Notifications ────────────────────────────────────────────────────

    @POST("${CHANNEL}device-token")
    fun registerDeviceToken(
        @Header("Authorization") authorization: String?,
        @Body body: DeviceTokenRequest
    ): Call<MessageItem>

    // ── Location ──────────────────────────────────────────────────────────────

    @POST("${CHANNEL}location")
    fun sendLocation(
        @Header("Authorization") authorization: String,
        @Body body: UserLocationRequest
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

    // Same response shape as auth/me; 422 if an MPIN is already set
    @POST("${CHANNEL}auth/mpin")
    fun createMpin(
        @Header("Authorization") token: String,
        @Body body: CreateMpinRequest
    ): Call<General<MeDataItem>>

    // ── B2C V2 Registration & Onboarding ─────────────────────────────────────

    @Multipart
    @POST("${CHANNEL}auth/register/individual")
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
    @POST("${CHANNEL}kyc/individual/resubmit")
    fun reuploadAadhaar(
        @Header("Authorization") authorization: String,
        @Part("aadhaar_number") aadhaarNumber: RequestBody,
        @Part aadhaarPhoto: MultipartBody.Part
    ): Call<MessageItem>

    @GET("${CHANNEL}address/states")
    fun getAddressStates(): Call<General<List<StateItem>>>

    @GET("${CHANNEL}address/states/{state_id}/cities")
    fun getAddressCities(
        @Path("state_id") stateId: String
    ): Call<General<List<CityItem>>>

    @POST("${CHANNEL}kyc/individual/bank-details")
    fun addBankDetails(
        @Header("Authorization") authorization: String,
        @Body body: BankDetailsV2Request
    ): Call<BankDetailsV2Item>

    @POST("${CHANNEL}kyc/individual/pan")
    fun addPan(
        @Header("Authorization") authorization: String,
        @Body body: PanV2Request
    ): Call<PanV2Item>
}
