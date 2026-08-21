package com.shreefintech.paytouchconsumer.onboarding.kyc

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityKycBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.onboarding.kyc.bank.BankDetailsActivity
import com.shreefintech.paytouchconsumer.onboarding.kyc.identity.IdentityVerificationActivity
import com.shreefintech.paytouchconsumer.enums.KycSectionStatus
import com.shreefintech.paytouchconsumer.enums.KycSubmissionStatus
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycStatusItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.gone

class KycActivity : BaseActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, KycActivity::class.java))
        }
    }

    private lateinit var binding: ActivityKycBinding
    private val viewModel: KycViewModel by viewModels()

    private var identityDone = false
    private var bankDone = false

    private val identityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == 1) startKyc()
        }

    private val bankLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == 1) startKyc()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKycBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.incToolbar.ivBack.gone()

        LiquidGlassEffect.attach(
            targetView = binding.flCard,
            rootView = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion = 0f,
            strokeWidth = 1,
            strokeColor = ContextCompat.getColor(mActivity, R.color.primary),
            solidStroke = true,
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        binding.onClickListener = onClickListener()
        startKyc()
    }

    private fun startKyc() {
        viewModel.startKyc(
            onLoading = {
                binding.shimmerKyc.visibility = View.VISIBLE
                binding.shimmerKyc.startShimmer()
                binding.llKycContent.visibility = View.GONE
                binding.llPendingRegistration.visibility = View.GONE
            },
            onReady = { statusItem ->
                binding.shimmerKyc.stopShimmer()
                binding.shimmerKyc.visibility = View.GONE
                binding.llKycContent.visibility = View.VISIBLE
                binding.llPendingRegistration.visibility = View.GONE
                applyStatus(statusItem)
            },
            onRegistrationPending = { msg ->
                binding.shimmerKyc.stopShimmer()
                binding.shimmerKyc.visibility = View.GONE
                binding.llKycContent.visibility = View.GONE
                binding.llPendingRegistration.visibility = View.VISIBLE
                binding.tvPendingRegistrationMsg.text = msg
            },
            onError = { msg ->
                binding.shimmerKyc.stopShimmer()
                binding.shimmerKyc.visibility = View.GONE
                binding.llKycContent.visibility = View.VISIBLE
                binding.llPendingRegistration.visibility = View.GONE
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun applyStatus(statusItem: KycStatusItem) {
        val sub = statusItem.submission
        if (KycSubmissionStatus.from(sub?.status) == KycSubmissionStatus.KYC_SUBMITTED) {
            KycStatusActivity.start(mActivity, statusItem)
            finish()
            return
        }

        val sectionBStatus = KycSectionStatus.from(statusItem.sections?.b?.status)
        val sectionCStatus = KycSectionStatus.from(statusItem.sections?.c?.status)

        identityDone = sectionBStatus == KycSectionStatus.UNDER_REVIEW
        bankDone = sectionCStatus == KycSectionStatus.UNDER_REVIEW

        if (identityDone && bankDone) {
            agreeAndNavigateToStatus()
            return
        }

        updateSectionIcons(statusItem)
        updateProgress()
    }

    private fun updateSectionIcons(statusItem: KycStatusItem) {
        val sectionBStatus = KycSectionStatus.from(statusItem.sections?.b?.status)
        val sectionCStatus = KycSectionStatus.from(statusItem.sections?.c?.status)

        // Identity card icon — driven by section B
        when (sectionBStatus) {
            KycSectionStatus.UNDER_REVIEW -> {
                binding.ivSectionAStatus.setImageResource(R.drawable.ic_success)
                binding.ivSectionAStatus.imageTintList = null
                binding.mcIdentity.alpha = 0.5f
                binding.mcIdentity.isClickable = false
            }
            KycSectionStatus.REJECTED -> {
                binding.ivSectionAStatus.setImageResource(R.drawable.ic_reject)
                binding.ivSectionAStatus.imageTintList = null
                binding.mcIdentity.alpha = 1f
                binding.mcIdentity.isClickable = true
            }
            else -> {
                binding.ivSectionAStatus.setImageResource(R.drawable.ic_right_arrow)
                binding.ivSectionAStatus.imageTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.sheet_divider))
                binding.mcIdentity.alpha = 1f
                binding.mcIdentity.isClickable = true
            }
        }
        binding.ivSectionAStatus.visibility = View.VISIBLE

        // Bank card icon — driven by section C
        when (sectionCStatus) {
            KycSectionStatus.UNDER_REVIEW -> {
                binding.ivSectionBStatus.setImageResource(R.drawable.ic_success)
                binding.ivSectionBStatus.imageTintList = null
                binding.mcBank.alpha = 0.5f
                binding.mcBank.isClickable = false
            }
            KycSectionStatus.REJECTED -> {
                binding.ivSectionBStatus.setImageResource(R.drawable.ic_reject)
                binding.ivSectionBStatus.imageTintList = null
                binding.mcBank.alpha = if (identityDone) 1f else 0.7f
                binding.mcBank.isClickable = identityDone
            }
            else -> {
                binding.ivSectionBStatus.setImageResource(R.drawable.ic_right_arrow)
                binding.ivSectionBStatus.imageTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.sheet_divider))
                binding.mcBank.alpha = if (identityDone) 1.0f else 0.7f
                binding.mcBank.isClickable = identityDone
            }
        }
        binding.ivSectionBStatus.visibility = View.VISIBLE
    }

    private fun updateProgress() {
        val done = listOf(identityDone, bankDone).count { it }
        binding.tvProgressCount.text = getString(R.string.fmtDocumentProgress, done)
        binding.pbDocProgress.progress = done
    }

    private fun agreeAndNavigateToStatus() {
        binding.shimmerKyc.visibility = View.VISIBLE
        binding.shimmerKyc.startShimmer()
        binding.llKycContent.visibility = View.GONE

        viewModel.agreeAndFetchStatus(
            onLoading = {},
            onReady = { statusItem ->
                KycStatusActivity.start(mActivity, statusItem)
                finish()
            },
            onError = { msg ->
                binding.shimmerKyc.stopShimmer()
                binding.shimmerKyc.visibility = View.GONE
                binding.llKycContent.visibility = View.VISIBLE
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.mcIdentity -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (identityDone) return@OnClickListener
                    identityLauncher.launch(IdentityVerificationActivity.buildIntent(mActivity))
                }

                binding.mcBank -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (!identityDone || bankDone) return@OnClickListener
                    bankLauncher.launch(BankDetailsActivity.buildIntent(mActivity))
                }
            }
        }
    }
}
