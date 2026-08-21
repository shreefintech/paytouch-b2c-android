package com.shreefintech.paytouchconsumer.onboarding.kyc.bank

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.onboarding.kyc.bank.model.BankAccountInputItem
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycSubmissionDataItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BankDetailsViewModel(application: Application) : AndroidViewModel(application) {

    fun submit(
        accounts: List<BankAccountInputItem>,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()

        val imgMediaType = "image/*".toMediaTypeOrNull()

        val parts = mutableListOf<MultipartBody.Part>()
        accounts.forEachIndexed { i, account ->
            parts += MultipartBody.Part.createFormData("bank_accounts[$i][account_number]", account.accountNumber)
            parts += MultipartBody.Part.createFormData("bank_accounts[$i][bank_name]", account.bankName)
            parts += MultipartBody.Part.createFormData("bank_accounts[$i][ifsc]", account.ifscCode)
            parts += MultipartBody.Part.createFormData("bank_accounts[$i][branch_name]", account.branchName)
            parts += MultipartBody.Part.createFormData("bank_accounts[$i][proof_type]", account.proofType)
            account.statementPeriod?.let {
                parts += MultipartBody.Part.createFormData("bank_accounts[$i][statement_period]", it.apiValue)
            }
            parts += MultipartBody.Part.createFormData("bank_accounts[$i][bank_proof]", "bank_proof_$i.jpg", account.proofBytes.toRequestBody(imgMediaType))
        }

        ApiClient.apiService.submitKycSectionC(bearerToken(), parts)
            .enqueue(object : Callback<General<KycSubmissionDataItem>> {
                override fun onResponse(call: Call<General<KycSubmissionDataItem>>, response: Response<General<KycSubmissionDataItem>>) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        onSuccess()
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<KycSubmissionDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
