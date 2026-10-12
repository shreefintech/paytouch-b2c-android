package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.CityItem
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.StateItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.RegisterItem
import com.shreefintech.paytouchconsumer.utill.Utility
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateAccountV2ViewModel(application: Application) : AndroidViewModel(application) {

    fun loadStates(
        onLoading: () -> Unit,
        onSuccess: (List<StateItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getAddressStates("").enqueue(object : Callback<General<List<StateItem>>> {
            override fun onResponse(
                call: Call<General<List<StateItem>>>,
                response: Response<General<List<StateItem>>>
            ) {
                if (response.isSuccessful && response.body()?.data != null) {
                    onSuccess(response.body()!!.data!!)
                } else {
                    val msg = ApiHelper.parseErrorMessage(
                        getApplication(), response.code(), response.errorBody()?.string()
                    )
                    onError(msg)
                }
            }

            override fun onFailure(call: Call<General<List<StateItem>>>, t: Throwable) {
                onError(t.localizedMessage ?: getApplication<Application>().getString(R.string.errGeneric))
            }
        })
    }

    fun loadCities(
        stateId: String,
        onLoading: () -> Unit,
        onSuccess: (List<CityItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getAddressCities("", stateId).enqueue(object : Callback<General<List<CityItem>>> {
            override fun onResponse(
                call: Call<General<List<CityItem>>>,
                response: Response<General<List<CityItem>>>
            ) {
                if (response.isSuccessful && response.body()?.data != null) {
                    onSuccess(response.body()!!.data!!)
                } else {
                    val msg = ApiHelper.parseErrorMessage(
                        getApplication(), response.code(), response.errorBody()?.string()
                    )
                    onError(msg)
                }
            }

            override fun onFailure(call: Call<General<List<CityItem>>>, t: Throwable) {
                onError(t.localizedMessage ?: getApplication<Application>().getString(R.string.errGeneric))
            }
        })
    }

    fun register(
        name: String,
        mobile: String,
        email: String,
        password: String,
        passwordConfirmation: String,
        aadhaarNumber: String,
        aadhaarUri: Uri,
        contentResolver: ContentResolver,
        stateId: String,
        cityId: String,
        referralCode: String,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        Thread {
            try {
                val mimeType = contentResolver.getType(aadhaarUri) ?: "image/jpeg"
                val extension = when {
                    mimeType.contains("pdf") -> ".pdf"
                    mimeType.contains("png") -> ".png"
                    else -> ".jpg"
                }
                val bytes = contentResolver.openInputStream(aadhaarUri)?.use { it.readBytes() }
                if (bytes == null) {
                    postMain { onError(getApplication<Application>().getString(R.string.errGeneric)) }
                    return@Thread
                }
                if (bytes.size > 5 * 1024 * 1024) {
                    postMain { onError(getApplication<Application>().getString(R.string.msgAadhaarPhotoInvalid)) }
                    return@Thread
                }

                val toText: String.() -> RequestBody = { toRequestBody("text/plain".toMediaTypeOrNull()) }
                val photoBody = RequestBody.create(mimeType.toMediaTypeOrNull(), bytes)
                val photoPart = MultipartBody.Part.createFormData("aadhaar_photo", "aadhaar$extension", photoBody)

                ApiClient.apiService.registerV2(
                    name.toText(),
                    mobile.toText(),
                    email.toText(),
                    password.toText(),
                    passwordConfirmation.toText(),
                    aadhaarNumber.toText(),
                    photoPart,
                    stateId.toText(),
                    cityId.toText(),
                    referralCode.toText(),
                    "android".toText()
                ).enqueue(object : Callback<RegisterItem> {
                    override fun onResponse(call: Call<RegisterItem>, response: Response<RegisterItem>) {
                        if (response.isSuccessful) {
                            onSuccess()
                        } else {
                            val msg = ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                            onError(msg)
                        }
                    }

                    override fun onFailure(call: Call<RegisterItem>, t: Throwable) {
                        onError(t.localizedMessage ?: getApplication<Application>().getString(R.string.errGeneric))
                    }
                })
            } catch (e: Exception) {
                Utility.logError(e)
                postMain { onError(getApplication<Application>().getString(R.string.errGeneric)) }
            }
        }.start()
    }

    private fun postMain(block: () -> Unit) {
        Handler(Looper.getMainLooper()).post(block)
    }
}
