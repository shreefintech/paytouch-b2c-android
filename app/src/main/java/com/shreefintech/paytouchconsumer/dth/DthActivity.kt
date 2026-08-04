package com.shreefintech.paytouchconsumer.dth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import com.google.gson.Gson
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityDthBinding
import com.shreefintech.paytouchconsumer.dth.viewmodel.DthViewModel
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthPlanItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.getThemeColor
import com.shreefintech.paytouchconsumer.widget.CustomDropdown

class DthActivity : BaseActivity() {

    private lateinit var binding: ActivityDthBinding
    private val viewModel: DthViewModel by viewModels()

    private var operatorItems: List<DthOperatorItem> = emptyList()
    private var selectedOperatorId: String? = null
    private var selectedOperatorName: String? = null

    private var selectedPlan: DthPlanItem? = null
    private var isPlanSelected = false

    private val showProgressBrowse = ObservableBoolean(false)
    private val showProgressPay = ObservableBoolean(false)

    private val planSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val json = result.data?.getStringExtra(DthPlanSelectionActivity.EXTRA_SELECTED_PLAN)
            val plan = json?.let { Gson().fromJson(it, DthPlanItem::class.java) }
            if (plan != null) onPlanSelected(plan)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDthBinding.inflate(layoutInflater)
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

        LiquidGlassEffect.attach(
            targetView = binding.flCard,
            rootView = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        binding.onClickListener = onClickListener()
        binding.showProgressBrowse = showProgressBrowse
        binding.showProgressPay = showProgressPay
        setupInputFilters()
        setupAmountWatcher()
        setupTermsText()
        loadOperators()
        onBack()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupInputFilters() {
        val emojiFilter = Utility.EmojiExcludeFilter()
        binding.etMobileNumber.filters = arrayOf(Utility.digitFilter(), emojiFilter)
        binding.etAmount.filters = arrayOf(emojiFilter)
    }

    private fun setupAmountWatcher() {
        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val amount = s?.toString()?.trim()?.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    resetFeeDisplay()
                    return
                }
                val fee = Utility.calculatePlatformFee(amount)
                val total = amount + fee
                val black = ContextCompat.getColor(mActivity, R.color.black)
                binding.tvPlatformFee.text = Utility.formatAmount(fee)
                binding.tvPlatformFee.setTextColor(black)
                binding.tvTotalPayable.text = Utility.formatAmount(total)
                binding.tvTotalPayable.setTextColor(black)
            }
        })
    }

    private fun setupTermsText() {
        val fullText = getString(R.string.msgTermsAgreement)
        val linkText = getString(R.string.termsLinkText)
        val start = fullText.indexOf(linkText)
        if (start < 0) {
            binding.tvTerms.text = fullText
            return
        }
        val spannable = SpannableString(fullText)
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(Constant.URL_PLATFORM_TERMS)))
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(mActivity, R.color.primary)
                    ds.isUnderlineText = true
                }
            },
            start,
            start + linkText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.tvTerms.text = spannable
        binding.tvTerms.movementMethod = LinkMovementMethod.getInstance()
        binding.tvTerms.highlightColor = android.graphics.Color.TRANSPARENT
    }

    // ── API Calls (via ViewModel) ─────────────────────────────────────────────

    private fun loadOperators() {
        viewModel.loadOperators(
            onLoading = { setOperatorLoading(true) },
            onSuccess = { operators ->
                setOperatorLoading(false)
                operatorItems = operators
            },
            onError = { msg ->
                setOperatorLoading(false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun verifyBalanceAndProcessPayment() {
        val mobileNumber = binding.etMobileNumber.text?.toString()?.trim() ?: ""
        val amount = binding.etAmount.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
        val fee = Utility.calculatePlatformFee(amount)
        val total = amount + fee
        viewModel.verifyAndPay(
            mobileNo = mobileNumber,
            operatorId = selectedOperatorId ?: "",
            amount = amount,
            fee = fee,
            total = total,
            onLoading = { showProgressPay.set(true) },
            onSuccess = { _ ->
                showProgressPay.set(false)
                // TODO(B2C-68): replace with DthSmsReceiptActivity.start(mActivity, fromPayment = true) once DTH transaction screens are built
                ToastUtil.showSuccess(mActivity, getString(R.string.msgDthRechargeSuccess))
                finish()
            },
            onError = { msg ->
                showProgressPay.set(false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    private fun showCompanyDropdown() {
        Utility.hideKeyboard(binding.clRoot)
        if (operatorItems.isEmpty()) {
            loadOperators()
            ToastUtil.showWarning(mActivity, getString(R.string.msgLoadingOperators))
            return
        }
        val names = operatorItems.map { it.name ?: "" }
        CustomDropdown.showDropdown(
            activity = mActivity,
            anchorView = binding.flCompanyAnchor,
            arrowView = binding.ivCompanyArrow,
            textView = binding.tvCompany,
            items = names
        ) { selected, index ->
            val operator = operatorItems.getOrNull(index)
            selectedOperatorId = operator?.id
            selectedOperatorName = selected
            binding.tvCompany.setTextColor(ContextCompat.getColor(mActivity, R.color.black))
            clearSelectedPlan()
        }
    }

    private fun setOperatorLoading(loading: Boolean) {
        binding.pbCompanyLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.ivCompanyArrow.visibility = if (loading) View.GONE else View.VISIBLE
        binding.flCompanyAnchor.isClickable = !loading
        binding.flCompanyAnchor.isFocusable = !loading
    }

    private fun showPlanDetails() {
        val plan = selectedPlan ?: return
        binding.tvSelectedPlanDescription.text = plan.description ?: "-"
        binding.tvSelectedValidity.text = plan.validity ?: "-"
        binding.tvSelectedTalktime.text =
            if (plan.talktime == null || plan.talktime < 0) "-" else Utility.formatAmount(plan.talktime)
        binding.tvSelectedData.text = if (plan.data.isNullOrEmpty()) "--" else plan.data
        binding.etAmount.setText(plan.amount?.toString() ?: "")
        binding.cvPlanDetails.visibility = View.VISIBLE
    }

    private fun clearSelectedPlan() {
        isPlanSelected = false
        selectedPlan = null
        binding.cvPlanDetails.visibility = View.GONE
        binding.etAmount.setText("")
    }

    private fun resetFeeDisplay() {
        val hintColor = mActivity.getThemeColor(R.attr.colorTextHint)
        binding.tvPlatformFee.text = getString(R.string.hintPlatformFee)
        binding.tvPlatformFee.setTextColor(hintColor)
        binding.tvTotalPayable.text = getString(R.string.hintTotalPayable)
        binding.tvTotalPayable.setTextColor(hintColor)
    }

    private fun onPlanSelected(plan: DthPlanItem) {
        selectedPlan = plan
        isPlanSelected = true
        showPlanDetails()
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private fun onBrowsePlan() {
        if (selectedOperatorId.isNullOrEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgSelectCompany))
            return
        }
        Utility.hideKeyboard(mActivity)
        DthPlanSelectionActivity.start(mActivity, planSelectionLauncher, selectedOperatorId!!)
    }

    private fun onProceedToPay() {
        val mobileNumber = binding.etMobileNumber.text?.toString()?.trim() ?: ""
        if (mobileNumber.isEmpty()) {
            binding.etMobileNumber.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgMobileNumberEmpty))
            return
        }
        if (mobileNumber.length != 10) {
            binding.etMobileNumber.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgMobileNumberInvalid))
            return
        }
        if (selectedOperatorId.isNullOrEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgSelectCompany))
            return
        }
        if (!isPlanSelected) {
            onBrowsePlan()
            return
        }
        if (!binding.cbTerms.isChecked) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgTermsNotAccepted))
            return
        }
        val amount = binding.etAmount.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
        if (amount <= 0) {
            binding.etAmount.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgAmountEmpty))
            return
        }
        Utility.hideKeyboard(mActivity)
        verifyBalanceAndProcessPayment()
    }

    private fun onReset() {
        binding.etMobileNumber.setText("")
        binding.etAmount.setText("")
        binding.tvCompany.text = getString(R.string.hintSelectCompany)
        binding.tvCompany.setTextColor(mActivity.getThemeColor(R.attr.colorTextHint))
        binding.cbTerms.isChecked = false
        selectedOperatorId = null
        selectedOperatorName = null
        clearSelectedPlan()
        resetFeeDisplay()
        Utility.hideKeyboard(binding.clRoot)
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.lytToolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBackPressedDispatcher.onBackPressed()
                }
                binding.flCompanyAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showCompanyDropdown()
                }
                binding.llBrowsePlan -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressBrowse.get()) return@OnClickListener
                    onBrowsePlan()
                }
                binding.cvChangePlan -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBrowsePlan()
                }
                binding.llProceed -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressPay.get()) return@OnClickListener
                    onProceedToPay()
                }
                binding.llReset -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressPay.get()) return@OnClickListener
                    onReset()
                }
            }
        }
    }
}
