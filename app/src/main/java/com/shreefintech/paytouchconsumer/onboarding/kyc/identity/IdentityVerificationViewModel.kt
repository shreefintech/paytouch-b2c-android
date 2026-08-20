package com.shreefintech.paytouchconsumer.onboarding.kyc.identity

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycSignatoryDataItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class IdentityVerificationViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val TOTAL_STEPS = 4
    }

    val currentStep = MutableLiveData(0)

    var mobile = ""; private set
    var email = ""; private set

    var aadhaarNumber = ""; private set
    var aadhaarFrontUri: Uri? = null; private set
    var aadhaarBackUri: Uri? = null; private set

    var panNumber = ""; private set
    var panFrontUri: Uri? = null; private set

    var selfieUri: Uri? = null; private set

    fun setStep1(mobile: String, email: String) {
        this.mobile = mobile
        this.email = email
    }

    fun setAadhaarNumber(number: String) { aadhaarNumber = number }
    fun setAadhaarFrontUri(uri: Uri?) { aadhaarFrontUri = uri }
    fun setAadhaarBackUri(uri: Uri?) { aadhaarBackUri = uri }

    fun setPanNumber(number: String) { panNumber = number }
    fun setPanFrontUri(uri: Uri?) { panFrontUri = uri }

    fun setSelfieUri(uri: Uri?) { selfieUri = uri }

    fun goToNextStep() {
        val step = currentStep.value ?: 0
        if (step < TOTAL_STEPS - 1) currentStep.value = step + 1
    }

    /** @return false when already at the first step (caller should exit the flow instead) */
    fun goToPreviousStep(): Boolean {
        val step = currentStep.value ?: 0
        if (step == 0) return false
        currentStep.value = step - 1
        return true
    }

    fun submitIdentity(
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()

        val textMediaType   = "text/plain".toMediaTypeOrNull()
        val imgMediaType    = "image/*".toMediaTypeOrNull()
        val contentResolver = getApplication<Application>().contentResolver
        val mainHandler     = Handler(Looper.getMainLooper())

        val emailBody   = email.toRequestBody(textMediaType)
        val mobileBody  = mobile.toRequestBody(textMediaType)
        val panBody     = panNumber.toRequestBody(textMediaType)
        val aadhaarBody = aadhaarNumber.toRequestBody(textMediaType)

        Thread {
            val panBytes          = panFrontUri?.let { contentResolver.openInputStream(it)?.use { s -> s.readBytes() } }
            val aadhaarFrontBytes = aadhaarFrontUri?.let { contentResolver.openInputStream(it)?.use { s -> s.readBytes() } }
            val aadhaarBackBytes  = aadhaarBackUri?.let { contentResolver.openInputStream(it)?.use { s -> s.readBytes() } }
            val selfieBytes       = selfieUri?.let { contentResolver.openInputStream(it)?.use { s -> s.readBytes() } }

            if (panBytes == null || aadhaarFrontBytes == null || aadhaarBackBytes == null || selfieBytes == null) {
                mainHandler.post { onError(getApplication<Application>().getString(R.string.errGeneric)) }
                return@Thread
            }

            val panFilePart           = MultipartBody.Part.createFormData("pan_file", "pan.jpg", panBytes.toRequestBody(imgMediaType))
            val aadhaarFrontFilePart  = MultipartBody.Part.createFormData("aadhaar_front_file", "aadhaar_front.jpg", aadhaarFrontBytes.toRequestBody(imgMediaType))
            val aadhaarBackFilePart   = MultipartBody.Part.createFormData("aadhaar_back_file", "aadhaar_back.jpg", aadhaarBackBytes.toRequestBody(imgMediaType))
            val passportPhotoFilePart = MultipartBody.Part.createFormData("passport_photo_file", "passport.jpg", selfieBytes.toRequestBody(imgMediaType))

            mainHandler.post {
                ApiClient.apiService.submitKycSectionB(
                    bearerToken(), emailBody, mobileBody, panBody, aadhaarBody,
                    panFilePart, aadhaarFrontFilePart, aadhaarBackFilePart, passportPhotoFilePart
                ).enqueue(object : Callback<General<KycSignatoryDataItem>> {
                    override fun onResponse(call: Call<General<KycSignatoryDataItem>>, response: Response<General<KycSignatoryDataItem>>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            onSuccess()
                        } else {
                            onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<General<KycSignatoryDataItem>>, t: Throwable) {
                        onError(t.localizedMessage ?: getApplication<Application>().getString(R.string.errGeneric))
                    }
                })
            }
        }.start()
    }
}
