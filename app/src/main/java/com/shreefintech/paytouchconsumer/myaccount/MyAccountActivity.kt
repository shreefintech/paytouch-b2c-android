package com.shreefintech.paytouchconsumer.myaccount

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean

import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityMyAccountBinding
import com.shreefintech.paytouchconsumer.databinding.DialogEditProfileBinding
import com.shreefintech.paytouchconsumer.myaccount.viewmodel.MyAccountViewModel
import com.shreefintech.paytouchconsumer.retrofit.model.CityItem
import com.shreefintech.paytouchconsumer.retrofit.model.StateItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.AccountInfoItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.EditProfileRequest
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.ReferralDataItem
import com.shreefintech.paytouchconsumer.utill.AnimationHelper
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.getThemeColor
import com.shreefintech.paytouchconsumer.utill.loadAddressCities
import com.shreefintech.paytouchconsumer.utill.loadAddressStates
import com.shreefintech.paytouchconsumer.widget.CustomDropdown
import androidx.core.view.isVisible
import androidx.core.net.toUri

class MyAccountActivity : BaseActivity() {

    private lateinit var binding: ActivityMyAccountBinding
    private val viewModel: MyAccountViewModel by viewModels()

    private var isAccountInfoLoading = true
    private var isReferEarnLoading = false
    private var currentTab = TAB_ACCOUNT_INFO

    // ── Edit Profile dialog state ──
    private val showProgressUpdate = ObservableBoolean(false)
    private var accountInfo: AccountInfoItem? = null
    private var editProfileDialog: Dialog? = null
    private var editProfileBinding: DialogEditProfileBinding? = null
    private var isStatesLoading = false
    private var stateList = listOf<StateItem>()
    private var cityList = listOf<CityItem>()
    // State whose cities are in cityList — reused across dialog opens instead of refetching
    private var cityListStateId: String? = null
    // Pre-selected in the Edit Profile dialog — sourced from account-overview `address`
    private var savedState: StateItem? = null
    private var savedCity: CityItem? = null
    private var selectedState: StateItem? = null
    private var selectedCity: CityItem? = null

    companion object {
        private const val TAB_ACCOUNT_INFO = 0
        private const val TAB_REFER_EARN = 1

        fun start(context: Context) {
            context.startActivity(Intent(context, MyAccountActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(imeInsets.bottom, systemBars.bottom)
            )
            insets
        }

        binding.onClickListener = onClickListener()
        binding.lytToolbar.ivHelp.visibility = View.VISIBLE
        selectTab(TAB_ACCOUNT_INFO, animate = false)
        onBack()

        retryCallback = { loadData() }
        loadData()
    }

    // ── API Calls ─────────────────────────────────────────────

    private fun loadData() {
        if (!Utility.isInternetAvailable(mActivity)) {
            showNoInternet()
            return
        }
        hideNoInternet()
        loadAccountInfo()
        loadReferralInfo()
    }

    private fun loadAccountInfo() {
        viewModel.getAccountInfo(
            onLoading = {
                isAccountInfoLoading = true
                binding.llAccountInfoContent.visibility = View.GONE
                // shimmer only shown if Account Info tab is active when loading starts
                if (currentTab == TAB_ACCOUNT_INFO) {
                    binding.shimmerAccountInfo.visibility = View.VISIBLE
                    binding.shimmerAccountInfo.startShimmer()
                }
            },
            onSuccess = { data: AccountInfoItem ->
                isAccountInfoLoading = false
                binding.shimmerAccountInfo.stopShimmer()
                binding.shimmerAccountInfo.visibility = View.GONE
                binding.llAccountInfoContent.visibility =
                    if (currentTab == TAB_ACCOUNT_INFO) View.VISIBLE else View.GONE
                populateAccountInfo(data)
            },
            onError = { msg ->
                isAccountInfoLoading = false
                binding.shimmerAccountInfo.stopShimmer()
                binding.shimmerAccountInfo.visibility = View.GONE
                binding.llAccountInfoContent.visibility =
                    if (currentTab == TAB_ACCOUNT_INFO) View.VISIBLE else View.GONE
                if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun loadReferralInfo() {
        viewModel.getReferralInfo(
            onLoading = {
                isReferEarnLoading = true
                // shimmer only shown if Refer & Earn tab is active when loading starts
                if (binding.shimmerReferEarn.isVisible ||
                    binding.llReferEarnContent.isVisible
                ) {
                    binding.shimmerReferEarn.visibility = View.VISIBLE
                    binding.shimmerReferEarn.startShimmer()
                    binding.llReferEarnContent.visibility = View.GONE
                }
            },
            onSuccess = { data ->
                isReferEarnLoading = false
                binding.shimmerReferEarn.stopShimmer()
                binding.shimmerReferEarn.visibility = View.GONE
                binding.llReferEarnContent.visibility =
                    if (currentTab == TAB_REFER_EARN) View.VISIBLE else View.GONE
                populateReferralInfo(data)
            },
            onError = { msg ->
                isReferEarnLoading = false
                binding.shimmerReferEarn.stopShimmer()
                binding.shimmerReferEarn.visibility = View.GONE
                if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    // ── Populate ──────────────────────────────────────────────

    private fun populateAccountInfo(data: AccountInfoItem) {
        // Hidden tab is animated by selectTab() when the user switches back to it
        if (currentTab == TAB_ACCOUNT_INFO) AnimationHelper.animateChildren(binding.llAccountInfoContent)
        accountInfo = data
        binding.tvMemberName.text = data.name ?: "--"
        binding.tvStatus.text = data.member?.status ?: "--"
        binding.tvMemberCode.text = data.member?.memberCode ?: "--"
        binding.tvMobileNo.text = data.contact?.mobile ?: "--"
        binding.tvEmail.text = data.contact?.email ?: "--"
        binding.tvHomeAddress.text = data.homeAddress ?: "--"
        savedState = data.address?.state?.takeIf { it.id != null }
        savedCity = data.address?.city?.takeIf { it.id != null }
        binding.tvRegistrationDate.text = Utility.formatDate(data.membership?.registeredOn, "dd-MM-yyyy")
        binding.tvActivationDate.text = Utility.formatDate(data.membership?.activatedOn, "dd-MM-yyyy")
        binding.tvBalance.text = if (data.walletBalance != null) Utility.formatAmount(data.walletBalance) else "--"
    }

    private fun populateReferralInfo(data: ReferralDataItem) {
        val code = data.referralCode ?: "--"
        val link = data.referralLink ?: "--"

        binding.tvReferralCode.text = code
        binding.tvReferralLink.text = link
        binding.tvTotalEarnings.text = Utility.formatAmount(data.totalEarnings)

        // Store referral code in SharedPreferences for sharing
        if (code != "--") {
            SharedPreferenceHelper.setSharedPreferenceString(
                mActivity, com.shreefintech.paytouchconsumer.Constant.KEY_REFERRAL_CODE, code
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        ToastUtil.showSuccess(mActivity, getString(R.string.msgCopiedToClipboard))
    }

    // ── Tab Switching ─────────────────────────────────────────

    private fun selectTab(tab: Int, animate: Boolean = true) {
        currentTab = tab
        val isAccountInfo = tab == TAB_ACCOUNT_INFO

        // Account Info section
        if (isAccountInfo) {
            if (isAccountInfoLoading) {
                binding.shimmerAccountInfo.visibility = View.VISIBLE
                binding.shimmerAccountInfo.startShimmer()
                binding.llAccountInfoContent.visibility = View.GONE
            } else {
                binding.shimmerAccountInfo.visibility = View.GONE
                binding.llAccountInfoContent.visibility = View.VISIBLE
                if (animate) AnimationHelper.animateChildren(binding.llAccountInfoContent)
            }
            binding.shimmerReferEarn.stopShimmer()
            binding.shimmerReferEarn.visibility = View.GONE
            binding.llReferEarnContent.visibility = View.GONE
        } else {
            if (isReferEarnLoading) {
                binding.shimmerReferEarn.visibility = View.VISIBLE
                binding.shimmerReferEarn.startShimmer()
                binding.llReferEarnContent.visibility = View.GONE
            } else {
                binding.shimmerReferEarn.visibility = View.GONE
                binding.llReferEarnContent.visibility = View.VISIBLE
                if (animate) AnimationHelper.animateChildren(binding.llReferEarnContent)
            }
            binding.shimmerAccountInfo.stopShimmer()
            binding.shimmerAccountInfo.visibility = View.GONE
            binding.llAccountInfoContent.visibility = View.GONE
        }


        val activeColor = ContextCompat.getColor(mActivity, R.color.primary)
        val inactiveColor = android.graphics.Color.TRANSPARENT
        val activeTextColor = ContextCompat.getColor(mActivity, R.color.white)
        val inactiveTextColor = ContextCompat.getColor(mActivity, R.color.primary)

        binding.cvTabAccountInfo.setCardBackgroundColor(if (isAccountInfo) activeColor else inactiveColor)
        binding.cvTabReferEarn.setCardBackgroundColor(if (isAccountInfo) inactiveColor else activeColor)
        binding.tvTabAccountInfo.setTextColor(if (isAccountInfo) activeTextColor else inactiveTextColor)
        binding.tvTabReferEarn.setTextColor(if (isAccountInfo) inactiveTextColor else activeTextColor)
    }

    // ── Back ──────────────────────────────────────────────────

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    // ── Clicks ────────────────────────────────────────────────

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.lytToolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBackPressedDispatcher.onBackPressed()
                }
                binding.lytToolbar.ivHelp -> {
                    if (Utility.stopClick()) return@OnClickListener
                    ContactUsActivity.start(mActivity)
                }
                binding.cvTabAccountInfo -> {
                    if (Utility.stopClick()) return@OnClickListener
                    selectTab(TAB_ACCOUNT_INFO)
                }
                binding.cvTabReferEarn -> {
                    if (Utility.stopClick()) return@OnClickListener
                    selectTab(TAB_REFER_EARN)
                }
                binding.cvEditProfile -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // No prefill data until account info loads successfully
                    if (isAccountInfoLoading || accountInfo == null) return@OnClickListener
                    showEditProfileDialog()
                }
                binding.cvViewKycDetails -> {
                    if (Utility.stopClick()) return@OnClickListener
                    KycDetailsActivity.start(mActivity)
                }
                binding.ivCopyReferralCode -> {
                    if (Utility.stopClick()) return@OnClickListener
                    val code = binding.tvReferralCode.text.toString()
                    if (code != "--") copyToClipboard(getString(R.string.labelReferralCode), code)
                }
                binding.cvCopyReferralLink -> {
                    if (Utility.stopClick()) return@OnClickListener
                    val link = binding.tvReferralLink.text.toString()
                    if (link != "--") copyToClipboard(getString(R.string.labelReferralLink), link)
                }
                binding.cvShareWhatsapp -> {
                    if (Utility.stopClick()) return@OnClickListener
                    shareViaWhatsApp()
                }
                binding.cvShareFacebook -> {
                    if (Utility.stopClick()) return@OnClickListener
                    shareViaFacebook()
                }
                binding.cvShareEmail -> {
                    if (Utility.stopClick()) return@OnClickListener
                    shareViaEmail()
                }
            }
        }
    }

    // ── Edit Profile ──────────────────────────────────────────

    private fun showEditProfileDialog() {
        if (editProfileDialog?.isShowing == true) return
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)
        dialogBinding.showProgress = showProgressUpdate
        dialogBinding.onClickListener = editProfileClickListener(dialogBinding)

        dialogBinding.etName.setText(accountInfo?.name.orEmpty())
        dialogBinding.etEmail.setText(accountInfo?.contact?.email.orEmpty())
        selectedState = savedState
        selectedCity = savedCity
        setDropdownValue(dialogBinding.tvState, selectedState?.name, R.string.hintSelectState)
        setDropdownValue(dialogBinding.tvCity, selectedCity?.name, R.string.hintSelectCity)

        val dialog = Dialog(mActivity)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setWindowAnimations(R.style.DialogScaleFadeAnimation)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialog.setOnDismissListener {
            editProfileDialog = null
            editProfileBinding = null
        }
        editProfileDialog = dialog
        editProfileBinding = dialogBinding
        dialog.show()

        when {
            isStatesLoading -> setFieldLoading(dialogBinding.pbStateLoading, dialogBinding.ivStateArrow, dialogBinding.flStateAnchor, true)
            stateList.isEmpty() -> loadStates()
        }
        val stateId = selectedState?.id
        if (stateId != cityListStateId) {
            // Cached cities belong to another state (picked earlier, then dismissed without saving)
            cityList = emptyList()
            cityListStateId = null
            stateId?.let { loadCities(it) }
        }
    }

    private fun editProfileClickListener(dialogBinding: DialogEditProfileBinding): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                dialogBinding.cardClose -> {
                    if (Utility.stopClick()) return@OnClickListener
                    editProfileDialog?.dismiss()
                }
                dialogBinding.flStateAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showStateDropdown(dialogBinding)
                }
                dialogBinding.flCityAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showCityDropdown(dialogBinding)
                }
                dialogBinding.cardSubmit -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressUpdate.get()) return@OnClickListener
                    onUpdateProfile(dialogBinding)
                }
            }
        }
    }

    private fun showStateDropdown(dialogBinding: DialogEditProfileBinding) {
        if (stateList.isEmpty()) {
            loadStates()
            return
        }
        CustomDropdown.showDropdown(
            activity = mActivity,
            anchorView = dialogBinding.flStateAnchor,
            arrowView = dialogBinding.ivStateArrow,
            textView = dialogBinding.tvState,
            items = stateList.map { it.name.orEmpty() }
        ) { _, index ->
            val state = stateList.getOrNull(index) ?: return@showDropdown
            setDropdownValue(dialogBinding.tvState, state.name, R.string.hintSelectState)
            if (state.id == selectedState?.id) return@showDropdown
            selectedState = state
            selectedCity = null
            cityList = emptyList()
            cityListStateId = null
            setDropdownValue(dialogBinding.tvCity, null, R.string.hintSelectCity)
            // No id = nothing to load — clear a spinner left by the previous state's in-flight request
            state.id?.let { loadCities(it) }
                ?: setFieldLoading(dialogBinding.pbCityLoading, dialogBinding.ivCityArrow, dialogBinding.flCityAnchor, false)
        }
    }

    private fun showCityDropdown(dialogBinding: DialogEditProfileBinding) {
        val stateId = selectedState?.id
        if (stateId == null) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgStateEmpty))
            return
        }
        if (cityList.isEmpty()) {
            loadCities(stateId)
            return
        }
        CustomDropdown.showDropdown(
            activity = mActivity,
            anchorView = dialogBinding.flCityAnchor,
            arrowView = dialogBinding.ivCityArrow,
            textView = dialogBinding.tvCity,
            items = cityList.map { it.name.orEmpty() }
        ) { _, index ->
            val city = cityList.getOrNull(index) ?: return@showDropdown
            selectedCity = city
            setDropdownValue(dialogBinding.tvCity, city.name, R.string.hintSelectCity)
        }
    }

    private fun loadStates() {
        if (isStatesLoading) return
        viewModel.loadAddressStates(
            onLoading = {
                isStatesLoading = true
                editProfileBinding?.let { setFieldLoading(it.pbStateLoading, it.ivStateArrow, it.flStateAnchor, true) }
            },
            onSuccess = onSuccess@{ states ->
                isStatesLoading = false
                if (isDestroyed) return@onSuccess
                stateList = states
                editProfileBinding?.let { setFieldLoading(it.pbStateLoading, it.ivStateArrow, it.flStateAnchor, false) }
            },
            onError = onError@{ msg ->
                isStatesLoading = false
                if (isDestroyed) return@onError
                val dialogBinding = editProfileBinding ?: return@onError
                setFieldLoading(dialogBinding.pbStateLoading, dialogBinding.ivStateArrow, dialogBinding.flStateAnchor, false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun loadCities(stateId: String) {
        viewModel.loadAddressCities(
            stateId = stateId,
            onLoading = {
                editProfileBinding?.let { setFieldLoading(it.pbCityLoading, it.ivCityArrow, it.flCityAnchor, true) }
            },
            onSuccess = onSuccess@{ cities ->
                // Stale response — user picked another state while this one was in flight
                if (isDestroyed || stateId != selectedState?.id) return@onSuccess
                cityList = cities
                cityListStateId = stateId
                editProfileBinding?.let { setFieldLoading(it.pbCityLoading, it.ivCityArrow, it.flCityAnchor, false) }
            },
            onError = onError@{ msg ->
                if (isDestroyed || stateId != selectedState?.id) return@onError
                val dialogBinding = editProfileBinding ?: return@onError
                setFieldLoading(dialogBinding.pbCityLoading, dialogBinding.ivCityArrow, dialogBinding.flCityAnchor, false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun onUpdateProfile(dialogBinding: DialogEditProfileBinding) {
        Utility.hideKeyboard(dialogBinding.root)
        val name = dialogBinding.etName.text?.toString()?.trim().orEmpty()
        val email = dialogBinding.etEmail.text?.toString()?.trim().orEmpty()
        val stateId = selectedState?.id?.toIntOrNull()
        val cityId = selectedCity?.id?.toIntOrNull()

        val msg = when {
            name.isEmpty() -> { dialogBinding.etName.requestFocus(); getString(R.string.msgNameEmpty) }
            name.any { !it.isLetter() && !it.isWhitespace() } -> { dialogBinding.etName.requestFocus(); getString(R.string.msgNameInvalid) }
            email.isEmpty() -> { dialogBinding.etEmail.requestFocus(); getString(R.string.msgEmailEmpty) }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { dialogBinding.etEmail.requestFocus(); getString(R.string.msgEmailInvalid) }
            stateId == null -> getString(R.string.msgStateEmpty)
            cityId == null -> getString(R.string.msgCityEmpty)
            else -> null
        }
        if (msg != null || stateId == null || cityId == null) {
            ToastUtil.showDelete(mActivity, msg ?: getString(R.string.errGeneric))
            return
        }

        // Dismiss only this dialog — a late response must not close one reopened after a manual dismiss
        val dialog = editProfileDialog
        viewModel.updateProfile(
            request = EditProfileRequest(name, email, stateId, cityId),
            onLoading = { showProgressUpdate.set(true) },
            onSuccess = onSuccess@{ data, message ->
                showProgressUpdate.set(false)
                if (isDestroyed) return@onSuccess
                savedState = data.state ?: selectedState
                savedCity = data.city ?: selectedCity
                data.email?.let {
                    SharedPreferenceHelper.setSharedPreferenceString(mActivity, Constant.KEY_EMAIL, it)
                }
                dialog?.dismiss()
                ToastUtil.showSuccess(mActivity, message)
                // Home address is derived server-side from state/city — refetch instead of patching fields
                loadAccountInfo()
            },
            onError = onError@{ errMsg ->
                showProgressUpdate.set(false)
                if (isDestroyed) return@onError
                ToastUtil.showDelete(mActivity, errMsg)
            }
        )
    }

    private fun setDropdownValue(textView: TextView, value: String?, @StringRes hintRes: Int) {
        textView.text = value ?: getString(hintRes)
        textView.setTextColor(
            if (value != null) ContextCompat.getColor(mActivity, R.color.black)
            else getThemeColor(R.attr.colorTextHint)
        )
    }

    private fun setFieldLoading(progress: View, arrow: View, anchor: View, loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        arrow.visibility = if (loading) View.GONE else View.VISIBLE
        anchor.isClickable = !loading
        anchor.isFocusable = !loading
    }

    override fun onDestroy() {
        // Dismiss here so the dialog window doesn't leak past the Activity
        editProfileDialog?.dismiss()
        super.onDestroy()
    }

    // ── Share Helpers ─────────────────────────────────────────

    private fun shareViaWhatsApp() {
        val link = binding.tvReferralLink.text.toString().takeIf { it != "--" } ?: return
        val message = getString(R.string.msgShareReferralInfo) + "\n\n$link"
        try {
            startActivity(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, message)
            })
        } catch (e: Exception) {
            shareGeneric(message)
        }
    }

    private fun shareViaFacebook() {
        val link = binding.tvReferralLink.text.toString().takeIf { it != "--" } ?: return
        try {
            startActivity(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.facebook.katana")
                putExtra(Intent.EXTRA_TEXT, link)
            })
        } catch (e: Exception) {
            shareGeneric(link)
        }
    }

    private fun shareViaEmail() {
        val link = binding.tvReferralLink.text.toString().takeIf { it != "--" } ?: return
        val message = getString(R.string.msgShareReferralInfo) + "\n\n$link"
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.titleReferAndEarn))
            putExtra(Intent.EXTRA_TEXT, message)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            shareGeneric(message)
        }
    }

    private fun shareGeneric(text: String) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                getString(R.string.titleReferAndEarn)
            )
        )
    }
}
