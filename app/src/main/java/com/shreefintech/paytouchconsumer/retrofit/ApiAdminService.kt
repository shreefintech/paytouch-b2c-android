package com.shreefintech.paytouchconsumer.retrofit

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiAdminService {

    @FormUrlEncoded
    @POST("users.php")
    fun registerUser(
        @Field("id")            id: Int,
        @Field("username")      username: String,
        @Field("email")         email: String,
        @Field("mobile")        mobile: String,
        @Field("referral_code") referralCode: String
    ): Call<Any>
}
