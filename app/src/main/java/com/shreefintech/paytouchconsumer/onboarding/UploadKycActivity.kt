package com.shreefintech.paytouchconsumer.onboarding

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.onboarding.viewmodel.UploadKycViewModel
import com.shreefintech.paytouchconsumer.databinding.ActivityUploadKycBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.widget.CustomDropdown
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class UploadKycActivity : BaseActivity() {

    companion object {
        private val PAN_REGEX = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")
        private val GST_REGEX = Regex("^\\d{2}[A-Z]{5}\\d{4}[A-Z][1-9A-Z]Z[0-9A-Z]$")
        private const val MIN_AGE_YEARS = 18
        private const val MAX_AGE_YEARS = 100
    }

    private lateinit var binding: ActivityUploadKycBinding
    private val viewModel: UploadKycViewModel by viewModels()
    private var showProgress = ObservableBoolean(false)
    private var resultCode = 0

    private var birthCalendar: Calendar? = null
    private var selectedCity: String? = null

    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val cityList = listOf(
        "Mumbai", "Delhi", "Bengaluru", "Hyderabad", "Ahmedabad",
        "Chennai", "Kolkata", "Pune", "Jaipur", "Surat"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadKycBinding.inflate(layoutInflater)
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

        binding.etBirthdate.showSoftInputOnFocus = false

        onBack()
        setupInputFilters()
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setResult(resultCode)
                finish()
            }
        })
    }

    private fun setupInputFilters() {
        val emojiFilter      = Utility.EmojiExcludeFilter()
        val upperCaseFilter  = InputFilter { source, start, end, _, _, _ ->
            source.subSequence(start, end).toString().uppercase()
        }

        binding.etMobile.filters     = arrayOf(InputFilter.LengthFilter(10), Utility.digitFilter(), emojiFilter)
        binding.etMemberName.filters = arrayOf(InputFilter.LengthFilter(50), Utility.alphaSpaceFilter(), emojiFilter)
        binding.etAddress.filters    = arrayOf(InputFilter.LengthFilter(200), emojiFilter)
        binding.etEmail.filters      = arrayOf(InputFilter.LengthFilter(100), emojiFilter)
        binding.etPan.filters        = arrayOf(InputFilter.LengthFilter(10), upperCaseFilter, emojiFilter)
        binding.etAadhar.filters     = arrayOf(InputFilter.LengthFilter(12), Utility.digitFilter(), emojiFilter)
        binding.etGst.filters        = arrayOf(InputFilter.LengthFilter(15), upperCaseFilter, emojiFilter)
    }

    private fun showDatePicker() {
        val maxDob = Calendar.getInstance().apply { add(Calendar.YEAR, -MIN_AGE_YEARS) }
        val minDob = Calendar.getInstance().apply { add(Calendar.YEAR, -MAX_AGE_YEARS) }
        val defaultCal = birthCalendar ?: maxDob

        DatePickerDialog(
            mActivity,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    clear()
                    set(year, month, dayOfMonth)
                }
                birthCalendar = picked
                binding.etBirthdate.setText(displayDateFormat.format(picked.time))
                binding.etAge.setText(calculateAge(picked).toString())
            },
            defaultCal.get(Calendar.YEAR),
            defaultCal.get(Calendar.MONTH),
            defaultCal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = maxDob.timeInMillis
            datePicker.minDate = minDob.timeInMillis
        }.show()
    }

    private fun calculateAge(dob: Calendar): Int {
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        val notYetHadBirthday =
            today.get(Calendar.MONTH) < dob.get(Calendar.MONTH) ||
            (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) &&
             today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))
        if (notYetHadBirthday) age--
        return age
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

    private fun validate(): Boolean {
        Utility.hideKeyboard(binding.clRoot)
        val msg = validatePersonalInfo() ?: validateContactAndLocation() ?: validateIdentityDocs()
        if (msg != null) { ToastUtil.showDelete(mActivity, msg); return false }
        return true
    }

    private fun validatePersonalInfo(): String? {
        val mobile     = binding.etMobile.text?.toString()?.trim()     ?: ""
        val memberName = binding.etMemberName.text?.toString()?.trim() ?: ""
        val age        = binding.etAge.text?.toString()?.trim()?.toIntOrNull()
        return when {
            mobile.isEmpty()     -> { binding.etMobile.requestFocus(); getString(R.string.msgMobileEmpty) }
            mobile.length != 10  -> { binding.etMobile.requestFocus(); getString(R.string.msgMobileInvalid) }
            memberName.isEmpty() -> { binding.etMemberName.requestFocus(); getString(R.string.msgMemberNameEmpty) }
            birthCalendar == null                  -> getString(R.string.msgBirthdateEmpty)
            age == null || age < MIN_AGE_YEARS     -> getString(R.string.msgAgeInvalid)
            else -> null
        }
    }

    private fun validateContactAndLocation(): String? {
        val address = binding.etAddress.text?.toString()?.trim() ?: ""
        val email   = binding.etEmail.text?.toString()?.trim()   ?: ""
        return when {
            address.isEmpty()   -> { binding.etAddress.requestFocus(); getString(R.string.msgHomeAddressEmpty) }
            address.length < 5  -> { binding.etAddress.requestFocus(); getString(R.string.msgHomeAddressShort) }
            selectedCity.isNullOrEmpty() -> getString(R.string.msgCityEmpty)
            email.isEmpty()     -> { binding.etEmail.requestFocus(); getString(R.string.msgEmailEmpty) }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.etEmail.requestFocus(); getString(R.string.msgEmailInvalid)
            }
            else -> null
        }
    }

    private fun validateIdentityDocs(): String? {
        val pan    = binding.etPan.text?.toString()?.trim()    ?: ""
        val aadhar = binding.etAadhar.text?.toString()?.trim() ?: ""
        val gst    = binding.etGst.text?.toString()?.trim()    ?: ""
        return when {
            pan.isEmpty()           -> { binding.etPan.requestFocus(); getString(R.string.msgPanEmpty) }
            !PAN_REGEX.matches(pan) -> { binding.etPan.requestFocus(); getString(R.string.msgPanInvalid) }
            aadhar.isEmpty()        -> { binding.etAadhar.requestFocus(); getString(R.string.msgAadharEmpty) }
            aadhar.length != 12     -> { binding.etAadhar.requestFocus(); getString(R.string.msgAadharInvalid) }
            gst.isNotEmpty() && !GST_REGEX.matches(gst) -> {
                binding.etGst.requestFocus(); getString(R.string.msgGstInvalid)
            }
            else -> null
        }
    }

    private fun onSubmitKyc() {
        if (!validate()) return
        val calendar = birthCalendar ?: return

        viewModel.submitKyc(
            mobile        = binding.etMobile.text?.toString()?.trim()     ?: "",
            memberName    = binding.etMemberName.text?.toString()?.trim() ?: "",
            birthdate     = apiDateFormat.format(calendar.time),
            age           = binding.etAge.text?.toString()?.toIntOrNull() ?: 0,
            address       = binding.etAddress.text?.toString()?.trim()    ?: "",
            city          = selectedCity ?: "",
            email         = binding.etEmail.text?.toString()?.trim()      ?: "",
            panNumber     = binding.etPan.text?.toString()?.trim()        ?: "",
            aadharNumber  = binding.etAadhar.text?.toString()?.trim()     ?: "",
            gstNumber     = binding.etGst.text?.toString()?.trim()        ?: "",
            onLoading     = { showProgress.set(true) },
            onSuccess     = {
                showProgress.set(false)
                ToastUtil.showSuccess(mActivity, getString(R.string.msgKycSubmitSuccess))
                resultCode = 1
                startActivity(Intent(mActivity, CreateVirtualAccountActivity::class.java))
                finish()
            },
            onError       = { msg -> showProgress.set(false); ToastUtil.showDelete(mActivity, msg) }
        )
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
                    // TODO(PAYTOUCH-514): replace with onSubmitKyc() once KYC API is wired
                    startActivity(Intent(mActivity, CreateVirtualAccountActivity::class.java))
                    finish()
                }

                binding.etBirthdate -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showDatePicker()
                }

                binding.flCityAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showCityDropdown()
                }
            }
        }
    }
}
