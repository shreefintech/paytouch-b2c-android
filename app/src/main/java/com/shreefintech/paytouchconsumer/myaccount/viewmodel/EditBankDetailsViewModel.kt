package com.shreefintech.paytouchconsumer.myaccount.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.EditBankUpdateDataItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditBankDetailsViewModel(application: Application) : AndroidViewModel(application) {

    fun submit(
        bankId: Int,
        accountHolderName: String,
        accountNumber: String,
        ifsc: String,
        bankName: String,
        branchName: String,
        password: String,
        proofBytes: ByteArray?,
        proofExtension: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }

        val parts = mutableListOf<MultipartBody.Part>()
        parts += MultipartBody.Part.createFormData("password", password)
        parts += MultipartBody.Part.createFormData("bank_id", bankId.toString())
        parts += MultipartBody.Part.createFormData("account_holder_name", accountHolderName)
        parts += MultipartBody.Part.createFormData("account_number", accountNumber)
        parts += MultipartBody.Part.createFormData("ifsc", ifsc)
        parts += MultipartBody.Part.createFormData("bank_name", bankName)
        parts += MultipartBody.Part.createFormData("branch_name", branchName)
        proofBytes?.let {
            val isPdf = proofExtension.equals("pdf", ignoreCase = true)
            parts += MultipartBody.Part.createFormData(
                "bank_proof",
                "bank_proof.$proofExtension",
                it.toRequestBody((if (isPdf) "application/pdf" else "image/jpeg").toMediaTypeOrNull())
            )
        }

        ApiClient.apiService.editBankAccount(bearerToken(), parts)
            .enqueue(object : Callback<General<EditBankUpdateDataItem>> {
                override fun onResponse(
                    call: Call<General<EditBankUpdateDataItem>>,
                    response: Response<General<EditBankUpdateDataItem>>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        onSuccess(body.message ?: getString(R.string.msgBankDetailsSubmitSuccess))
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<EditBankUpdateDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
