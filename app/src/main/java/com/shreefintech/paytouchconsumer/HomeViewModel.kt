package com.shreefintech.paytouchconsumer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shreefintech.paytouchconsumer.enums.NextStep
import com.shreefintech.paytouchconsumer.enums.RewardsTier
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.auth.LogoutRequest
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MeDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MessageItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayCategoryItem
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.RewardsLevelItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import com.shreefintech.paytouchconsumer.utill.saveOnboardingFlags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    fun logout(onLoading: () -> Unit, onComplete: () -> Unit, onError: (String) -> Unit) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        callLogout(onComplete, onError)
    }

    /** GET billpay/categories — only categories with at least one active biller. */
    fun loadBillPayCategories(
        onLoading: () -> Unit,
        onSuccess: (List<BillPayCategoryItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getBillPayCategories(bearerToken())
            .enqueue(object : Callback<General<List<BillPayCategoryItem>>> {
                override fun onResponse(
                    call: Call<General<List<BillPayCategoryItem>>>,
                    response: Response<General<List<BillPayCategoryItem>>>
                ) {
                    val categories = response.body()?.data
                    if (response.isSuccessful && categories != null) {
                        onSuccess(categories)
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<List<BillPayCategoryItem>>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    /**
     * Silently checks /api/mobile/rewards/level on every Dashboard appear.
     * Saves the level first so each level shows exactly once. With no saved level
     * (fresh install / re-login) the current level is celebrated once.
     * No toast on failure — matches iOS "the level check is silent" spec.
     */
    fun checkLevelUp(onLevelUp: (RewardsLevelItem) -> Unit) {
        if (!Utility.isInternetAvailable(getApplication())) return
        ApiClient.apiService.getCustomerLevel(bearerToken())
            .enqueue(object : Callback<General<RewardsLevelItem?>> {
                override fun onResponse(
                    call: Call<General<RewardsLevelItem?>>,
                    response: Response<General<RewardsLevelItem?>>
                ) {
                    val data = if (response.isSuccessful) response.body()?.data else null
                    if (data == null) return
                    val newLevel = data.level ?: return
                    val saved = SharedPreferenceHelper.getSharedPreferenceString(
                        getApplication(), Constant.KEY_LAST_SEEN_LEVEL, ""
                    ).orEmpty()
                    SharedPreferenceHelper.setSharedPreferenceString(
                        getApplication(), Constant.KEY_LAST_SEEN_LEVEL, newLevel
                    )
                    if (RewardsTier.shouldShowLevelUp(newLevel, saved)) onLevelUp(data)
                }

                override fun onFailure(call: Call<General<RewardsLevelItem?>>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    // Survives Activity recreation — a recreated Home must not re-block while the previous upload is in flight
    private var isAadhaarUploading = false
    private var aadhaarCheckCall: Call<General<MeDataItem>>? = null

    /**
     * Silent — Home stays usable if mobile/auth/me fails; the flag is re-checked on the next Home launch.
     * Also the post-login source of the bank/PAN onboarding flags (see [saveOnboardingFlags]) — keep this call.
     */
    fun checkAadhaarRejected(onAadhaarDialog: (isAddMode: Boolean) -> Unit, onRedirect: (String?) -> Unit) {
        if (!Utility.isInternetAvailable(getApplication())) return
        aadhaarCheckCall?.cancel()
        aadhaarCheckCall = ApiClient.apiService.getMe(bearerToken()).also {
            it.enqueue(object : Callback<General<MeDataItem>> {
                override fun onResponse(call: Call<General<MeDataItem>>, response: Response<General<MeDataItem>>) {
                    if (aadhaarCheckCall === call) aadhaarCheckCall = null
                    val data = response.body()?.data
                    // Refreshes the bank/PAN flags on every Home launch
                    saveOnboardingFlags(data?.bankDetailsAdded, data?.panAdded, data?.nextStep)
                    // Checked at response time so the flags above still refresh while an upload is in flight.
                    // ponytail: if that in-flight upload then fails, the block returns only on the next Home launch;
                    // switch the upload result to LiveData if Home ever stops being portrait-locked
                    if (isAadhaarUploading) return
                    if (!response.isSuccessful || data == null) return
                    val step = NextStep.from(data.nextStep)
                    when {
                        // Resubmit only fixes an open rejection naming both Aadhaar items (409 / 422 otherwise)
                        data.canResubmitAadhaar -> onAadhaarDialog(false)
                        // No Aadhaar on file yet — kyc/individual/aadhaar, which also takes state + city
                        step == NextStep.AADHAAR_REQUIRED -> onAadhaarDialog(true)
                        // Steps that leave Home — NextStepRouter picks KycActivity / KycStatusActivity
                        step == NextStep.KYC_REQUIRED || step == NextStep.PENDING_APPROVAL -> onRedirect(data.nextStep)
                    }
                }

                override fun onFailure(call: Call<General<MeDataItem>>, t: Throwable) {
                    if (aadhaarCheckCall === call) aadhaarCheckCall = null
                    t.printStackTrace()
                }
            })
        }
    }

    /** Releases the pending check's callback (and the Activity it captures). */
    fun cancelAadhaarCheck() {
        aadhaarCheckCall?.cancel()
        aadhaarCheckCall = null
    }

    /** [stateId] + [cityId] non-null → kyc/individual/aadhaar (aadhaar_required); else kyc/individual/resubmit. */
    fun reuploadAadhaar(
        aadhaarNumber: String,
        aadhaarUri: Uri,
        stateId: String?,
        cityId: String?,
        onLoading: () -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        isAadhaarUploading = true
        viewModelScope.launch {
            // Size already capped by FilePickerUtil (2 MB) — no re-check here
            val photoPart = withContext(Dispatchers.IO) {
                Utility.uriToFilePart(getApplication(), aadhaarUri, "aadhaar_photo", "aadhaar")
            }
            if (photoPart == null) {
                isAadhaarUploading = false
                onError(getString(R.string.errGeneric))
                return@launch
            }

            val textType = "text/plain".toMediaTypeOrNull()
            val numberPart = aadhaarNumber.toRequestBody(textType)
            val call = if (stateId != null && cityId != null) {
                ApiClient.apiService.addAadhaar(
                    bearerToken(), numberPart, photoPart, stateId.toRequestBody(textType), cityId.toRequestBody(textType)
                )
            } else {
                ApiClient.apiService.reuploadAadhaar(bearerToken(), numberPart, photoPart)
            }
            call.enqueue(object : Callback<MessageItem> {
                override fun onResponse(call: Call<MessageItem>, response: Response<MessageItem>) {
                    isAadhaarUploading = false
                    val body = response.body()
                    // Only an explicit success:false is a failure — the key may be absent (like /logout)
                    when {
                        !response.isSuccessful -> onError(
                            ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string())
                        )
                        body?.success == false -> onError(body.message ?: getString(R.string.errGeneric))
                        else -> onSuccess(body?.message ?: getString(R.string.msgAadhaarReuploaded))
                    }
                }

                override fun onFailure(call: Call<MessageItem>, t: Throwable) {
                    isAadhaarUploading = false
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
        }
    }

    private fun callLogout(onComplete: () -> Unit, onError: (String) -> Unit) {
        val fcmToken = SharedPreferenceHelper.getSharedPreferenceString(
            getApplication(), Constant.KEY_FCM_TOKEN, ""
        )?.ifEmpty { null }
        ApiClient.apiService.logout(bearerToken(), LogoutRequest(fcmToken))
            .enqueue(object : Callback<MessageItem> {
                override fun onResponse(call: Call<MessageItem>, response: Response<MessageItem>) {
                    // /logout response body only ever contains "message" (no "success" key),
                    // so checking body?.success == true would always be false. isSuccessful alone is correct here.
                    if (response.isSuccessful) {
                        SharedPreferenceHelper.setSharedPreferenceString(
                            getApplication(), Constant.KEY_FCM_TOKEN, ""
                        )
                        SharedPreferenceHelper.setSharedPreferenceBoolean(
                            getApplication(), Constant.KEY_FCM_TOKEN_AUTHED, false
                        )
                        onComplete()
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }
                override fun onFailure(call: Call<MessageItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getApplication<Application>().getString(R.string.errGeneric))
                }
            })
    }
}
