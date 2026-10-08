package com.shreefintech.paytouchconsumer.myaccount

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityEditBankDetailsBinding
import com.shreefintech.paytouchconsumer.kyc.bank.model.EditBankPassItem
import com.shreefintech.paytouchconsumer.utill.FilePickerUtil
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.PdfThumbnailRepository
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.graphics.createBitmap

class EditBankDetailsActivity : BaseActivity() {

    companion object {
        private const val EXTRA_BANK = "extra_bank"
        const val EXTRA_SUCCESS_MSG = "extra_success_msg"

        private val IFSC_REGEX = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")

        fun start(context: Context, item: EditBankPassItem): Intent =
            Intent(context, EditBankDetailsActivity::class.java).apply {
                putExtra(EXTRA_BANK, Gson().toJson(item))
            }
    }

    private lateinit var binding: ActivityEditBankDetailsBinding
    private val viewModel: EditBankDetailsViewModel by viewModels()

    private val showProgressSave = ObservableBoolean(false)
    private var isSaving = false
    private var isPasswordVisible = false

    private lateinit var filePickerUtil: FilePickerUtil
    private var proofUri: Uri? = null
    private var initialProofUrl: String? = null
    private var thumbnailJob: Job? = null

    private val bankItem: EditBankPassItem? by lazy {
        intent.getStringExtra(EXTRA_BANK)?.let { Gson().fromJson(it, EditBankPassItem::class.java) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBankDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left, systemBars.top, systemBars.right,
                maxOf(imeInsets.bottom, systemBars.bottom)
            )
            insets
        }

        LiquidGlassEffect.attach(
            targetView   = binding.flCard,
            rootView     = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion   = 0f,
            strokeWidth  = 1,
            strokeColor  = ContextCompat.getColor(mActivity, R.color.primary),
            solidStroke  = true,
            blur         = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )
        binding.flUpload1.attach(binding.clRoot as ViewGroup)
        attachEditDeleteGlass(binding.flEdit1)
        attachEditDeleteGlass(binding.flDelete1)

        val listener = onClickListener()
        binding.onClickListener  = listener
        binding.showProgressSave = showProgressSave
        binding.flUpload1.setOnClickListener(listener)
        binding.flBankProof.setOnClickListener(listener)
        binding.ivEditProof.setOnClickListener(listener)
        binding.ivDeleteProof.setOnClickListener(listener)
        binding.ivEyePassword.setOnClickListener(listener)

        setupInputFilters()
        setupFilePicker()
        onBack()
        prefillForm()
    }

    private fun attachEditDeleteGlass(targetView: View) {
        LiquidGlassEffect.attach(
            targetView   = targetView,
            rootView     = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.filter_btn_radius),
            distortion   = 0f,
            blur         = resources.getDimensionPixelSize(R.dimen.filter_btn_blure),
            tintColor    = ContextCompat.getColor(mActivity, R.color.filter_bg)
        )
    }

    private fun setupInputFilters() {
        val emojiFilter = Utility.EmojiExcludeFilter()
        val upperCaseFilter = InputFilter { source, start, end, _, _, _ ->
            source.subSequence(start, end).toString().uppercase()
        }
        binding.etAccountNumber.filters = arrayOf(InputFilter.LengthFilter(18), Utility.digitFilter(), emojiFilter)
        binding.etBankName.filters      = arrayOf(InputFilter.LengthFilter(50), emojiFilter)
        binding.etIfscCode.filters      = arrayOf(InputFilter.LengthFilter(11), upperCaseFilter, emojiFilter)
        binding.etBranchName.filters    = arrayOf(InputFilter.LengthFilter(50), emojiFilter)
        binding.etAccountHolderName.filters = arrayOf(InputFilter.LengthFilter(100), emojiFilter)
    }

    private fun setupFilePicker() {
        filePickerUtil = FilePickerUtil(this)
        filePickerUtil.onSuccess = { result -> applyProof(result.uri, result.extension) }
        filePickerUtil.onError   = { error ->
            ToastUtil.showDelete(
                mActivity,
                filePickerUtil.getErrorMessage(error),
                inWindow = error != FilePickerUtil.FilePickerError.CameraPermissionBlocked
            )
        }
    }

    private fun prefillForm() {
        val item = bankItem ?: return

        binding.etAccountHolderName.setText(item.accountHolderName ?: "")

        // Leave masked account number empty so the user types it fresh
        val accNum = item.accountNumber ?: ""
        binding.etAccountNumber.setText(if (accNum.any { !it.isDigit() }) "" else accNum)

        binding.etBankName.setText(item.bankName ?: "")
        binding.etIfscCode.setText(item.ifsc ?: "")
        binding.etBranchName.setText(item.branchName ?: "")

        if (item.isPrimary) {
            binding.cvWithdrawalNote.isVisible = true
        }

        initialProofUrl = item.bankProofUrl
        if (!initialProofUrl.isNullOrBlank()) {
            showProofPreview(loadUrl = initialProofUrl)
        }
    }

    private fun applyProof(uri: Uri, extension: String) {
        proofUri = uri
        showProofPreview(loadUri = uri, localIsPdf = extension.equals("pdf", ignoreCase = true))
    }

    private fun showProofPreview(
        loadUrl: String? = null,
        loadUri: Uri? = null,
        localIsPdf: Boolean = false
    ) {
        binding.llUploadProof.visibility     = View.GONE
        binding.pbProofLoading.visibility    = View.GONE
        binding.ivPreviewProof.visibility    = View.VISIBLE
        binding.llEditDeleteProof.visibility = View.VISIBLE

        thumbnailJob?.cancel()
        thumbnailJob = null

        if (loadUri != null) {
            // Freshly picked local file — trust the validated extension from FilePickerUtil;
            // fall back to ContentResolver MIME type as a secondary signal
            val isPdf = localIsPdf || contentResolver.getType(loadUri) == "application/pdf"
            if (isPdf) {
                binding.ivPreviewProof.setImageResource(R.drawable.ic_file_not_found)
                lifecycleScope.launch(Dispatchers.IO) {
                    val bmp = renderLocalPdf(loadUri)
                    withContext(Dispatchers.Main) {
                        if (isDestroyed || isFinishing) return@withContext
                        if (bmp != null) binding.ivPreviewProof.setImageBitmap(bmp)
                    }
                }
            } else {
                Glide.with(mActivity)
                    .load(loadUri)
                    .placeholder(R.drawable.ic_file_not_found)
                    .error(R.drawable.ic_file_not_found)
                    .into(binding.ivPreviewProof)
            }
        } else if (!loadUrl.isNullOrBlank()) {
            if (isPdfUrl(loadUrl)) {
                binding.pbProofLoading.visibility = View.VISIBLE
                binding.ivPreviewProof.visibility = View.INVISIBLE
                thumbnailJob = PdfThumbnailRepository.loadThumbnail(loadUrl, cacheDir, SharedPreferenceHelper.bearerToken(mActivity)) { bitmap ->
                    if (isDestroyed || isFinishing) return@loadThumbnail
                    binding.pbProofLoading.visibility = View.GONE
                    binding.ivPreviewProof.visibility = View.VISIBLE
                    if (bitmap != null) binding.ivPreviewProof.setImageBitmap(bitmap)
                    else binding.ivPreviewProof.setImageResource(R.drawable.ic_file_not_found)
                }
            } else {
                Glide.with(binding.ivPreviewProof)
                    .load(loadUrl)
                    .placeholder(R.drawable.ic_file_not_found)
                    .error(R.drawable.ic_file_not_found)
                    .centerCrop()
                    .into(binding.ivPreviewProof)
            }
        }
    }

    private fun renderLocalPdf(uri: Uri): Bitmap? {
        return try {
            val tempFile = File(filesDir, "edit_bank_proof_preview.pdf")
            tempFile.delete()
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (!tempFile.exists() || tempFile.length() == 0L) return null
            val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            try {
                val page = renderer.openPage(0)
                page.use { page ->
                    if (page.width <= 0 || page.height <= 0) return null
                    val scale = 800f / page.width
                    val bmp = createBitmap(800, (page.height * scale).toInt())
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            } finally {
                renderer.close()
                fd.close()
            }
        } catch (e: Exception) {
            Utility.logError(e)
            null
        }
    }

    private fun isPdfUrl(url: String) = url.lowercase().let {
        it.endsWith(".pdf") || it.contains(".pdf?") || it.contains("type=pdf")
    }

    private fun clearProofToUploadState() {
        thumbnailJob?.cancel()
        thumbnailJob = null
        proofUri = null
        Glide.with(mActivity).clear(binding.ivPreviewProof)
        binding.pbProofLoading.visibility    = View.GONE
        binding.ivPreviewProof.visibility    = View.GONE
        binding.llEditDeleteProof.visibility = View.GONE
        binding.llUploadProof.visibility     = View.VISIBLE
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        binding.etPassword.inputType = if (isPasswordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        binding.ivEyePassword.setImageResource(
            if (isPasswordVisible) R.drawable.ic_eye_on else R.drawable.ic_eye_off
        )
    }

    private fun validate(): Boolean {
        Utility.hideKeyboard(mActivity)
        val holderName    = binding.etAccountHolderName.text?.toString()?.trim() ?: ""
        val accountNumber = binding.etAccountNumber.text?.toString()?.trim()     ?: ""
        val ifsc          = binding.etIfscCode.text?.toString()?.trim()           ?: ""
        val bankName      = binding.etBankName.text?.toString()?.trim()           ?: ""
        val branchName    = binding.etBranchName.text?.toString()?.trim()         ?: ""
        val password      = binding.etPassword.text?.toString()                   ?: ""

        return when {
            holderName.isEmpty() -> {
                binding.etAccountHolderName.requestFocus()
                ToastUtil.showDelete(mActivity, getString(R.string.msgAccountHolderNameEmpty))
                false
            }
            accountNumber.isEmpty() -> {
                binding.etAccountNumber.requestFocus()
                ToastUtil.showDelete(mActivity, getString(R.string.msgAccountNumberEmpty))
                false
            }
            accountNumber.length !in 9..18 -> {
                binding.etAccountNumber.requestFocus()
                ToastUtil.showDelete(mActivity, getString(R.string.msgAccountNumberRange))
                false
            }
            ifsc.isEmpty() -> {
                binding.etIfscCode.requestFocus()
                ToastUtil.showDelete(mActivity, getString(R.string.msgIfscEmpty))
                false
            }
            !IFSC_REGEX.matches(ifsc) -> {
                binding.etIfscCode.requestFocus()
                ToastUtil.showDelete(mActivity, getString(R.string.msgIfscInvalid))
                false
            }
            bankName.isEmpty() -> {
                binding.etBankName.requestFocus()
                ToastUtil.showDelete(mActivity, getString(R.string.msgBankNameEmpty))
                false
            }
            branchName.isEmpty() -> {
                binding.etBranchName.requestFocus()
                ToastUtil.showDelete(mActivity, getString(R.string.msgBranchNameEmpty))
                false
            }
            password.isEmpty() -> {
                binding.etPassword.requestFocus()
                ToastUtil.showDelete(mActivity, getString(R.string.msgPasswordEmpty))
                false
            }
            else -> true
        }
    }

    private fun onSubmit() {
        if (!validate()) return
        val item = bankItem ?: return

        // Snapshot all form values on the main thread before going to IO
        val holderNameSnap    = binding.etAccountHolderName.text?.toString()?.trim() ?: ""
        val accountNumberSnap = binding.etAccountNumber.text?.toString()?.trim()     ?: ""
        val ifscSnap          = binding.etIfscCode.text?.toString()?.trim()           ?: ""
        val bankNameSnap      = binding.etBankName.text?.toString()?.trim()           ?: ""
        val branchNameSnap    = binding.etBranchName.text?.toString()?.trim()         ?: ""
        val passwordSnap      = binding.etPassword.text?.toString()                   ?: ""
        val capturedUri       = proofUri

        isSaving = true
        showProgressSave.set(true)

        lifecycleScope.launch(Dispatchers.IO) {
            val proofMimeType = capturedUri?.let { contentResolver.getType(it) }
            val proofBytes = capturedUri?.let { uri ->
                try { contentResolver.openInputStream(uri)?.use { it.readBytes() } } catch (e: Exception) { Utility.logError(e); null }
            }
            withContext(Dispatchers.Main) {
                if (isDestroyed || isFinishing) return@withContext
                viewModel.submit(
                    bankId              = item.bankId,
                    accountHolderName   = holderNameSnap,
                    accountNumber       = accountNumberSnap,
                    ifsc                = ifscSnap,
                    bankName            = bankNameSnap,
                    branchName          = branchNameSnap,
                    password            = passwordSnap,
                    proofBytes          = proofBytes,
                    proofMimeType       = proofMimeType,
                    onSuccess           = { msg ->
                        isSaving = false
                        showProgressSave.set(false)
                        val resultIntent = Intent().putExtra(EXTRA_SUCCESS_MSG, msg)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onError             = { msg ->
                        isSaving = false
                        showProgressSave.set(false)
                        ToastUtil.showDelete(mActivity, msg)
                    }
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
                binding.llSubmit -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressSave.get()) return@OnClickListener
                    onSubmit()
                }
                binding.flUpload1, binding.flBankProof, binding.ivEditProof -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // flBankProof catches border taps — only open picker when in upload state
                    if (view == binding.flBankProof && !binding.llUploadProof.isVisible) return@OnClickListener
                    filePickerUtil.showSourceChooser(File(filesDir, Constant.KYC_BANK_DOCS_DIR))
                }
                binding.ivDeleteProof -> {
                    if (Utility.stopClick()) return@OnClickListener
                    clearProofToUploadState()
                }
                binding.ivEyePassword -> {
                    if (Utility.stopClick()) return@OnClickListener
                    togglePasswordVisibility()
                }
            }
        }
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSaving) return
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

}
