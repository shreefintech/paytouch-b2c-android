package com.shreefintech.paytouchconsumer.utill

import android.app.Activity
import android.app.Dialog
import android.view.ViewGroup
import androidx.databinding.ObservableBoolean
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.DialogBankDetailsBinding
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.v2.BankDetailsV2Item
import com.shreefintech.paytouchconsumer.retrofit.model.v2.BankDetailsV2Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object BankDetailsDialogHelper {

    /** Runs [action] when bank details exist; otherwise asks for them first and runs [action] once saved. */
    fun runIfBankAdded(activity: Activity, action: () -> Unit) {
        if (SharedPreferenceHelper.getSharedPreferenceBoolean(activity, Constant.KEY_BANK_DETAILS_ADDED, false)) {
            action()
        } else {
            show(activity, SharedPreferenceHelper.bearerToken(activity), onBankAdded = action)
        }
    }

    private fun show(activity: Activity, authorization: String, onBankAdded: () -> Unit) {
        val binding = DialogBankDetailsBinding.inflate(activity.layoutInflater)
        val showProgress = ObservableBoolean(false)
        binding.showProgress = showProgress

        val dialog = Dialog(activity)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setWindowAnimations(R.style.DialogScaleFadeAnimation)
        dialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)

        binding.onClickListener = android.view.View.OnClickListener { view ->
            when (view) {
                binding.cardClose -> dialog.dismiss()
                binding.cardSubmit -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgress.get()) return@OnClickListener
                    onSubmit(activity, authorization, binding, showProgress, dialog, onBankAdded)
                }
            }
        }

        dialog.show()
    }

    private fun onSubmit(
        activity: Activity,
        authorization: String,
        binding: DialogBankDetailsBinding,
        showProgress: ObservableBoolean,
        dialog: Dialog,
        onBankAdded: () -> Unit
    ) {
        val accountNumber = binding.etAccountNumber.text?.toString()?.trim() ?: ""
        val ifsc = binding.etIfsc.text?.toString()?.trim()?.uppercase() ?: ""
        val branch = binding.etBranch.text?.toString()?.trim() ?: ""

        val msg = when {
            accountNumber.isEmpty() -> activity.getString(R.string.msgBankAccountEmpty)
            accountNumber.length < 9 -> activity.getString(R.string.msgBankAccountInvalid)
            ifsc.isEmpty() -> activity.getString(R.string.msgIfscEmpty)
            ifsc.length != 11 -> activity.getString(R.string.msgIfscInvalid)
            branch.isEmpty() -> activity.getString(R.string.msgBranchNameEmpty)
            else -> null
        }
        if (msg != null) { ToastUtil.showDelete(activity, msg); return }

        if (!Utility.isInternetAvailable(activity)) {
            ToastUtil.showDelete(activity, activity.getString(R.string.msgNoInternet))
            return
        }

        showProgress.set(true)
        ApiClient.apiService.addBankDetails(authorization, BankDetailsV2Request(accountNumber, ifsc, branch))
            .enqueue(object : Callback<BankDetailsV2Item> {
                override fun onResponse(call: Call<BankDetailsV2Item>, response: Response<BankDetailsV2Item>) {
                    showProgress.set(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        SharedPreferenceHelper.setSharedPreferenceBoolean(
                            activity, Constant.KEY_BANK_DETAILS_ADDED, true
                        )
                        // Dialog window is gone once the host is destroyed — dismiss() / onBankAdded() would crash
                        if (activity.isFinishing || activity.isDestroyed) return
                        ToastUtil.showSuccess(activity, activity.getString(R.string.msgBankDetailsSaved))
                        dialog.dismiss()
                        onBankAdded()
                    } else {
                        if (activity.isFinishing || activity.isDestroyed) return
                        val errMsg = ApiHelper.parseErrorMessage(
                            activity, response.code(), response.errorBody()?.string()
                        )
                        ToastUtil.showDelete(activity, errMsg)
                    }
                }

                override fun onFailure(call: Call<BankDetailsV2Item>, t: Throwable) {
                    showProgress.set(false)
                    if (activity.isFinishing || activity.isDestroyed) return
                    ToastUtil.showDelete(
                        activity,
                        t.localizedMessage ?: activity.getString(R.string.errGeneric)
                    )
                }
            })
    }
}
