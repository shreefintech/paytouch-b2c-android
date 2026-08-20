package com.shreefintech.paytouchconsumer.onboarding.kyc.bank

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.enums.StatementPeriod
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycSubmissionDataItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class BankAccountInput(
    val accountNumber: String,
    val bankName: String,
    val ifscCode: String,
    val branchName: String,
    val proofType: String,
    val proofUri: Uri,
    val statementPeriod: StatementPeriod?
)

class BankDetailsViewModel(application: Application) : AndroidViewModel(application) {

    fun submit(
        accounts: List<BankAccountInput>,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()

        val imgMediaType    = "image/*".toMediaTypeOrNull()
        val contentResolver = getApplication<Application>().contentResolver
        val mainHandler     = Handler(Looper.getMainLooper())

        Thread {
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
                val proofBytes = contentResolver.openInputStream(account.proofUri)?.use { it.readBytes() } ?: ByteArray(0)
                parts += MultipartBody.Part.createFormData("bank_accounts[$i][bank_proof]", "bank_proof_$i.jpg", proofBytes.toRequestBody(imgMediaType))
            }

            mainHandler.post {
                ApiClient.apiService.submitKycSectionC(bearerToken(), parts)
                    .enqueue(object : Callback<General<KycSubmissionDataItem>> {
                        override fun onResponse(call: Call<General<KycSubmissionDataItem>>, response: Response<General<KycSubmissionDataItem>>) {
                            if (response.isSuccessful && response.body()?.success == true) {
                                onSuccess()
                            } else {
                                onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                            }
                        }

                        override fun onFailure(call: Call<General<KycSubmissionDataItem>>, t: Throwable) {
                            onError(t.localizedMessage ?: getApplication<Application>().getString(R.string.errGeneric))
                        }
                    })
            }
        }.start()
    }
}
