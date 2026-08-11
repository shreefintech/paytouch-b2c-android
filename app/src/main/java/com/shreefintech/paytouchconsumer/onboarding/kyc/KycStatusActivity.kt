package com.shreefintech.paytouchconsumer.onboarding.kyc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityKycStatusBinding
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycStatusItem
import com.shreefintech.paytouchconsumer.utill.Utility

class KycStatusActivity : BaseActivity() {

    companion object {
        private const val EXTRA_STATUS = "extra_status"

        fun start(context: Context, status: KycStatusItem) {
            context.startActivity(
                Intent(context, KycStatusActivity::class.java).apply {
                    putExtra(EXTRA_STATUS, Gson().toJson(status))
                }
            )
        }
    }

    private lateinit var binding: ActivityKycStatusBinding

    private val statusItem: KycStatusItem? by lazy {
        intent.getStringExtra(EXTRA_STATUS)?.let { Gson().fromJson(it, KycStatusItem::class.java) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKycStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.lytToolbar.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.cvRetry.setOnClickListener {
            if (Utility.stopClick()) return@setOnClickListener
            onRetry()
        }

        onBack()
        renderStatus()
    }

    private fun renderStatus() {
        val submissionStatus = statusItem?.submission?.status
        when {
            submissionStatus == "kyc_approved"  -> showApproved()
            submissionStatus?.contains("reject") == true -> showRejected()
            else -> showPending()
        }
    }

    private fun showApproved() {
        binding.iv1.setImageResource(R.drawable.img_kyc_pending)
        binding.tvTitle1.text = getString(R.string.textVerificationApproved)
        binding.tvDes.text = getString(R.string.msgVerificationApproved)
        binding.cvRetry.visibility = View.GONE
    }

    private fun showPending() {
        binding.iv1.setImageResource(R.drawable.img_kyc_pending)
        binding.tvTitle1.text = getString(R.string.textVerificationPending)
        binding.tvDes.text = getString(R.string.msgVerificationPending)
        binding.cvRetry.visibility = View.GONE
    }

    private fun showRejected() {
        binding.iv1.setImageResource(R.drawable.img_kyc_rejected)
        binding.tvTitle1.text = getString(R.string.textVerificationRejected)
        binding.tvDes.text = getString(R.string.msgVerificationRejected)
        binding.cvRetry.visibility = View.VISIBLE
    }

    private fun onRetry() {
        KycActivity.start(mActivity)
        finish()
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }
}
