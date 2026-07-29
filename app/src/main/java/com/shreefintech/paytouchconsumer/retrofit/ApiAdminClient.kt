package com.shreefintech.paytouchconsumer.retrofit

import com.shreefintech.paytouchconsumer.Constant
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiAdminClient {

    val apiService: ApiAdminService by lazy {
        Retrofit.Builder()
            .baseUrl(Constant.BASE_URL_ADMIN)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiAdminService::class.java)
    }
}
