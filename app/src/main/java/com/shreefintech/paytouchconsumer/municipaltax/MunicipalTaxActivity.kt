package com.shreefintech.paytouchconsumer.municipaltax

import android.content.Intent
import android.net.Uri
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
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityMunicipalTaxBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.municipaltax.transactions.MunicipalTaxRecentTransactionActivity
import com.shreefintech.paytouchconsumer.municipaltax.transactions.MunicipalTaxSmsReceiptActivity
import com.shreefintech.paytouchconsumer.municipaltax.transactions.MunicipalTaxTransactionReportActivity
import com.shreefintech.paytouchconsumer.municipaltax.transactions.MunicipalTaxTransactionStatusActivity
import com.shreefintech.paytouchconsumer.municipaltax.viewmodel.MunicipalTaxViewModel
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxFetchBillDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxOperatorItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.getThemeColor
import com.shreefintech.paytouchconsumer.widget.CustomDropdown

class MunicipalTaxActivity : BaseActivity() {

    private lateinit var binding: ActivityMunicipalTaxBinding
    private val viewModel: MunicipalTaxViewModel by viewModels()

    private var operatorItems: List<MunicipalTaxOperatorItem> = emptyList()
    private var selectedOperatorId: String? = null
    private var selectedCircleId: String? = null
    private var selectedOperatorName: String? = null
    private var fetchedBillItem: MunicipalTaxFetchBillDataItem? = null
    private var isBillFetched = false

    private val showProgressFetch = ObservableBoolean(false)
    private val showProgressPay   = ObservableBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMunicipalTaxBinding.inflate(layoutInflater)
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

        binding.onClickListener  = onClickListener()
        binding.showProgressFetch = showProgressFetch
        binding.showProgressPay   = showProgressPay
        setupInputFilters()
        setupAmountWatcher()
        setupConsumerNumberWatcher()
        setupTermsText()
        loadOperators()
        onBack()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupInputFilters() {
        val emojiFilter = Utility.EmojiExcludeFilter()
        binding.etConsumerNumber.filters = arrayOf(Utility.digitFilter(), emojiFilter)
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
                val fee   = Utility.calculatePlatformFee(amount)
                val total = amount + fee
                val black = ContextCompat.getColor(mActivity, R.color.black)
                binding.tvPlatformFee.text = getString(R.string.fmtCurrencyAmount).format(fee)
                binding.tvPlatformFee.setTextColor(black)
                binding.tvTotalPayable.text = getString(R.string.fmtCurrencyAmount).format(total)
                binding.tvTotalPayable.setTextColor(black)
            }
        })
    }

    private fun setupConsumerNumberWatcher() {
        binding.etConsumerNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isBillFetched) {
                    isBillFetched   = false
                    fetchedBillItem = null
                    binding.cvBillDetails.visibility = View.GONE
                }
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
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constant.URL_PLATFORM_TERMS)))
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

    private fun fetchBill(consumerNumber: String) {
        viewModel.fetchBill(
            houseNumber = consumerNumber,
            cir         = selectedCircleId ?: "",
            op          = selectedOperatorId ?: "",
            onLoading   = { showProgressFetch.set(true) },
            onSuccess   = { bill ->
                showProgressFetch.set(false)
                fetchedBillItem = bill
                isBillFetched   = true
                showBillDetails()
            },
            onError = { msg ->
                showProgressFetch.set(false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun verifyBalanceAndProcessPayment() {
        val amount = binding.etAmount.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
        val fee    = Utility.calculatePlatformFee(amount)
        val total  = amount + fee
        viewModel.verifyAndPay(
            houseNumber  = binding.etConsumerNumber.text?.toString()?.trim() ?: "",
            op           = selectedOperatorId ?: "",
            cir          = selectedCircleId ?: "",
            amount       = amount,
            total        = total,
            customerName = fetchedBillItem?.userName ?: "",
            dueDate      = fetchedBillItem?.dueDate ?: "",
            opName       = selectedOperatorName ?: "",
            onLoading    = { showProgressPay.set(true) },
            onSuccess    = { _ ->
                showProgressPay.set(false)
                MunicipalTaxSmsReceiptActivity.start(mActivity, fromPayment = true)
                onReset()
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
            activity   = mActivity,
            anchorView = binding.flCompanyAnchor,
            arrowView  = binding.ivCompanyArrow,
            textView   = binding.tvCompany,
            items      = names
        ) { selected, index ->
            selectedOperatorId   = operatorItems.getOrNull(index)?.id
            selectedCircleId     = operatorItems.getOrNull(index)?.circleId
            selectedOperatorName = selected
            binding.tvCompany.setTextColor(ContextCompat.getColor(mActivity, R.color.black))
            if (isBillFetched) {
                isBillFetched   = false
                fetchedBillItem = null
                binding.cvBillDetails.visibility = View.GONE
            }
        }
    }

    private fun setOperatorLoading(loading: Boolean) {
        binding.pbCompanyLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.ivCompanyArrow.visibility   = if (loading) View.GONE else View.VISIBLE
        binding.flCompanyAnchor.isClickable = !loading
        binding.flCompanyAnchor.isFocusable = !loading
    }

    private fun showBillDetails() {
        val bill        = fetchedBillItem ?: return
        val billAmount  = bill.billAmount?.toDoubleOrNull() ?: 0.0
        val fee         = Utility.calculatePlatformFee(billAmount)
        val total       = billAmount + fee
        val black       = ContextCompat.getColor(mActivity, R.color.black)

        binding.tvBillCustomerName.text = bill.userName ?: "-"
        binding.tvBillPropertyNo.text   = bill.cellNumber ?: binding.etConsumerNumber.text?.toString()?.trim() ?: "-"
        binding.tvBillDueDate.text      = bill.dueDate ?: "-"
        binding.tvBillAmount.text       = bill.billAmount ?: "-"
        binding.tvBillPlatformFee.text  = getString(R.string.fmtCurrencyAmount).format(fee)
        binding.tvBillPlatformFee.setTextColor(black)
        binding.tvBillTotalPayable.text = getString(R.string.fmtCurrencyAmount).format(total)
        binding.tvBillTotalPayable.setTextColor(black)

        binding.etAmount.setText(bill.billAmount ?: "")
        binding.cvBillDetails.visibility = View.VISIBLE
    }

    private fun resetFeeDisplay() {
        val hintColor = mActivity.getThemeColor(R.attr.colorTextHint)
        binding.tvPlatformFee.text = getString(R.string.hintPlatformFee)
        binding.tvPlatformFee.setTextColor(hintColor)
        binding.tvTotalPayable.text = getString(R.string.hintTotalPayable)
        binding.tvTotalPayable.setTextColor(hintColor)
    }

    private fun onClearBill() {
        isBillFetched   = false
        fetchedBillItem = null
        binding.cvBillDetails.visibility = View.GONE
        binding.etAmount.setText("")
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private fun validateInputs(): Boolean {
        val consumerNumber = binding.etConsumerNumber.text?.toString()?.trim() ?: ""
        if (consumerNumber.isEmpty()) {
            binding.etConsumerNumber.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgConsumerNumberEmpty))
            return false
        }
        if (consumerNumber.length < 10) {
            binding.etConsumerNumber.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgConsumerNumberInvalid))
            return false
        }
        if (selectedOperatorId.isNullOrEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgSelectCompany))
            return false
        }
        return true
    }

    private fun onFetchBill() {
        if (!validateInputs()) return
        Utility.hideKeyboard(mActivity)
        fetchBill(binding.etConsumerNumber.text?.toString()?.trim() ?: "")
    }

    private fun onProceedToPay() {
        if (!validateInputs()) return
        if (!isBillFetched) {
            Utility.hideKeyboard(mActivity)
            fetchBill(binding.etConsumerNumber.text?.toString()?.trim() ?: "")
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
        binding.etConsumerNumber.setText("")
        binding.etAmount.setText("")
        binding.tvCompany.text = getString(R.string.hintSelectCompany)
        binding.tvCompany.setTextColor(mActivity.getThemeColor(R.attr.colorTextHint))
        binding.cbTerms.isChecked = false
        selectedOperatorId   = null
        selectedCircleId     = null
        selectedOperatorName = null
        isBillFetched        = false
        fetchedBillItem      = null
        binding.cvBillDetails.visibility = View.GONE
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
                binding.llTabReport -> {
                    if (Utility.stopClick()) return@OnClickListener
                    startActivity(Intent(mActivity, MunicipalTaxTransactionReportActivity::class.java))
                }
                binding.llTabStatus -> {
                    if (Utility.stopClick()) return@OnClickListener
                    startActivity(Intent(mActivity, MunicipalTaxTransactionStatusActivity::class.java))
                }
                binding.llTabSmsReceipt -> {
                    if (Utility.stopClick()) return@OnClickListener
                    MunicipalTaxSmsReceiptActivity.start(mActivity)
                }
                binding.llRecentTransactions -> {
                    if (Utility.stopClick()) return@OnClickListener
                    MunicipalTaxRecentTransactionActivity.start(mActivity)
                }
                binding.flCompanyAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showCompanyDropdown()
                }
                binding.llFetchBill -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressFetch.get()) return@OnClickListener
                    onFetchBill()
                }
                binding.cvClearBill -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onClearBill()
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
