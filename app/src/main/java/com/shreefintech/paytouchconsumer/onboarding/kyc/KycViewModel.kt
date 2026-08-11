package com.shreefintech.paytouchconsumer.onboarding.kyc

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycAgreeDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycStatusItem
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycSubmissionDataItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

class KycViewModel(application: Application) : AndroidViewModel(application) {

    private val textMediaType = "text/plain".toMediaTypeOrNull()

    /**
     * Entry point for the KYC hub.
     * 1. Always fetches /status first.
     * 2. Uses entity_type + section_a_submitted_at to decide whether initiate / section-A are needed.
     * 3. Passes the status item to onReady so KycActivity can derive section B/C completion state.
     */
    fun startKyc(
        onLoading: () -> Unit,
        onReady: (KycStatusItem) -> Unit,
        onRegistrationPending: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getKycStatus(bearerToken())
            .enqueue(object : Callback<KycStatusItem> {
                override fun onResponse(call: Call<KycStatusItem>, response: Response<KycStatusItem>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val statusItem = response.body()!!
                        val submission = statusItem.submission
                        when {
                            submission == null || submission.entityType.isNullOrEmpty() ->
                                callInitiate(
                                    onReady = { onReady(statusItem) },
                                    onRegistrationPending = onRegistrationPending,
                                    onError = onError
                                )

                            submission.sectionASubmittedAt == null ->
                                submitSectionAPlaceholder(onReady = { onReady(statusItem) }, onError = onError)

                            else -> onReady(statusItem)
                        }
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<KycStatusItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    private fun callInitiate(
        onReady: () -> Unit,
        onRegistrationPending: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val entityTypeBody = "individual".toRequestBody(textMediaType)
        ApiClient.apiService.initiateKyc(bearerToken(), entityTypeBody)
            .enqueue(object : Callback<General<KycSubmissionDataItem>> {
                override fun onResponse(
                    call: Call<General<KycSubmissionDataItem>>,
                    response: Response<General<KycSubmissionDataItem>>
                ) {
                    when {
                        response.isSuccessful && response.body()?.success == true ->
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

                override fun onFailure(call: Call<General<KycSubmissionDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    // TODO(PAYTOUCH-KYC): Dashboard rejects an empty `documents` array over multipart — remove this placeholder once fixed.
    private fun submitSectionAPlaceholder(onReady: () -> Unit, onError: (String) -> Unit) {
        val hasGstBody       = "0".toRequestBody(textMediaType)
        val documentTypeBody = "gst".toRequestBody(textMediaType)
        val documentPart     = MultipartBody.Part.createFormData(
            "documents[0][file]", "gst_placeholder.jpg", placeholderDocumentBody()
        )
        ApiClient.apiService.submitKycSectionA(bearerToken(), hasGstBody, documentTypeBody, documentPart)
            .enqueue(object : Callback<General<KycSubmissionDataItem>> {
                override fun onResponse(
                    call: Call<General<KycSubmissionDataItem>>,
                    response: Response<General<KycSubmissionDataItem>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        onReady()
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<KycSubmissionDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun agreeAndFetchStatus(
        onLoading: () -> Unit,
        onReady: (KycStatusItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        val emptyBody = ByteArray(0).toRequestBody("application/json".toMediaTypeOrNull())
        ApiClient.apiService.agreeKyc(bearerToken(), emptyBody)
            .enqueue(object : Callback<General<KycAgreeDataItem>> {
                override fun onResponse(
                    call: Call<General<KycAgreeDataItem>>,
                    response: Response<General<KycAgreeDataItem>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        fetchFinalStatus(onReady, onError)
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<KycAgreeDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    private fun fetchFinalStatus(onReady: (KycStatusItem) -> Unit, onError: (String) -> Unit) {
        ApiClient.apiService.getKycStatus(bearerToken())
            .enqueue(object : Callback<KycStatusItem> {
                override fun onResponse(call: Call<KycStatusItem>, response: Response<KycStatusItem>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        onReady(response.body()!!)
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<KycStatusItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    private fun placeholderDocumentBody(): RequestBody {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        bitmap.recycle()
        return stream.toByteArray().toRequestBody("image/jpeg".toMediaTypeOrNull())
    }
}
