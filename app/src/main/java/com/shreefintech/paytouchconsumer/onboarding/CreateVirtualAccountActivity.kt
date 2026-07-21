package com.shreefintech.paytouchconsumer.onboarding

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.onboarding.viewmodel.CreateVirtualAccountViewModel
import com.shreefintech.paytouchconsumer.databinding.ActivityCreateVirtualAccountBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.FilePickerUtil
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.widget.CustomDropdown
import com.shreefintech.paytouchconsumer.widget.LiquidGlassButton

class CreateVirtualAccountActivity : BaseActivity() {

    companion object {
        private val PAN_REGEX  = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")
        private val IFSC_REGEX = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")
        private val VPA_REGEX  = Regex("^[\\w.-]{2,100}@[a-zA-Z]{2,64}$")
    }

    private data class ProofSlot(
        val preview: AppCompatImageView,
        val progress: View,
        val uploadBtn: LiquidGlassButton,
        val editRow: View,
        val deleteRow: View
    )

    private lateinit var binding: ActivityCreateVirtualAccountBinding
    private val viewModel: CreateVirtualAccountViewModel by viewModels()
    private var showProgress = ObservableBoolean(false)

    private lateinit var filePickerUtil: FilePickerUtil
    private var activeUploadSlot = 0
    private val uploadUris = arrayOfNulls<Uri>(4)

    private var selectedState: String? = null
    private var selectedCity: String? = null
    private var selectedDistrict: String? = null

    private val stateList = listOf(
        "Maharashtra", "Delhi", "Karnataka", "Telangana", "Tamil Nadu",
        "West Bengal", "Gujarat", "Rajasthan", "Uttar Pradesh", "Punjab"
    )
    private val cityList = listOf(
        "Mumbai", "Delhi", "Bengaluru", "Hyderabad", "Ahmedabad",
        "Chennai", "Kolkata", "Pune", "Jaipur", "Surat"
    )
    private val districtList = listOf(
        "Central", "North", "South", "East", "West",
        "Andheri", "Borivali", "Thane", "Pune City", "Nagpur"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateVirtualAccountBinding.inflate(layoutInflater)
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
            blur         = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        binding.onClickListener = onClickListener()
        binding.showProgress    = showProgress

        binding.flUpload1.attach(binding.clRoot as ViewGroup)
        binding.flUpload2.attach(binding.clRoot as ViewGroup)
        binding.flUpload3.attach(binding.clRoot as ViewGroup)
        binding.flUpload4.attach(binding.clRoot as ViewGroup)

        attachEditDeleteGlass(binding.flEdit1)
        attachEditDeleteGlass(binding.flDelete1)
        attachEditDeleteGlass(binding.flEdit2)
        attachEditDeleteGlass(binding.flDelete2)
        attachEditDeleteGlass(binding.flEdit3)
        attachEditDeleteGlass(binding.flDelete3)
        attachEditDeleteGlass(binding.flEdit4)
        attachEditDeleteGlass(binding.flDelete4)

        onBack()
        setupInputFilters()
        setupFilePicker()
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

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })
    }

    private fun setupInputFilters() {
        val emojiFilter     = Utility.EmojiExcludeFilter()
        val upperCaseFilter = InputFilter { source, start, end, _, _, _ ->
            source.subSequence(start, end).toString().uppercase()
        }

        binding.etFullName.filters    = arrayOf(InputFilter.LengthFilter(50), Utility.alphaSpaceFilter(), emojiFilter)
        binding.etMobile.filters      = arrayOf(InputFilter.LengthFilter(10), Utility.digitFilter(), emojiFilter)
        binding.etPan.filters         = arrayOf(InputFilter.LengthFilter(10), upperCaseFilter, emojiFilter)
        binding.etAadhar.filters      = arrayOf(InputFilter.LengthFilter(12), Utility.digitFilter(), emojiFilter)
        binding.etIfsc.filters        = arrayOf(InputFilter.LengthFilter(11), upperCaseFilter, emojiFilter)
        binding.etBankAccount.filters = arrayOf(InputFilter.LengthFilter(18), Utility.digitFilter(), emojiFilter)
        binding.etVpa.filters         = arrayOf(InputFilter.LengthFilter(100), emojiFilter)
        binding.etBranchName.filters  = arrayOf(InputFilter.LengthFilter(50), emojiFilter)
    }

    // ─── Dropdowns ────────────────────────────────────────────────────────────

    private fun showStateDropdown() {
        Utility.hideKeyboard(binding.clRoot)
        CustomDropdown.showDropdown(
            activity   = mActivity,
            anchorView = binding.flStateAnchor,
            arrowView  = binding.ivStateArrow,
            textView   = binding.tvState,
            items      = stateList
        ) { selected, _ -> selectedState = selected }
    }

    private fun showCityDropdown() {
        Utility.hideKeyboard(binding.clRoot)
        CustomDropdown.showDropdown(
            activity   = mActivity,
            anchorView = binding.flCityAnchor,
            arrowView  = binding.ivCityArrow,
            textView   = binding.tvCity,
            items      = cityList
        ) { selected, _ -> selectedCity = selected }
    }

    private fun showDistrictDropdown() {
        Utility.hideKeyboard(binding.clRoot)
        CustomDropdown.showDropdown(
            activity   = mActivity,
            anchorView = binding.flDistrictAnchor,
            arrowView  = binding.ivDistrictArrow,
            textView   = binding.tvDistrict,
            items      = districtList
        ) { selected, _ -> selectedDistrict = selected }
    }

    // ─── Document upload ──────────────────────────────────────────────────────

    private fun setupFilePicker() {
        filePickerUtil = FilePickerUtil(this)
        filePickerUtil.onSuccess = { result -> applyFileToActiveSlot(result.uri) }
        filePickerUtil.onError   = { error ->
            ToastUtil.showDelete(mActivity, filePickerUtil.getErrorMessage(error))
        }
    }

    private fun proofSlot(slot: Int): ProofSlot = when (slot) {
        1 -> ProofSlot(binding.ivPreview1, binding.prgPreview1, binding.flUpload1, binding.flEdit1, binding.flDelete1)
        2 -> ProofSlot(binding.ivPreview2, binding.prgPreview2, binding.flUpload2, binding.flEdit2, binding.flDelete2)
        3 -> ProofSlot(binding.ivPreview3, binding.prgPreview3, binding.flUpload3, binding.flEdit3, binding.flDelete3)
        else -> ProofSlot(binding.ivPreview4, binding.prgPreview4, binding.flUpload4, binding.flEdit4, binding.flDelete4)
    }

    private fun openPickerForSlot(slot: Int) {
        activeUploadSlot = slot
        filePickerUtil.openPicker()
    }

    private fun applyFileToActiveSlot(uri: Uri) {
        val slot = activeUploadSlot
        if (slot !in 1..4) return
        uploadUris[slot - 1] = uri

        val views = proofSlot(slot)
        views.progress.visibility = View.VISIBLE
        views.uploadBtn.visibility = View.GONE

        Glide.with(mActivity)
            .load(uri)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean
                ): Boolean {
                    views.progress.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable, model: Any, target: Target<Drawable>?,
                    dataSource: DataSource, isFirstResource: Boolean
                ): Boolean {
                    views.progress.visibility = View.GONE
                    return false
                }
            })
            .into(views.preview)

        views.preview.visibility = View.VISIBLE
        views.editRow.visibility = View.VISIBLE
        views.deleteRow.visibility = View.VISIBLE
    }

    private fun clearSlot(slot: Int) {
        uploadUris[slot - 1] = null
        val views = proofSlot(slot)
        Glide.with(mActivity).clear(views.preview)
        views.preview.visibility = View.GONE
        views.editRow.visibility = View.GONE
        views.deleteRow.visibility = View.GONE
        views.progress.visibility = View.GONE
        views.uploadBtn.visibility = View.VISIBLE
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    private fun validate(): Boolean {
        Utility.hideKeyboard(binding.clRoot)
        val msg = validatePersonalInfo()
            ?: validateIdentityDocs()
            ?: validateBankDetails()
            ?: validateDocuments()
        if (msg != null) { ToastUtil.showDelete(mActivity, msg); return false }
        return true
    }

    private fun validatePersonalInfo(): String? {
        val fullName = binding.etFullName.text?.toString()?.trim() ?: ""
        val mobile   = binding.etMobile.text?.toString()?.trim()   ?: ""
        return when {
            fullName.isEmpty()  -> { binding.etFullName.requestFocus(); getString(R.string.msgFullNameEmpty) }
            mobile.isEmpty()    -> { binding.etMobile.requestFocus(); getString(R.string.msgMobileEmpty) }
            mobile.length != 10 -> { binding.etMobile.requestFocus(); getString(R.string.msgMobileInvalid) }
            selectedState.isNullOrEmpty()    -> getString(R.string.msgStateEmpty)
            selectedCity.isNullOrEmpty()     -> getString(R.string.msgCityEmpty)
            selectedDistrict.isNullOrEmpty() -> getString(R.string.msgDistrictEmpty)
            else -> null
        }
    }

    private fun validateIdentityDocs(): String? {
        val pan    = binding.etPan.text?.toString()?.trim()    ?: ""
        val aadhar = binding.etAadhar.text?.toString()?.trim() ?: ""
        return when {
            pan.isEmpty()           -> { binding.etPan.requestFocus(); getString(R.string.msgPanEmpty) }
            !PAN_REGEX.matches(pan) -> { binding.etPan.requestFocus(); getString(R.string.msgPanInvalid) }
            aadhar.isEmpty()        -> { binding.etAadhar.requestFocus(); getString(R.string.msgAadharEmpty) }
            aadhar.length != 12     -> { binding.etAadhar.requestFocus(); getString(R.string.msgAadharInvalid) }
            else -> null
        }
    }

    private fun validateBankDetails(): String? {
        val ifsc        = binding.etIfsc.text?.toString()?.trim()        ?: ""
        val bankAccount = binding.etBankAccount.text?.toString()?.trim() ?: ""
        val vpa         = binding.etVpa.text?.toString()?.trim()         ?: ""
        val branchName  = binding.etBranchName.text?.toString()?.trim()  ?: ""
        return when {
            ifsc.isEmpty()             -> { binding.etIfsc.requestFocus(); getString(R.string.msgIfscEmpty) }
            !IFSC_REGEX.matches(ifsc)  -> { binding.etIfsc.requestFocus(); getString(R.string.msgIfscInvalid) }
            bankAccount.isEmpty()      -> { binding.etBankAccount.requestFocus(); getString(R.string.msgBankAccountEmpty) }
            bankAccount.length !in 9..18 -> { binding.etBankAccount.requestFocus(); getString(R.string.msgBankAccountInvalid) }
            vpa.isEmpty()              -> { binding.etVpa.requestFocus(); getString(R.string.msgVpaEmpty) }
            !VPA_REGEX.matches(vpa)    -> { binding.etVpa.requestFocus(); getString(R.string.msgVpaInvalid) }
            branchName.isEmpty()       -> { binding.etBranchName.requestFocus(); getString(R.string.msgBranchNameEmpty) }
            else -> null
        }
    }

    private fun validateDocuments(): String? = when {
        uploadUris[0] == null -> getString(R.string.msgDocumentRequired, getString(R.string.labelAadharFront))
        uploadUris[1] == null -> getString(R.string.msgDocumentRequired, getString(R.string.labelAadharBack))
        uploadUris[2] == null -> getString(R.string.msgDocumentRequired, getString(R.string.labelPanUpload))
        uploadUris[3] == null -> getString(R.string.msgDocumentRequired, getString(R.string.labelProof))
        else -> null
    }

    private fun onSubmit() {
        // TODO(PAYTOUCH-514): re-enable validate() once virtual account API is wired
//        if (!validate()) return

        val aadharFrontUri = uploadUris[0] ?: run {
            ToastUtil.showDelete(mActivity, getString(R.string.msgDocumentRequired, getString(R.string.labelAadharFront)))
            return
        }
        val aadharBackUri = uploadUris[1] ?: run {
            ToastUtil.showDelete(mActivity, getString(R.string.msgDocumentRequired, getString(R.string.labelAadharBack)))
            return
        }
        val panUri = uploadUris[2] ?: run {
            ToastUtil.showDelete(mActivity, getString(R.string.msgDocumentRequired, getString(R.string.labelPanUpload)))
            return
        }
        val proofUri = uploadUris[3] ?: run {
            ToastUtil.showDelete(mActivity, getString(R.string.msgDocumentRequired, getString(R.string.labelProof)))
            return
        }

        viewModel.submitVirtualAccount(
            fullName       = binding.etFullName.text?.toString()?.trim()    ?: "",
            mobile         = binding.etMobile.text?.toString()?.trim()      ?: "",
            state          = selectedState ?: "",
            city           = selectedCity ?: "",
            district       = selectedDistrict ?: "",
            panNumber      = binding.etPan.text?.toString()?.trim()         ?: "",
            aadharNumber   = binding.etAadhar.text?.toString()?.trim()      ?: "",
            ifscCode       = binding.etIfsc.text?.toString()?.trim()        ?: "",
            bankAccount    = binding.etBankAccount.text?.toString()?.trim() ?: "",
            vpa            = binding.etVpa.text?.toString()?.trim()         ?: "",
            branchName     = binding.etBranchName.text?.toString()?.trim() ?: "",
            aadharFrontUri = aadharFrontUri,
            aadharBackUri  = aadharBackUri,
            panUri         = panUri,
            proofUri       = proofUri,
            onLoading      = { showProgress.set(true) },
            onSuccess      = {
                showProgress.set(false)
                ToastUtil.showSuccess(mActivity, getString(R.string.msgVaSubmitSuccess))
                finish()
            },
            onError        = { msg -> showProgress.set(false); ToastUtil.showDelete(mActivity, msg) }
        )
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.lytToolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBackPressedDispatcher.onBackPressed()
                }

                binding.flStateAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showStateDropdown()
                }

                binding.flCityAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showCityDropdown()
                }

                binding.flDistrictAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showDistrictDropdown()
                }

                binding.flUpload1, binding.ivEditProof1 -> {
                    if (Utility.stopClick()) return@OnClickListener
                    openPickerForSlot(1)
                }
                binding.ivDeleteProof1 -> {
                    if (Utility.stopClick()) return@OnClickListener
                    clearSlot(1)
                }

                binding.flUpload2, binding.ivEditProof2 -> {
                    if (Utility.stopClick()) return@OnClickListener
                    openPickerForSlot(2)
                }
                binding.ivDeleteProof2 -> {
                    if (Utility.stopClick()) return@OnClickListener
                    clearSlot(2)
                }

                binding.flUpload3, binding.ivEditProof3 -> {
                    if (Utility.stopClick()) return@OnClickListener
                    openPickerForSlot(3)
                }
                binding.ivDeleteProof3 -> {
                    if (Utility.stopClick()) return@OnClickListener
                    clearSlot(3)
                }

                binding.flUpload4, binding.ivEditProof4 -> {
                    if (Utility.stopClick()) return@OnClickListener
                    openPickerForSlot(4)
                }
                binding.ivDeleteProof4 -> {
                    if (Utility.stopClick()) return@OnClickListener
                    clearSlot(4)
                }

                binding.llCreateVirtualAccount -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onSubmit()
                }
            }
        }
    }
}