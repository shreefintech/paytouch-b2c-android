package com.shreefintech.paytouchconsumer.kyc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycDataItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class KycViewModel(application: Application) : AndroidViewModel(application) {

    private val textMediaType = "text/plain".toMediaTypeOrNull()

    /**
     * Entry point for the KYC hub. Flow is driven by current_section from /kyc/status:
     *  - entity_type null (never started) → initiate → section A placeholder → onReady
     *  - current_section = "a"            → section A placeholder → onReady
     *  - current_section = "b" / "c"      → onReady directly
     *  - current_section = null (all done) → onReady (KycActivity calls agree)
     */
    fun startKyc(
        onLoading: () -> Unit,
        onReady: (KycDataItem) -> Unit,
        onRegistrationPending: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getKycStatus(bearerToken())
            .enqueue(object : Callback<General<KycDataItem>> {
                override fun onResponse(call: Call<General<KycDataItem>>, response: Response<General<KycDataItem>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()!!.data
                        when {
                            data?.entityType.isNullOrEmpty() ->
                                callInitiate(onReady, onRegistrationPending, onError)
                            data?.currentSection == "a" ->
                                submitSectionAPlaceholder(onReady, onError)
                            else -> onReady(data!!)
                        }
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<KycDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    private fun callInitiate(
        onReady: (KycDataItem) -> Unit,
        onRegistrationPending: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        ApiClient.apiService.initiateKyc(bearerToken())
            .enqueue(object : Callback<General<KycDataItem>> {
                override fun onResponse(call: Call<General<KycDataItem>>, response: Response<General<KycDataItem>>) {
                    when {
                        response.isSuccessful ->
                            submitSectionAPlaceholder(onReady, onError)
                        response.code() == 422 ->
                            submitSectionAPlaceholder(onReady, onError)
                        response.code() == 403 ->
                            onRegistrationPending(
                                ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string())
                            )
                        else ->
                            onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<KycDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    private fun submitSectionAPlaceholder(onReady: (KycDataItem) -> Unit, onError: (String) -> Unit) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        val hasGstBody = "0".toRequestBody(textMediaType)
        ApiClient.apiService.submitKycSectionA(bearerToken(), hasGstBody)
            .enqueue(object : Callback<General<KycDataItem>> {
                override fun onResponse(call: Call<General<KycDataItem>>, response: Response<General<KycDataItem>>) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        onReady(response.body()!!.data!!)
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<KycDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun agreeAndFetchStatus(
        onLoading: () -> Unit,
        onReady: (KycDataItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.agreeKyc(bearerToken())
            .enqueue(object : Callback<General<KycDataItem>> {
                override fun onResponse(call: Call<General<KycDataItem>>, response: Response<General<KycDataItem>>) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        onReady(response.body()!!.data!!)
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                        fetchFinalStatus(onReady, onError)
                    }
                }

                override fun onFailure(call: Call<General<KycDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    private fun fetchFinalStatus(onReady: (KycDataItem) -> Unit, onError: (String) -> Unit) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        ApiClient.apiService.getKycStatus(bearerToken())
            .enqueue(object : Callback<General<KycDataItem>> {
                override fun onResponse(call: Call<General<KycDataItem>>, response: Response<General<KycDataItem>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()?.data ?: run {
                            onError(getString(R.string.errGeneric))
                            return
                        }
                        onReady(data)
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<KycDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

}
