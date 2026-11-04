package com.shreefintech.paytouchconsumer.billpay

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
import com.shreefintech.paytouchconsumer.billpay.transactions.RecentTransactionActivity
import com.shreefintech.paytouchconsumer.billpay.transactions.SmsReceiptActivity
import com.shreefintech.paytouchconsumer.billpay.transactions.TransactionReportActivity
import com.shreefintech.paytouchconsumer.billpay.transactions.TransactionStatusActivity
import com.shreefintech.paytouchconsumer.billpay.viewmodel.BillPayViewModel
import com.shreefintech.paytouchconsumer.databinding.ActivityBillPayBinding
import com.shreefintech.paytouchconsumer.databinding.DialogMpinBinding
import com.shreefintech.paytouchconsumer.databinding.ItemBillPayFieldBinding
import com.shreefintech.paytouchconsumer.enums.CategoryType
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.location.LocationPermissionHelper
import com.shreefintech.paytouchconsumer.operator.OperatorSelectionActivity
import com.shreefintech.paytouchconsumer.operator.model.OperatorSelectionItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayBillItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayBillerItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayCircleItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayFeePreviewItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayPlanItem
import com.shreefintech.paytouchconsumer.transactions.BillPaymentStatusActivity
import com.shreefintech.paytouchconsumer.transactions.model.BillPaymentStatusItem
import com.shreefintech.paytouchconsumer.utill.BankDetailsDialogHelper
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.TabAnimationHelper
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.getThemeColor
import com.shreefintech.paytouchconsumer.utill.clearMpin
import com.shreefintech.paytouchconsumer.utill.createDialog
import com.shreefintech.paytouchconsumer.utill.getMpin
import com.shreefintech.paytouchconsumer.utill.sendLocation
import com.shreefintech.paytouchconsumer.utill.setupMpinBoxes
import com.shreefintech.paytouchconsumer.widget.CustomDropdown
import java.math.BigDecimal

/**
 * One Bill Pay screen for every category ([CategoryType] in [EXTRA_CATEGORY]).
 * Bill categories fetch the amount (Fetch Bill); plan categories pick it (Browse Plan; prepaid needs a circle first).
 */
class BillPayActivity : BaseActivity() {

    private lateinit var binding: ActivityBillPayBinding
    private val viewModel: BillPayViewModel by viewModels()
    private lateinit var tabHelper: TabAnimationHelper

    private val category: CategoryType by lazy { CategoryType.from(intent.getStringExtra(EXTRA_CATEGORY)) }

    private var billers: List<BillPayBillerItem> = emptyList()
    private var selectedBiller: BillPayBillerItem? = null
    // One entry per drawn account field: input_params key → its view
    private val fieldBindings = ArrayList<Pair<String, ItemBillPayFieldBinding>>()

    private var circles: List<BillPayCircleItem> = emptyList()
    private var selectedCircleCode: String? = null

    private var selectedPlan: BillPayPlanItem? = null
    private var fetchedBill: BillPayBillItem? = null

    // Kept only for the amount it was asked for
    private var feePreview: BillPayFeePreviewItem? = null
    private var feePreviewAmount: String? = null

    private var paymentMade = false

    private val showProgressFetch = ObservableBoolean(false)
    private val showProgressPay = ObservableBoolean(false)

    private var mpinDialog: Dialog? = null
    private var mpinDialogBinding: DialogMpinBinding? = null

    private val feeHandler = Handler(Looper.getMainLooper())
    private val feeRunnable = Runnable { loadFeePreview() }

    private val locationHelper = LocationPermissionHelper(this) { location ->
        location?.let { viewModel.sendLocation(it) }
    }

    private val operatorSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val json = result.data?.getStringExtra(OperatorSelectionActivity.EXTRA_SELECTED) ?: return@registerForActivityResult
        val item = Gson().fromJson(json, OperatorSelectionItem::class.java)
        val biller = billers.firstOrNull { it.id?.toString() == item.id } ?: return@registerForActivityResult
        onBillerSelected(biller)
    }

    private val planSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val json = result.data?.getStringExtra(BillPayPlanSelectionActivity.EXTRA_SELECTED_PLAN)
        val plan = json?.let { Gson().fromJson(it, BillPayPlanItem::class.java) } ?: return@registerForActivityResult
        onPlanSelected(plan)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillPayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, maxOf(imeInsets.bottom, systemBars.bottom))
            if (imeInsets.bottom > 0) Utility.scrollToFocused(mActivity)
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
        binding.showProgressFetch = showProgressFetch
        binding.showProgressPay = showProgressPay
        binding.tvTitle.setText(category.titleRes)
        binding.llCircle.visibility = if (category.needsCircle) View.VISIBLE else View.GONE
        binding.cvFetchBill.visibility = if (category.isPlanBased) View.GONE else View.VISIBLE
        binding.cvBrowsePlan.visibility = if (category.isPlanBased) View.VISIBLE else View.GONE
        binding.etAmount.filters = arrayOf(Utility.EmojiExcludeFilter())
        buildInputFields(null)
        setupAmountWatcher()
        setupTermsText()
        retryCallback = { loadInitialData() }
        loadInitialData()
        tabHelper = TabAnimationHelper(mActivity, binding.llTabPayBill, binding.llTabReport, binding.llTabStatus, binding.llTabSmsReceipt)
        onBack()
    }

    override fun onResume() {
        super.onResume()
        if (paymentMade) { paymentMade = false; onReset() }
        tabHelper.resetAll()
        tabHelper.selectPayBill()
    }

    override fun onDestroy() {
        feeHandler.removeCallbacks(feeRunnable)
        viewModel.cancelFeePreview()
        mpinDialog?.dismiss()
        super.onDestroy()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupAmountWatcher() {
        updateProceedButton(false)
        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val amount = s?.toString()?.trim()?.toDoubleOrNull()
                updateProceedButton(amount != null && amount > 0)
                feeHandler.removeCallbacks(feeRunnable)
                viewModel.cancelFeePreview()
                feePreview = null
                feePreviewAmount = null
                resetFeeDisplay()
                if (amount != null && amount > 0) feeHandler.postDelayed(feeRunnable, FEE_PREVIEW_DEBOUNCE_MS)
            }
        })
    }

    private fun updateProceedButton(enabled: Boolean) {
        binding.llProceed.isEnabled = enabled
        binding.cvProceed.alpha = if (enabled) 1f else 0.5f
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
                    Utility.openUrl(mActivity, Constant.URL_PLATFORM_TERMS)
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
        binding.tvTerms.highlightColor = Color.TRANSPARENT
    }

    // ── API Calls (via ViewModel) ─────────────────────────────────────────────

    private fun loadInitialData() {
        if (!Utility.isInternetAvailable(mActivity)) {
            showNoInternet()
            return
        }
        hideNoInternet()
        loadBillers()
        if (category.needsCircle) loadCircles()
    }

    private fun loadBillers() {
        viewModel.loadBillers(
            category = category.value,
            onLoading = { setDropdownLoading(binding.pbCompanyLoading, binding.ivCompanyArrow, binding.flCompanyAnchor, true) },
            onSuccess = { list ->
                if (isDestroyed) return@loadBillers
                setDropdownLoading(binding.pbCompanyLoading, binding.ivCompanyArrow, binding.flCompanyAnchor, false)
                billers = list
            },
            onError = { msg ->
                if (isDestroyed) return@loadBillers
                setDropdownLoading(binding.pbCompanyLoading, binding.ivCompanyArrow, binding.flCompanyAnchor, false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun loadCircles() {
        viewModel.loadCircles(
            onLoading = { setDropdownLoading(binding.pbCircleLoading, binding.ivCircleArrow, binding.flCircleAnchor, true) },
            onSuccess = { list ->
                if (isDestroyed) return@loadCircles
                setDropdownLoading(binding.pbCircleLoading, binding.ivCircleArrow, binding.flCircleAnchor, false)
                circles = list
            },
            onError = { msg ->
                if (isDestroyed) return@loadCircles
                setDropdownLoading(binding.pbCircleLoading, binding.ivCircleArrow, binding.flCircleAnchor, false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun fetchBill() {
        val billerId = selectedBiller?.id?.toString() ?: return
        viewModel.fetchBill(
            billerId = billerId,
            inputParams = collectInputParams(),
            onLoading = { showProgressFetch.set(true) },
            onSuccess = { bill ->
                showProgressFetch.set(false)
                if (isDestroyed) return@fetchBill
                // has_bill false → "No bill found", no bill card, nothing to pay
                if (bill.hasBill != true) {
                    ToastUtil.showWarning(mActivity, getString(R.string.msgNoBillFound))
                    return@fetchBill
                }
                fetchedBill = bill
                showBillDetails(bill)
            },
            onError = { msg ->
                showProgressFetch.set(false)
                if (isDestroyed) return@fetchBill
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun loadFeePreview() {
        val amount = currentAmountText()
        if ((amount.toDoubleOrNull() ?: 0.0) <= 0) return
        viewModel.loadFeePreview(
            amount = amount,
            onLoading = {},
            onSuccess = { preview ->
                if (isDestroyed || amount != currentAmountText()) return@loadFeePreview
                feePreview = preview
                feePreviewAmount = amount
                val black = ContextCompat.getColor(mActivity, R.color.black)
                binding.tvPlatformFee.text = Utility.formatAmount((preview.fee ?: 0.0) + (preview.gstAmount ?: 0.0))
                binding.tvPlatformFee.setTextColor(black)
                binding.tvTotalPayable.text = Utility.formatAmount(preview.total)
                binding.tvTotalPayable.setTextColor(black)
            },
            onError = { msg ->
                if (isDestroyed) return@loadFeePreview
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun pay(mpin: String) {
        val billerId = selectedBiller?.id?.toString() ?: return
        viewModel.pay(
            billerId = billerId,
            inputParams = collectInputParams(),
            amount = currentAmountText(),
            mpin = mpin,
            onLoading = { showProgressPay.set(true) },
            onSuccess = { item ->
                showProgressPay.set(false)
                if (isFinishing || isDestroyed) return@pay
                mpinDialog?.dismiss()
                paymentMade = true
                BillPaymentStatusActivity.start(
                    mActivity,
                    BillPaymentStatusItem(
                        clientReference = item.clientReference ?: "--",
                        txnReference = item.txnReference?.takeIf { it.isNotBlank() } ?: "--",
                        amount = (item.amount ?: currentAmountText().toDoubleOrNull())?.toString() ?: "",
                        status = item.status ?: "",
                        isFinal = item.isFinal == true,
                        category = category.value
                    )
                )
            },
            onError = { msg, errorCode ->
                showProgressPay.set(false)
                if (isFinishing || isDestroyed) return@pay
                onPayError(msg, errorCode)
            },
            onNotConfirmed = { msg ->
                showProgressPay.set(false)
                if (isFinishing || isDestroyed) return@pay
                mpinDialog?.dismiss()
                ToastUtil.showWarning(mActivity, msg)
                // Reset so a quick retry can't pay twice — the payment shows in Recent Transactions
                onReset()
            }
        )
    }

    private fun onPayError(msg: String, errorCode: String?) {
        when (errorCode) {
            Constant.ERROR_CODE_BANK_DETAILS_REQUIRED -> {
                mpinDialog?.dismiss()
                // Local flag was stale — Save in the popup retries Proceed to Pay
                SharedPreferenceHelper.setSharedPreferenceBoolean(mActivity, Constant.KEY_BANK_DETAILS_ADDED, false)
                BankDetailsDialogHelper.runIfBankAdded(mActivity) { onProceedToPay() }
            }
            Constant.ERROR_CODE_LOCATION_REQUIRED -> {
                mpinDialog?.dismiss()
                ToastUtil.showWarning(mActivity, msg)
                // Resends location; the user taps Pay again
                locationHelper.start()
            }
            null -> {
                // 422 (amount, params, MPIN) etc. — form and popup stay
                mpinDialogBinding?.clearMpin()
                ToastUtil.showDelete(mActivity, msg, inWindow = mpinDialog?.isShowing != true)
            }
            else -> {
                // AADHAAR / KYC / USER_BLOCKED — ApiHelper already navigated
                mpinDialog?.dismiss()
                ToastUtil.showDelete(mActivity, msg)
            }
        }
    }

    // ── Company / account fields ──────────────────────────────────────────────

    private fun showBillerSelection() {
        Utility.hideKeyboard(binding.clRoot)
        if (billers.isEmpty()) {
            loadBillers()
            ToastUtil.showWarning(mActivity, getString(R.string.msgLoadingOperators))
            return
        }
        val items = billers.mapNotNull { biller ->
            val id = biller.id?.toString() ?: return@mapNotNull null
            OperatorSelectionItem(id = id, name = biller.name ?: "")
        }
        operatorSelectionLauncher.launch(
            OperatorSelectionActivity.newIntent(mActivity, items, selectedBiller?.id?.toString())
        )
    }

    private fun onBillerSelected(biller: BillPayBillerItem) {
        selectedBiller = biller
        binding.tvCompany.text = biller.name ?: "-"
        binding.tvCompany.setTextColor(ContextCompat.getColor(mActivity, R.color.black))
        clearFetchedBill()
        clearSelectedPlan()
        buildInputFields(biller)
    }

    /**
     * One field per input_params entry; none → the category default key; no default → biller can't be paid yet.
     * [biller] null = no company picked yet: the category's own field is shown so the form isn't empty.
     * Values typed under the same key are kept across company changes.
     */
    private fun buildInputFields(biller: BillPayBillerItem?) {
        val typed = fieldBindings.associate { (key, field) -> key to field.etField.text?.toString().orEmpty() }
        binding.llInputFields.removeAllViews()
        fieldBindings.clear()
        val categoryLabel = getString(category.fieldLabelRes)
        val fields = if (biller == null) {
            // Key is only a placeholder when the category has no default — validateAccount() needs a company first
            listOf((category.defaultParamName ?: "") to categoryLabel)
        } else {
            biller.inputParams.orEmpty().mapNotNull { param ->
                val key = param.resolvedKey ?: return@mapNotNull null
                key to (param.label?.takeIf { it.isNotBlank() } ?: categoryLabel)
            }.ifEmpty {
                val defaultKey = category.defaultParamName ?: run {
                    ToastUtil.showDelete(mActivity, getString(R.string.msgBillerDetailsUnavailable))
                    return
                }
                listOf(defaultKey to categoryLabel)
            }
        }
        fields.forEach { (key, label) ->
            val field = ItemBillPayFieldBinding.inflate(layoutInflater, binding.llInputFields, true)
            field.tvFieldLabel.text = label
            field.etField.hint = getString(R.string.hintEnterField, label)
            if (key == Constant.BILLPAY_PARAM_MOBILE_NUMBER) {
                field.etField.inputType = InputType.TYPE_CLASS_NUMBER
                field.etField.filters = arrayOf(InputFilter.LengthFilter(MOBILE_NUMBER_LENGTH), Utility.digitFilter(), Utility.EmojiExcludeFilter())
            } else {
                field.etField.filters = arrayOf(Utility.EmojiExcludeFilter())
            }
            field.etField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    // A fetched bill belongs to the account it was fetched for
                    if (fetchedBill != null) clearFetchedBill()
                }
            })
            // The placeholder field (key "") hands its value to a single-field biller
            (typed[key] ?: typed[""]?.takeIf { fields.size == 1 })?.let { field.etField.setText(it) }
            fieldBindings.add(key to field)
        }
    }

    private fun collectInputParams(): Map<String, String> {
        val params = LinkedHashMap<String, String>()
        fieldBindings.forEach { (key, field) -> params[key] = field.etField.text?.toString()?.trim().orEmpty() }
        if (category.needsCircle) selectedCircleCode?.let { params[Constant.BILLPAY_PARAM_CIRCLE_CODE] = it }
        return params
    }

    private fun showCircleDropdown() {
        Utility.hideKeyboard(binding.clRoot)
        if (circles.isEmpty()) {
            loadCircles()
            ToastUtil.showWarning(mActivity, getString(R.string.msgLoadingCircles))
            return
        }
        CustomDropdown.showDropdown(
            activity = mActivity,
            anchorView = binding.flCircleAnchor,
            arrowView = binding.ivCircleArrow,
            textView = binding.tvCircle,
            items = circles.map { it.name ?: "" }
        ) { _, index ->
            val code = circles.getOrNull(index)?.code ?: return@showDropdown
            selectedCircleCode = code
            binding.tvCircle.setTextColor(ContextCompat.getColor(mActivity, R.color.black))
            clearSelectedPlan()
        }
    }

    // Small spinner replaces the arrow in the field slot
    private fun setDropdownLoading(progress: View, arrow: View, anchor: View, loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        arrow.visibility = if (loading) View.GONE else View.VISIBLE
        anchor.isClickable = !loading
        anchor.isFocusable = !loading
    }

    // ── Bill / plan ───────────────────────────────────────────────────────────

    private fun showBillDetails(bill: BillPayBillItem) {
        binding.tvBillCustomerName.text = bill.customerName ?: "-"
        binding.tvBillDueDate.text = Utility.formatDate(bill.dueDate, "dd/MM/yyyy")
        binding.tvBillDate.text = Utility.formatDate(bill.billDate, "dd/MM/yyyy")
        binding.tvBillAmount.text = Utility.formatAmount(bill.amount ?: 0.0)
        binding.tvBillConnectionNumber.text = fieldBindings.firstOrNull()?.second?.etField?.text?.toString()?.trim() ?: "-"
        binding.tvBillOperator.text = selectedBiller?.name ?: "-"
        binding.etAmount.setText((bill.amount ?: 0.0).toPlainAmount())
        binding.cvBillDetails.visibility = View.VISIBLE
    }

    private fun clearFetchedBill() {
        if (fetchedBill == null && binding.cvBillDetails.visibility != View.VISIBLE) return
        fetchedBill = null
        binding.cvBillDetails.visibility = View.GONE
        binding.etAmount.setText("")
    }

    private fun onBrowsePlan() {
        val billerId = selectedBiller?.id?.toString()
        if (billerId == null) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgSelectCompany))
            return
        }
        if (category.needsCircle && selectedCircleCode.isNullOrEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgSelectCircle))
            return
        }
        Utility.hideKeyboard(mActivity)
        BillPayPlanSelectionActivity.start(
            mActivity, planSelectionLauncher, billerId, selectedCircleCode.takeIf { category.needsCircle }
        )
    }

    private fun onPlanSelected(plan: BillPayPlanItem) {
        selectedPlan = plan
        binding.tvSelectedPlanDescription.text = plan.description ?: "-"
        binding.tvSelectedValidity.text = plan.validity ?: "-"
        binding.tvSelectedData.text = if (plan.data.isNullOrEmpty()) "--" else plan.data
        binding.etAmount.setText(plan.amount?.toPlainAmount() ?: "")
        binding.cvPlanDetails.visibility = View.VISIBLE
    }

    private fun clearSelectedPlan() {
        if (selectedPlan == null) return
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

    private fun currentAmountText() = binding.etAmount.text?.toString()?.trim().orEmpty()

    // 1240.0 → "1240", 199.5 → "199.5"
    private fun Double.toPlainAmount(): String = BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Null when every account field is filled, else the first empty field's message (focus moved to it). */
    private fun validateAccount(): String? {
        if (selectedBiller == null) return getString(R.string.msgSelectCompany)
        if (fieldBindings.isEmpty()) return getString(R.string.msgBillerDetailsUnavailable)
        if (category.needsCircle && selectedCircleCode.isNullOrEmpty()) return getString(R.string.msgSelectCircle)
        fieldBindings.forEach { (key, field) ->
            val value = field.etField.text?.toString()?.trim().orEmpty()
            val invalid = value.isEmpty() ||
                (key == Constant.BILLPAY_PARAM_MOBILE_NUMBER && value.length != MOBILE_NUMBER_LENGTH)
            if (invalid) {
                field.etField.requestFocus()
                return getString(R.string.msgFieldRequired, field.tvFieldLabel.text)
            }
        }
        return null
    }

    private fun onFetchBill() {
        validateAccount()?.let { ToastUtil.showDelete(mActivity, it); return }
        Utility.hideKeyboard(mActivity)
        fetchBill()
    }

    private fun onProceedToPay() {
        validateAccount()?.let { ToastUtil.showDelete(mActivity, it); return }
        if (category.isPlanBased && selectedPlan == null) {
            onBrowsePlan()
            return
        }
        if (!category.isPlanBased && fetchedBill == null) {
            Utility.hideKeyboard(mActivity)
            fetchBill()
            return
        }
        if (!binding.cbTerms.isChecked) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgTermsNotAccepted))
            return
        }
        val amount = currentAmountText()
        if ((amount.toDoubleOrNull() ?: 0.0) <= 0) {
            binding.etAmount.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgAmountEmpty))
            return
        }
        // The fee must be shown before paying — ask again if it isn't loaded for this amount
        if (feePreview == null || feePreviewAmount != amount) {
            loadFeePreview()
            ToastUtil.showWarning(mActivity, getString(R.string.msgFeePreviewLoading))
            return
        }
        Utility.hideKeyboard(mActivity)
        showMpinDialog()
    }

    // Same MPIN popup as Load Wallet / Earning Wallet — the pay call goes out from its Submit
    private fun showMpinDialog() {
        if (mpinDialog?.isShowing == true) return
        val dialogBinding = DialogMpinBinding.inflate(layoutInflater)
        mpinDialogBinding = dialogBinding
        dialogBinding.showProgress = showProgressPay
        dialogBinding.setupMpinBoxes()
        dialogBinding.onClickListener = onClickListener()
        val dialog = dialogBinding.createDialog(mActivity)
        dialog.setOnDismissListener {
            mpinDialog = null
            mpinDialogBinding = null
        }
        mpinDialog = dialog
        dialog.show()
    }

    private fun submitMpin(dialogBinding: DialogMpinBinding) {
        Utility.hideKeyboard(dialogBinding.root)
        val mpin = dialogBinding.getMpin()
        val msg = when {
            mpin.isEmpty() -> getString(R.string.msgMpinEmpty)
            mpin.length != MPIN_LENGTH -> getString(R.string.msgMpinInvalid)
            else -> null
        }
        if (msg != null) {
            ToastUtil.showDelete(mActivity, msg, inWindow = false)
            return
        }
        pay(mpin)
    }

    private fun onReset() {
        feeHandler.removeCallbacks(feeRunnable)
        viewModel.cancelFeePreview()
        binding.tvCompany.text = getString(R.string.hintSelectCompany)
        binding.tvCompany.setTextColor(mActivity.getThemeColor(R.attr.colorTextHint))
        binding.tvCircle.text = getString(R.string.labelSelectCircle)
        binding.tvCircle.setTextColor(mActivity.getThemeColor(R.attr.colorTextHint))
        binding.cbTerms.isChecked = false
        selectedBiller = null
        selectedCircleCode = null
        // Clear first so the default field comes back empty
        binding.llInputFields.removeAllViews()
        fieldBindings.clear()
        buildInputFields(null)
        clearFetchedBill()
        clearSelectedPlan()
        binding.etAmount.setText("")
        Utility.hideKeyboard(binding.clRoot)
    }

    private fun animateTabAndNavigate(tab: LinearLayout, navigate: () -> Unit) =
        tabHelper.animateAndNavigate(tab, navigate)

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mpinDialog?.isShowing == true) {
                    if (!showProgressPay.get()) mpinDialog?.dismiss()
                    return
                }
                finish()
            }
        })
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.lytToolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBackPressedDispatcher.onBackPressed()
                }
                binding.llTabReport -> {
                    if (Utility.stopClick()) return@OnClickListener
                    animateTabAndNavigate(binding.llTabReport) { TransactionReportActivity.start(mActivity, category) }
                }
                binding.llTabStatus -> {
                    if (Utility.stopClick()) return@OnClickListener
                    animateTabAndNavigate(binding.llTabStatus) { TransactionStatusActivity.start(mActivity, category) }
                }
                binding.llTabSmsReceipt -> {
                    if (Utility.stopClick()) return@OnClickListener
                    animateTabAndNavigate(binding.llTabSmsReceipt) { SmsReceiptActivity.start(mActivity, category) }
                }
                binding.flCompanyAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showBillerSelection()
                }
                binding.flCircleAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showCircleDropdown()
                }
                binding.llFetchBill -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressFetch.get()) return@OnClickListener
                    onFetchBill()
                }
                binding.llBrowsePlan, binding.cvChangePlan -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBrowsePlan()
                }
                binding.cvClearBill -> {
                    if (Utility.stopClick()) return@OnClickListener
                    clearFetchedBill()
                }
                binding.llProceed -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressPay.get()) return@OnClickListener
                    if (paymentMade) return@OnClickListener
                    BankDetailsDialogHelper.runIfBankAdded(mActivity) { onProceedToPay() }
                }
                binding.llReset -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressPay.get()) return@OnClickListener
                    onReset()
                }
                binding.llRecentTransactions -> {
                    if (Utility.stopClick()) return@OnClickListener
                    RecentTransactionActivity.start(mActivity, category)
                }
                mpinDialogBinding?.cardClose -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressPay.get()) return@OnClickListener
                    mpinDialog?.dismiss()
                }
                mpinDialogBinding?.cardSubmit -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressPay.get()) return@OnClickListener
                    mpinDialogBinding?.let { submitMpin(it) }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_CATEGORY = "extra_category"
        private const val FEE_PREVIEW_DEBOUNCE_MS = 400L
        private const val MOBILE_NUMBER_LENGTH = 10
        private const val MPIN_LENGTH = 4

        fun start(context: Context, category: CategoryType) {
            context.startActivity(
                Intent(context, BillPayActivity::class.java).putExtra(EXTRA_CATEGORY, category.value)
            )
        }
    }
}
