package com.shreefintech.paytouchconsumer.onboarding.kyc

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
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

    fun startKyc(
        onLoading: () -> Unit,
        onReady: (KycSubmissionDataItem?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        val entityTypeBody = "individual".toRequestBody(textMediaType)
        ApiClient.apiService.initiateKyc(bearerToken(), entityTypeBody)
            .enqueue(object : Callback<General<KycSubmissionDataItem>> {
                override fun onResponse(
                    call: Call<General<KycSubmissionDataItem>>,
                    response: Response<General<KycSubmissionDataItem>>
                ) {
                    when {
                        response.isSuccessful && response.body()?.success == true ->
                            ensureSectionA(response.body()?.data, onReady, onError)

                        // "KYC already initiated for this user." — expected state, not an error.
                        response.code() == 422 -> fetchStatus(onReady, onError)

                        else -> onError(
                            ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string())
                        )
                    }
                }

                override fun onFailure(call: Call<General<KycSubmissionDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    private fun fetchStatus(
        onReady: (KycSubmissionDataItem?) -> Unit,
        onError: (String) -> Unit
    ) {
        ApiClient.apiService.getKycStatus(bearerToken())
            .enqueue(object : Callback<General<KycSubmissionDataItem>> {
                override fun onResponse(
                    call: Call<General<KycSubmissionDataItem>>,
                    response: Response<General<KycSubmissionDataItem>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        ensureSectionA(response.body()?.data, onReady, onError)
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<KycSubmissionDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    private fun ensureSectionA(
        submission: KycSubmissionDataItem?,
        onReady: (KycSubmissionDataItem?) -> Unit,
        onError: (String) -> Unit
    ) {
        val sectionAAlreadySubmitted = submission?.sectionReviews?.any { it.section == "A" } == true
        if (sectionAAlreadySubmitted) {
            onReady(submission)
        } else {
            submitSectionAPlaceholder(onReady, onError)
        }
    }

    // TODO(PAYTOUCH-KYC): Dashboard rejects an empty `documents` array over multipart — remove this placeholder once fixed.
    private fun submitSectionAPlaceholder(
        onReady: (KycSubmissionDataItem?) -> Unit,
        onError: (String) -> Unit
    ) {
        val hasGstBody = "0".toRequestBody(textMediaType)
        val documentTypeBody = "gst".toRequestBody(textMediaType)
        val documentPart = MultipartBody.Part.createFormData(
            "documents[0][file]",
            "gst_placeholder.jpg",
            placeholderDocumentBody()
        )
        ApiClient.apiService.submitKycSectionA(bearerToken(), hasGstBody, documentTypeBody, documentPart)
            .enqueue(object : Callback<General<KycSubmissionDataItem>> {
                override fun onResponse(
                    call: Call<General<KycSubmissionDataItem>>,
                    response: Response<General<KycSubmissionDataItem>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        onReady(response.body()?.data)
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<KycSubmissionDataItem>>, t: Throwable) {
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
