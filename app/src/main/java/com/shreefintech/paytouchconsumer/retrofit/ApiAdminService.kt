package com.shreefintech.paytouchconsumer.retrofit

import com.shreefintech.paytouchconsumer.retrofit.model.VpsBalanceItem
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiAdminService {

    @GET("balance.php")
    fun getVpsBalance(
        @Query("id") userId: String
    ): Call<VpsBalanceItem>

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
