package com.shreefintech.paytouchconsumer.onboarding.kyc

import android.content.Context
import android.content.Intent
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

    private val identityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == 1) {
            identityDone = true
            binding.ivSectionAStatus.setImageResource(R.drawable.ic_success)
            binding.ivSectionAStatus.visibility = View.VISIBLE
            onSectionResult()

            updateProgress()
        }
    }

    private val bankLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == 1) {
            bankDone = true
            binding.ivSectionBStatus.setImageResource(R.drawable.ic_success)
            binding.ivSectionBStatus.visibility = View.VISIBLE
            onSectionResult()
        }
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
            targetView  = binding.flCard,
            rootView    = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion  = 0f,
            strokeWidth = 1,
            strokeColor = ContextCompat.getColor(mActivity, R.color.primary),
            solidStroke = true,
            blur        = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        binding.onClickListener = onClickListener()
        startKyc()
    }

    private fun startKyc() {
        viewModel.startKyc(
            onLoading = {
                binding.shimmerKyc.visibility         = View.VISIBLE
                binding.shimmerKyc.startShimmer()
                binding.llKycContent.visibility        = View.GONE
                binding.llPendingRegistration.visibility = View.GONE
            },
            onReady = { statusItem ->
                binding.shimmerKyc.stopShimmer()
                binding.shimmerKyc.visibility         = View.GONE
                binding.llKycContent.visibility        = View.VISIBLE
                binding.llPendingRegistration.visibility = View.GONE
                applyStatus(statusItem)
            },
            onRegistrationPending = { msg ->
                binding.shimmerKyc.stopShimmer()
                binding.shimmerKyc.visibility         = View.GONE
                binding.llKycContent.visibility        = View.GONE
                binding.llPendingRegistration.visibility = View.VISIBLE
                binding.tvPendingRegistrationMsg.text  = msg
            },
            onError = { msg ->
                binding.shimmerKyc.stopShimmer()
                binding.shimmerKyc.visibility         = View.GONE
                binding.llKycContent.visibility        = View.VISIBLE
                binding.llPendingRegistration.visibility = View.GONE
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun applyStatus(statusItem: KycStatusItem) {
        identityDone = statusItem.submission?.sectionBSubmittedAt != null
        bankDone     = statusItem.submission?.sectionCSubmittedAt != null
        updateSectionIcons(statusItem)
        updateProgress()
    }

    private fun updateSectionIcons(statusItem: KycStatusItem) {
        val isRejected = statusItem.submission?.status?.contains("reject") == true

        // Identity card icon — driven by section B
        when {
            statusItem.submission?.sectionBSubmittedAt != null -> {
                binding.ivSectionAStatus.setImageResource(R.drawable.ic_success)
                binding.ivSectionAStatus.visibility = View.VISIBLE
            }
            isRejected -> {
                binding.ivSectionAStatus.setImageResource(R.drawable.ic_reject)
                binding.ivSectionAStatus.visibility = View.VISIBLE
            }
            else -> binding.ivSectionAStatus.visibility = View.GONE
        }

        // Bank card icon — driven by section C
        when {
            statusItem.submission?.sectionCSubmittedAt != null -> {
                binding.ivSectionBStatus.setImageResource(R.drawable.ic_success)
                binding.ivSectionBStatus.visibility = View.VISIBLE
            }
            isRejected -> {
                binding.ivSectionBStatus.setImageResource(R.drawable.ic_reject)
                binding.ivSectionBStatus.visibility = View.VISIBLE
            }
            else -> binding.ivSectionBStatus.visibility = View.GONE
        }
    }

    private fun updateProgress() {
        val done = listOf(identityDone, bankDone).count { it }
        binding.tvProgressCount.text = getString(R.string.fmtDocumentProgress, done)
        binding.pbDocProgress.progress = done
    }

    private fun onSectionResult() {
        updateProgress()
        if (identityDone && bankDone) {
            agreeAndNavigateToStatus()
        }
    }

    private fun agreeAndNavigateToStatus() {
        binding.shimmerKyc.visibility  = View.VISIBLE
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
                binding.shimmerKyc.visibility  = View.GONE
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
                    identityLauncher.launch(IdentityVerificationActivity.buildIntent(mActivity))
                }

                binding.mcBank -> {
                    if (Utility.stopClick()) return@OnClickListener
                    bankLauncher.launch(BankDetailsActivity.buildIntent(mActivity))
                }
            }
        }
    }
}
