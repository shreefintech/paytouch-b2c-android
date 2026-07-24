package com.shreefintech.paytouchconsumer.retrofit

import com.shreefintech.paytouchconsumer.retrofit.model.auth.LoginItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MessageItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.RegisterItem
import com.shreefintech.paytouchconsumer.retrofit.model.UserProfileItem
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

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
}
