package com.shreefintech.paytouchconsumer.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.auth.viewmodel.CreateAccountV2ViewModel
import com.shreefintech.paytouchconsumer.databinding.ActivityCreateAccountV2Binding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.retrofit.model.CityItem
import com.shreefintech.paytouchconsumer.retrofit.model.StateItem
import com.shreefintech.paytouchconsumer.utill.FilePickerUtil
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.widget.CustomDropdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CreateAccountV2Activity : BaseActivity() {

    private lateinit var binding: ActivityCreateAccountV2Binding
    private val viewModel: CreateAccountV2ViewModel by viewModels()

    private val showProgress = ObservableBoolean(false)
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    private var aadhaarUri: Uri? = null
    private val aadhaarDir by lazy { File(filesDir, Constant.REG_AADHAAR_DIR) }
    private var stateList = listOf<StateItem>()
    private var cityList = listOf<CityItem>()
    private var selectedStateId = ""
    private var selectedCityId = ""

    private lateinit var filePickerUtil: FilePickerUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAccountV2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(imeInsets.bottom, systemBars.bottom)
            )
            if (imeInsets.bottom > 0) Utility.scrollToFocused(mActivity)
            insets
        }

        LiquidGlassEffect.attach(
            targetView = binding.flCard,
            rootView = binding.root as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        binding.onClickListener = onClickListener()
        binding.showProgress = showProgress

        onBack()
        setupInputFilters()
        setupTermsText()
        setupAadhaarUpload()
        loadStates()
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun setupInputFilters() {
        val emojiFilter = Utility.EmojiExcludeFilter()
        binding.etName.filters = arrayOf(InputFilter.LengthFilter(150), Utility.alphaSpaceFilter(), emojiFilter)
        binding.etMobile.filters = arrayOf(InputFilter.LengthFilter(10), Utility.digitFilter(), emojiFilter)
        binding.etEmail.filters = arrayOf(InputFilter.LengthFilter(100), emojiFilter)
        binding.etAadhaarNumber.filters = arrayOf(InputFilter.LengthFilter(12), Utility.digitFilter(), emojiFilter)
        binding.etReferralCode.filters = arrayOf(InputFilter.LengthFilter(50), emojiFilter)
        binding.etPassword.filters = arrayOf(InputFilter.LengthFilter(20), emojiFilter)
        binding.etConfirmPassword.filters = arrayOf(InputFilter.LengthFilter(20), emojiFilter)
    }

    private fun setupTermsText() {
        val fullText = getString(R.string.labelTermsConditions)
        val linkText = getString(R.string.termsLinkText)
        val spannable = SpannableString(fullText)
        val linkStart = fullText.indexOf(linkText)
        if (linkStart < 0) return
        val linkEnd = linkStart + linkText.length
        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                Utility.openUrl(mActivity, Constant.URL_PLATFORM_TERMS)
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(this@CreateAccountV2Activity, R.color.primary)
                ds.isUnderlineText = true
            }
        }, linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvTerms.text = spannable
        binding.tvTerms.movementMethod = LinkMovementMethod.getInstance()
        binding.tvTerms.highlightColor = ContextCompat.getColor(this, android.R.color.transparent)
    }

    private fun setupAadhaarUpload() {
        binding.flUploadAadhaarBtn.attach(binding.root as ViewGroup)
        LiquidGlassEffect.attach(
            targetView = binding.flEditAadhaar,
            rootView = binding.root as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.filter_btn_radius),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.filter_btn_blure),
            tintColor = ContextCompat.getColor(this, R.color.filter_bg)
        )
        LiquidGlassEffect.attach(
            targetView = binding.flDeleteAadhaar,
            rootView = binding.root as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.filter_btn_radius),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.filter_btn_blure),
            tintColor = ContextCompat.getColor(this, R.color.filter_bg)
        )

        filePickerUtil = FilePickerUtil(this)
        filePickerUtil.onSuccess = { result ->
            aadhaarUri = result.uri
            showAadhaarPreview(result.uri)
        }
        filePickerUtil.onError = { error ->
            ToastUtil.showDelete(
                mActivity,
                filePickerUtil.getErrorMessage(error),
                inWindow = error != FilePickerUtil.FilePickerError.CameraPermissionBlocked
            )
        }
    }

    private fun showAadhaarPreview(uri: Uri) {
        binding.llUploadPrompt.visibility = View.GONE
        binding.ivPreviewAadhaar.visibility = View.VISIBLE
        binding.llEditDeleteAadhaar.visibility = View.VISIBLE

        if (contentResolver.getType(uri) == "application/pdf") {
            binding.ivPreviewAadhaar.setImageResource(R.drawable.ic_file_not_found)
            val targetWidth = binding.ivPreviewAadhaar.width.takeIf { it > 0 }
                ?: resources.displayMetrics.widthPixels / 2
            lifecycleScope.launch(Dispatchers.IO) {
                val bmp = Utility.renderPdfFirstPage(this@CreateAccountV2Activity, uri, targetWidth)
                withContext(Dispatchers.Main) {
                    Glide.with(this@CreateAccountV2Activity).load(bmp).into(binding.ivPreviewAadhaar)
                }
            }
        } else {
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_file_not_found)
                .error(R.drawable.ic_file_not_found)
                .into(binding.ivPreviewAadhaar)
        }
    }

    private fun clearAadhaarSlot() {
        aadhaarUri = null
        Glide.with(this).clear(binding.ivPreviewAadhaar)
        binding.ivPreviewAadhaar.visibility = View.GONE
        binding.llEditDeleteAadhaar.visibility = View.GONE
        binding.llUploadPrompt.visibility = View.VISIBLE
    }

    private fun loadStates() {
        viewModel.loadStates(
            onLoading = { setStateLoading(true) },
            onSuccess = onSuccess@{ states ->
                if (isDestroyed) return@onSuccess
                setStateLoading(false)
                stateList = states
            },
            onError = onError@{ msg ->
                if (isDestroyed) return@onError
                setStateLoading(false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun setStateLoading(loading: Boolean) {
        binding.pbStateLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.ivStateArrow.visibility = if (loading) View.GONE else View.VISIBLE
        binding.flStateAnchor.isClickable = !loading
        binding.flStateAnchor.isFocusable = !loading
    }

    private fun setCityLoading(loading: Boolean) {
        binding.pbCityLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.ivCityArrow.visibility = if (loading) View.GONE else View.VISIBLE
        binding.flCityAnchor.isClickable = !loading
        binding.flCityAnchor.isFocusable = !loading
    }

    private fun validate(): Boolean {
        Utility.hideKeyboard(binding.clRoot)
        val name = binding.etName.text?.toString()?.trim() ?: ""
        val mobile = binding.etMobile.text?.toString()?.trim() ?: ""
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val aadhaar = binding.etAadhaarNumber.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""
        val confirmPassword = binding.etConfirmPassword.text?.toString() ?: ""

        val msg = when {
            name.isEmpty() -> { binding.etName.requestFocus(); getString(R.string.msgNameEmpty) }
            name.any { !it.isLetter() && !it.isWhitespace() } -> { binding.etName.requestFocus(); getString(R.string.msgNameInvalid) }
            mobile.isEmpty() -> { binding.etMobile.requestFocus(); getString(R.string.msgMobileEmpty) }
            mobile.length != 10 -> { binding.etMobile.requestFocus(); getString(R.string.msgMobileInvalid) }
            email.isEmpty() -> { binding.etEmail.requestFocus(); getString(R.string.msgEmailEmpty) }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { binding.etEmail.requestFocus(); getString(R.string.msgEmailInvalid) }
            aadhaar.isEmpty() -> { binding.etAadhaarNumber.requestFocus(); getString(R.string.msgAadharEmpty) }
            aadhaar.length != 12 -> { binding.etAadhaarNumber.requestFocus(); getString(R.string.msgAadharInvalid) }
            aadhaarUri == null -> getString(R.string.msgAadhaarPhotoRequired)
            selectedStateId.isEmpty() -> getString(R.string.msgStateEmpty)
            selectedCityId.isEmpty() -> getString(R.string.msgCityEmpty)
            password.isEmpty() -> { binding.etPassword.requestFocus(); getString(R.string.msgPasswordEmpty) }
            password.length < 8 -> { binding.etPassword.requestFocus(); getString(R.string.msgPasswordShort) }
            !Utility.isPasswordStrong(password) -> { binding.etPassword.requestFocus(); getString(R.string.msgPasswordWeak) }
            confirmPassword.isEmpty() -> { binding.etConfirmPassword.requestFocus(); getString(R.string.msgConfirmPasswordEmpty) }
            confirmPassword != password -> { binding.etConfirmPassword.requestFocus(); getString(R.string.msgPasswordMismatch) }
            !binding.cbTerms.isChecked -> getString(R.string.msgTermsNotAccepted)
            else -> null
        }

        if (msg != null) {
            ToastUtil.showDelete(mActivity, msg)
            return false
        }
        return true
    }

    private fun onCreateAccount() {
        if (!validate()) return
        val uri = aadhaarUri ?: return

        viewModel.register(
            name = binding.etName.text?.toString()?.trim() ?: "",
            mobile = binding.etMobile.text?.toString()?.trim() ?: "",
            email = binding.etEmail.text?.toString()?.trim() ?: "",
            password = binding.etPassword.text?.toString() ?: "",
            passwordConfirmation = binding.etConfirmPassword.text?.toString() ?: "",
            aadhaarNumber = binding.etAadhaarNumber.text?.toString()?.trim() ?: "",
            aadhaarUri = uri,
            stateId = selectedStateId,
            cityId = selectedCityId,
            referralCode = binding.etReferralCode.text?.toString()?.trim() ?: "",
            onLoading = { showProgress.set(true) },
            onSuccess = onSuccess@{
                if (isDestroyed) return@onSuccess
                showProgress.set(false)
                ToastUtil.showSuccess(mActivity, getString(R.string.msgAccountCreatedSuccessfully), inWindow = false)
                val intent = Intent(mActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            },
            onError = onError@{ msg ->
                if (isDestroyed) return@onError
                showProgress.set(false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.llCreateAccount -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onCreateAccount()
                }
                binding.tvBackToSignIn -> {
                    if (Utility.stopClick()) return@OnClickListener
                    finish()
                }
                binding.flStateAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showStateDropdown()
                }
                binding.flCityAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showCityDropdown()
                }
                binding.flUploadAadhaarBtn, binding.flEditAadhaar -> {
                    if (Utility.stopClick()) return@OnClickListener
                    filePickerUtil.showSourceChooser(aadhaarDir)
                }
                binding.ivDeleteAadhaar -> {
                    if (Utility.stopClick()) return@OnClickListener
                    clearAadhaarSlot()
                }
                binding.ivPasswordToggle -> togglePasswordVisibility()
                binding.ivConfirmPasswordToggle -> toggleConfirmPasswordVisibility()
            }
        }
    }

    private fun showStateDropdown() {
        if (stateList.isEmpty()) {
            loadStates()
            return
        }
        CustomDropdown.showDropdown(
            activity = mActivity,
            anchorView = binding.flStateAnchor,
            arrowView = binding.ivStateArrow,
            textView = binding.tvState,
            items = stateList.map { it.name.orEmpty() }
        ) { _, index ->
            val state = stateList.getOrNull(index) ?: return@showDropdown
            selectedStateId = state.id ?: ""
            binding.tvState.setTextColor(ContextCompat.getColor(mActivity, R.color.black))
            selectedCityId = ""
            cityList = emptyList()
            binding.tvCity.text = getString(R.string.hintSelectCity)
            binding.tvCity.setTextColor(ContextCompat.getColor(mActivity, android.R.color.darker_gray))
            if (selectedStateId.isNotEmpty()) loadCities(selectedStateId)
        }
    }

    private fun showCityDropdown() {
        if (selectedStateId.isEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgStateEmpty))
            return
        }
        if (cityList.isEmpty()) {
            loadCities(selectedStateId)
            return
        }
        CustomDropdown.showDropdown(
            activity = mActivity,
            anchorView = binding.flCityAnchor,
            arrowView = binding.ivCityArrow,
            textView = binding.tvCity,
            items = cityList.map { it.name.orEmpty() }
        ) { _, index ->
            val city = cityList.getOrNull(index) ?: return@showDropdown
            selectedCityId = city.id ?: ""
            binding.tvCity.setTextColor(ContextCompat.getColor(mActivity, R.color.black))
        }
    }

    private fun loadCities(stateId: String) {
        viewModel.loadCities(
            stateId = stateId,
            onLoading = { setCityLoading(true) },
            onSuccess = onSuccess@{ cities ->
                if (isDestroyed) return@onSuccess
                setCityLoading(false)
                cityList = cities
            },
            onError = onError@{ msg ->
                if (isDestroyed) return@onError
                setCityLoading(false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        binding.etPassword.inputType = if (isPasswordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        binding.ivPasswordToggle.setImageResource(
            if (isPasswordVisible) R.drawable.ic_eye_on else R.drawable.ic_eye_off
        )
    }

    private fun toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible
        binding.etConfirmPassword.inputType = if (isConfirmPasswordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text?.length ?: 0)
        binding.ivConfirmPasswordToggle.setImageResource(
            if (isConfirmPasswordVisible) R.drawable.ic_eye_on else R.drawable.ic_eye_off
        )
    }


    override fun onDestroy() {
        if (isFinishing) Utility.deleteKycDirs(mActivity, Constant.REG_AADHAAR_DIR)
        super.onDestroy()
    }
}
