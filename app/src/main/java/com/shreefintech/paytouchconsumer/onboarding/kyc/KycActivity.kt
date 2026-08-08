package com.shreefintech.paytouchconsumer.onboarding.kyc

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityKycBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.onboarding.CreateVirtualAccountActivity
import com.shreefintech.paytouchconsumer.onboarding.kyc.bank.BankDetailsActivity
import com.shreefintech.paytouchconsumer.onboarding.kyc.identity.IdentityVerificationActivity
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.gone

class KycActivity : BaseActivity() {

    private lateinit var binding: ActivityKycBinding

    private var identityDone = false
    private var bankDone = false

    private val identityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == 1) {
            identityDone = true
            SharedPreferenceHelper.setSharedPreferenceBoolean(mActivity, Constant.KEY_KYC_IDENTITY_DONE, true)
            onSectionResult()
        }
    }

    private val bankLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == 1) {
            bankDone = true
            SharedPreferenceHelper.setSharedPreferenceBoolean(mActivity, Constant.KEY_KYC_BANK_DONE, true)
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
            targetView   = binding.flCard,
            rootView     = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion   = 0f,
            strokeWidth = 1,
            strokeColor = ContextCompat.getColor(mActivity, R.color.primary),
            solidStroke = true,
            blur         = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        binding.onClickListener = onClickListener()

        identityDone = SharedPreferenceHelper.getSharedPreferenceBoolean(mActivity, Constant.KEY_KYC_IDENTITY_DONE, false)
        bankDone     = SharedPreferenceHelper.getSharedPreferenceBoolean(mActivity, Constant.KEY_KYC_BANK_DONE, false)
        updateProgress()
    }

    private fun updateProgress() {
        val done = listOf(identityDone, bankDone).count { it }
        binding.tvProgressCount.text = getString(R.string.fmtDocumentProgress, done)
        binding.pbDocProgress.progress = done
    }

    private fun onSectionResult() {
        updateProgress()
        if (identityDone && bankDone) {
            startActivity(Intent(mActivity, CreateVirtualAccountActivity::class.java))
            finish()
        }
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.mcIdentity -> {
                    if (Utility.stopClick()) return@OnClickListener
                    identityLauncher.launch(Intent(mActivity, IdentityVerificationActivity::class.java))
                }

                binding.mcBank -> {
                    if (Utility.stopClick()) return@OnClickListener
                    bankLauncher.launch(Intent(mActivity, BankDetailsActivity::class.java))
                }
            }
        }
    }
}
