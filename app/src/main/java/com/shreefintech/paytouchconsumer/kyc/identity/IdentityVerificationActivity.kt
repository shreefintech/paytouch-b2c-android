package com.shreefintech.paytouchconsumer.kyc.identity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.lifecycleScope
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityIdentityVerificationBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.kyc.identity.fragment.BaseKycStepFragment
import com.shreefintech.paytouchconsumer.kyc.identity.fragment.KycStep1Fragment
import com.shreefintech.paytouchconsumer.kyc.identity.fragment.KycStep2Fragment
import com.shreefintech.paytouchconsumer.kyc.identity.fragment.KycStep3Fragment
import com.shreefintech.paytouchconsumer.kyc.identity.fragment.KycStep4Fragment
import com.shreefintech.paytouchconsumer.utill.FilePickerUtil
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class IdentityVerificationActivity : BaseActivity() {

    companion object {
        fun buildIntent(context: Context): Intent =
            Intent(context, IdentityVerificationActivity::class.java)
    }

    private lateinit var binding: ActivityIdentityVerificationBinding
    private val viewModel: IdentityVerificationViewModel by viewModels()
    private val showProgressSubmit = ObservableBoolean(false)
    private val showCaptureButton = ObservableBoolean(false)
    private var resultCode = 0

    private val dotViews = mutableListOf<AppCompatImageView>()
    private val stepFragments by lazy {
        listOf(KycStep1Fragment(), KycStep2Fragment(), KycStep3Fragment(), KycStep4Fragment())
    }

    // ─── Document picker — owned by the host so it survives fragment re-creation ──
    private lateinit var filePickerUtil: FilePickerUtil
    private var onDocumentPicked: ((Uri) -> Unit)? = null

    // ─── Live selfie capture with liveness check (SelfieCaptureActivity) ───────
    private var cameraOutputFile: File? = null
    private var cameraOutputUri: Uri? = null
    private var onSelfieCaptured: ((Uri) -> Unit)? = null
    private val selfieLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val file = cameraOutputFile
            val uri = cameraOutputUri
            val callback = onSelfieCaptured
            onSelfieCaptured = null
            if (result.resultCode == RESULT_OK && file != null && file.exists() && uri != null && callback != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    Utility.compressImageFile(file, 800 * 1024)
                    withContext(Dispatchers.Main) { callback(uri) }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            binding.fcvStep.post { Utility.applyImeOverlapPadding(binding.fcvStep, imeInsets.bottom, mActivity) }
            insets
        }

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
        binding.showProgressSubmit = showProgressSubmit
        binding.showCaptureButton = showCaptureButton

        onBack()
        setupDots()
        setupFilePicker()
        observeStep()
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (showProgressSubmit.get()) return
                if (!viewModel.goToPreviousStep()) {
                    setResult(resultCode)
                    finish()
                }
            }
        })
    }

    private fun setupDots() {
        val size = resources.getDimensionPixelSize(R.dimen.kyc_dot_size)
        val margin = resources.getDimensionPixelSize(R.dimen.kyc_dot_margin)
        repeat(IdentityVerificationViewModel.TOTAL_STEPS) { index ->
            val dot = AppCompatImageView(mActivity).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = if (index == 0) 0 else margin
                }
            }
            dotViews.add(dot)
            binding.llDots.addView(dot)
        }
    }

    private fun observeStep() {
        viewModel.currentStep.observe(this) { step ->
            updateDots(step)
            updateTitle(step)
            showFragment(step)
            val isLastStep = step == IdentityVerificationViewModel.TOTAL_STEPS - 1
            binding.tvContinueLabel.text =
                if (isLastStep) getString(R.string.btnSubmit) else getString(R.string.btnContinue)
            showCaptureButton.set(isLastStep)
        }
    }

    private fun updateDots(step: Int) {
        dotViews.forEachIndexed { index, dot ->
            dot.setImageResource(if (index == step) R.drawable.ic_dot_filled else R.drawable.ic_dot_empty)
        }
    }

    private fun updateTitle(step: Int) {
        binding.tvStepTitle.text = getString(
            when (step) {
                0 -> R.string.titleDetails
                1 -> R.string.titleUploadAadharCard
                2 -> R.string.titleUploadPanCard
                else -> R.string.titleSelfieCapture
            }
        )
    }

    private fun showFragment(step: Int) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fcvStep, stepFragments[step])
            .commit()
    }

    private fun currentFragment(): BaseKycStepFragment? =
        supportFragmentManager.findFragmentById(R.id.fcvStep) as? BaseKycStepFragment

    // ─── Document upload (delegated to by fragments) ───────────────────────────

    private fun setupFilePicker() {
        filePickerUtil = FilePickerUtil(this)
        filePickerUtil.cameraMaxBytes = 800 * 1024
        filePickerUtil.onSuccess =
            { result -> onDocumentPicked?.invoke(result.uri); onDocumentPicked = null }
        filePickerUtil.onError = { error ->
            onDocumentPicked = null
            ToastUtil.showDelete(mActivity, filePickerUtil.getErrorMessage(error))
        }
    }

    fun pickDocument(onPicked: (Uri) -> Unit) {
        onDocumentPicked = onPicked
        filePickerUtil.showSourceChooser(java.io.File(filesDir, "kyc_docs"))
    }

    fun captureSelfie(onCaptured: (Uri) -> Unit) {
        val dir = File(filesDir, "kyc").also { it.mkdirs() }
        val file = File(dir, "selfie_${System.currentTimeMillis()}.jpg")
        val uri =
            FileProvider.getUriForFile(mActivity, "${mActivity.packageName}.fileprovider", file)
        cameraOutputFile = file
        cameraOutputUri = uri
        onSelfieCaptured = onCaptured
        selfieLauncher.launch(SelfieCaptureActivity.buildIntent(mActivity, file))
    }

    // ─── Navigation ─────────────────────────────────────────────────────────────

    private fun onContinueClicked() {
        Utility.hideKeyboard(binding.clRoot)
        val fragment = currentFragment() ?: return
        if (!fragment.validate()) return

        val step = viewModel.currentStep.value ?: 0
        if (step == IdentityVerificationViewModel.TOTAL_STEPS - 1) {
            submitIdentity()
        } else {
            viewModel.goToNextStep()
        }
    }

    private fun submitIdentity() {
        if (!Utility.isInternetAvailable(mActivity)) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
            return
        }

        val panUri = viewModel.panFrontUri
        val aadhaarFrontUri = viewModel.aadhaarFrontUri
        val aadhaarBackUri = viewModel.aadhaarBackUri
        val selfieUri = viewModel.selfieUri

        showProgressSubmit.set(true)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cr = contentResolver
                val (panBytes, aadhaarFrontBytes, aadhaarBackBytes, selfieBytes) = coroutineScope {
                    listOf(
                        async { panUri?.let { cr.openInputStream(it)?.use { s -> s.readBytes() } } },
                        async { aadhaarFrontUri?.let { cr.openInputStream(it)?.use { s -> s.readBytes() } } },
                        async { aadhaarBackUri?.let { cr.openInputStream(it)?.use { s -> s.readBytes() } } },
                        async { selfieUri?.let { cr.openInputStream(it)?.use { s -> s.readBytes() } } }
                    ).awaitAll()
                }

                withContext(Dispatchers.Main) {
                    if (panBytes == null || aadhaarFrontBytes == null || aadhaarBackBytes == null || selfieBytes == null) {
                        showProgressSubmit.set(false)
                        ToastUtil.showDelete(mActivity, getString(R.string.errGeneric))
                        return@withContext
                    }
                    viewModel.submitIdentity(
                        panBytes = panBytes,
                        aadhaarFrontBytes = aadhaarFrontBytes,
                        aadhaarBackBytes = aadhaarBackBytes,
                        selfieBytes = selfieBytes,
                        onSuccess = {
                            showProgressSubmit.set(false)
                            ToastUtil.showSuccess(
                                mActivity,
                                getString(R.string.msgIdentitySubmitSuccess)
                            )
                            resultCode = 1
                            setResult(resultCode)
                            finish()
                        },
                        onError = { msg ->
                            showProgressSubmit.set(false); ToastUtil.showDelete(
                            mActivity,
                            msg
                        )
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showProgressSubmit.set(false)
                    ToastUtil.showDelete(
                        mActivity,
                        e.localizedMessage ?: getString(R.string.errGeneric)
                    )
                }
            }
        }
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.lytToolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressSubmit.get()) return@OnClickListener
                    onBackPressedDispatcher.onBackPressed()
                }

                binding.btnCapture -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressSubmit.get()) return@OnClickListener
                    (currentFragment() as? KycStep4Fragment)?.triggerCapture()
                }

                binding.btnContinue -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressSubmit.get()) return@OnClickListener
                    onContinueClicked()
                }
            }
        }
    }
}
