package com.shreefintech.paytouchconsumer.retrofit

import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.LoginItem
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    companion object {
        const val AUTH   = "api/"
        const val CLIENT = "api/mobile/client/"
    }

    // ── Authentication ────────────────────────────────────────────────────────

    @POST("${AUTH}login")
    suspend fun login(
        @Body body: RequestBody
    ): Response<General<LoginItem?>>
}
