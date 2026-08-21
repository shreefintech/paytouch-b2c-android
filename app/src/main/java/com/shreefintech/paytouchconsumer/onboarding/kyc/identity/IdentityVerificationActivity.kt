package com.shreefintech.paytouchconsumer.onboarding.kyc.identity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.shreefintech.paytouchconsumer.BaseActivity
import java.io.ByteArrayOutputStream
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityIdentityVerificationBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.onboarding.kyc.identity.fragment.BaseKycStepFragment
import com.shreefintech.paytouchconsumer.onboarding.kyc.identity.fragment.KycStep1Fragment
import com.shreefintech.paytouchconsumer.onboarding.kyc.identity.fragment.KycStep2Fragment
import com.shreefintech.paytouchconsumer.onboarding.kyc.identity.fragment.KycStep3Fragment
import com.shreefintech.paytouchconsumer.onboarding.kyc.identity.fragment.KycStep4Fragment
import com.shreefintech.paytouchconsumer.utill.FilePickerUtil
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import java.io.File

class IdentityVerificationActivity : BaseActivity() {

    companion object {
        fun buildIntent(context: android.content.Context): android.content.Intent =
            android.content.Intent(context, IdentityVerificationActivity::class.java)
    }

    private lateinit var binding: ActivityIdentityVerificationBinding
    private val viewModel: IdentityVerificationViewModel by viewModels()
    private var showProgressSubmit = ObservableBoolean(false)
    private var resultCode = 0

    private val dotViews = mutableListOf<AppCompatImageView>()
    private val stepFragments by lazy {
        listOf(KycStep1Fragment(), KycStep2Fragment(), KycStep3Fragment(), KycStep4Fragment())
    }

    // ─── Document picker — owned by the host so it survives fragment re-creation ──
    private lateinit var filePickerUtil: FilePickerUtil
    private var onDocumentPicked: ((Uri) -> Unit)? = null

    // ─── Selfie capture via system camera ──────────────────────────────────────
    private var cameraOutputFile: File? = null
    private var cameraOutputUri: Uri? = null
    private var onSelfieCaptured: ((Uri) -> Unit)? = null
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = cameraOutputFile
        val uri = cameraOutputUri
        val callback = onSelfieCaptured
        onSelfieCaptured = null
        if (success && file != null && uri != null && callback != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                compressIfNeeded(file)
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
            val imeInsets  = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(imeInsets.bottom, systemBars.bottom)
            )
            insets
        }

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

        binding.onClickListener    = onClickListener()
        binding.showProgressSubmit = showProgressSubmit

        onBack()
        setupDots()
        setupFilePicker()
        observeStep()
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.goToPreviousStep()) {
                    setResult(resultCode)
                    finish()
                }
            }
        })
    }

    private fun setupDots() {
        val size   = resources.getDimensionPixelSize(R.dimen.kyc_dot_size)
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
            binding.tvContinueLabel.text = if (step == IdentityVerificationViewModel.TOTAL_STEPS - 1) {
                getString(R.string.btnSubmit)
            } else {
                getString(R.string.btnContinue)
            }
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
        filePickerUtil.onSuccess = { result -> onDocumentPicked?.invoke(result.uri); onDocumentPicked = null }
        filePickerUtil.onError   = { error ->
            onDocumentPicked = null
            ToastUtil.showDelete(mActivity, filePickerUtil.getErrorMessage(error))
        }
    }

    fun pickDocument(onPicked: (Uri) -> Unit) {
        onDocumentPicked = onPicked
        filePickerUtil.openPicker()
    }

    fun captureSelfie(onCaptured: (Uri) -> Unit) {
        val dir = File(cacheDir, "kyc").also { it.mkdirs() }
        val file = File(dir, "selfie_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(mActivity, "${mActivity.packageName}.fileprovider", file)
        cameraOutputFile = file
        cameraOutputUri = uri
        onSelfieCaptured = onCaptured
        cameraLauncher.launch(uri)
    }

    private fun compressIfNeeded(file: File) {
        val maxBytes = 2 * 1024 * 1024
        val needsCompress = file.length() > maxBytes
        val rotation = exifRotation(file.absolutePath)
        if (!needsCompress && rotation == 0) return

        val raw = BitmapFactory.decodeFile(file.absolutePath) ?: return
        val bitmap = if (rotation != 0) {
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
                .also { if (it !== raw) raw.recycle() }
        } else raw

        var quality = if (needsCompress) 85 else 95
        while (true) {
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
            val bytes = bos.toByteArray()
            if (bytes.size <= maxBytes || quality == 40) {
                file.writeBytes(bytes)
                break
            }
            quality -= 15
        }
        bitmap.recycle()
    }

    private fun exifRotation(path: String): Int = try {
        when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90  -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) { 0 }

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

        val panUri          = viewModel.panFrontUri
        val aadhaarFrontUri = viewModel.aadhaarFrontUri
        val aadhaarBackUri  = viewModel.aadhaarBackUri
        val selfieUri       = viewModel.selfieUri

        showProgressSubmit.set(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val cr            = contentResolver
            val panBytes          = panUri?.let { cr.openInputStream(it)?.use { s -> s.readBytes() } }
            val aadhaarFrontBytes = aadhaarFrontUri?.let { cr.openInputStream(it)?.use { s -> s.readBytes() } }
            val aadhaarBackBytes  = aadhaarBackUri?.let { cr.openInputStream(it)?.use { s -> s.readBytes() } }
            val selfieBytes       = selfieUri?.let { cr.openInputStream(it)?.use { s -> s.readBytes() } }

            withContext(Dispatchers.Main) {
                if (panBytes == null || aadhaarFrontBytes == null || aadhaarBackBytes == null || selfieBytes == null) {
                    showProgressSubmit.set(false)
                    ToastUtil.showDelete(mActivity, getString(R.string.errGeneric))
                    return@withContext
                }
                viewModel.submitIdentity(
                    panBytes          = panBytes,
                    aadhaarFrontBytes = aadhaarFrontBytes,
                    aadhaarBackBytes  = aadhaarBackBytes,
                    selfieBytes       = selfieBytes,
                    onSuccess = {
                        showProgressSubmit.set(false)
                        ToastUtil.showSuccess(mActivity, getString(R.string.msgIdentitySubmitSuccess))
                        resultCode = 1
                        setResult(resultCode)
                        finish()
                    },
                    onError = { msg -> showProgressSubmit.set(false); ToastUtil.showDelete(mActivity, msg) }
                )
            }
        }
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.lytToolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBackPressedDispatcher.onBackPressed()
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
