package com.shreefintech.paytouchconsumer.loan

import android.content.Intent
import android.graphics.Color
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
import com.shreefintech.paytouchconsumer.databinding.ActivityLoanBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.loan.viewmodel.LoanViewModel
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanBillItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanOperatorItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.getThemeColor
import com.shreefintech.paytouchconsumer.widget.CustomDropdown

class LoanActivity : BaseActivity() {

    private lateinit var binding: ActivityLoanBinding
    private val viewModel: LoanViewModel by viewModels()

    private var operatorItems: List<LoanOperatorItem> = emptyList()
    private var selectedOperatorId: String? = null
    private var selectedOperatorName: String? = null
    private var fetchedBillItem: LoanBillItem? = null
    private var isBillFetched = false

    private val showProgressFetch = ObservableBoolean(false)
    private val showProgressPay = ObservableBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoanBinding.inflate(layoutInflater)
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
        binding.showProgressFetch = showProgressFetch
        binding.showProgressPay = showProgressPay
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

    private fun setupConsumerNumberWatcher() {
        binding.etConsumerNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isBillFetched) {
                    isBillFetched = false
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
        binding.tvTerms.highlightColor = Color.TRANSPARENT
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
            consumerNumber = consumerNumber,
            operatorId = selectedOperatorId ?: "",
            onLoading = { showProgressFetch.set(true) },
            onSuccess = { bill ->
                showProgressFetch.set(false)
                fetchedBillItem = bill
                isBillFetched = true
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
        val fee = Utility.calculatePlatformFee(amount)
        val total = amount + fee
        val consumerNumber = binding.etConsumerNumber.text?.toString()?.trim() ?: ""
        viewModel.verifyAndPay(
            consumerNumber = consumerNumber,
            operatorId = selectedOperatorId ?: "",
            amount = amount,
            fee = fee,
            total = total,
            onLoading = { showProgressPay.set(true) },
            onSuccess = { _ ->
                showProgressPay.set(false)
                ToastUtil.showSuccess(mActivity, getString(R.string.msgPaymentSuccess))
                // TODO(B2C-70): navigate to LoanSmsReceiptActivity once implemented
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
            activity = mActivity,
            anchorView = binding.flCompanyAnchor,
            arrowView = binding.ivCompanyArrow,
            textView = binding.tvCompany,
            items = names
        ) { selected, index ->
            selectedOperatorId = operatorItems.getOrNull(index)?.id
            selectedOperatorName = selected
            binding.tvCompany.setTextColor(ContextCompat.getColor(mActivity, R.color.black))
            if (isBillFetched) {
                isBillFetched = false
                fetchedBillItem = null
                binding.cvBillDetails.visibility = View.GONE
            }
        }
    }

    private fun setOperatorLoading(loading: Boolean) {
        binding.pbCompanyLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.ivCompanyArrow.visibility = if (loading) View.GONE else View.VISIBLE
        binding.flCompanyAnchor.isClickable = !loading
        binding.flCompanyAnchor.isFocusable = !loading
    }

    private fun showBillDetails() {
        val bill = fetchedBillItem ?: return
        binding.tvBillCustomerName.text = bill.customerName ?: "-"
        binding.tvBillDueDate.text = bill.dueDate ?: "-"
        binding.tvBillDate.text = bill.billDate ?: "-"
        binding.tvBillAmount.text = Utility.formatAmount(bill.billAmount)
        binding.tvBillConnectionNumber.text = binding.etConsumerNumber.text?.toString()?.trim() ?: "-"
        binding.tvBillOperator.text = selectedOperatorName ?: "-"
        binding.etAmount.setText(bill.billAmount ?: "")
        binding.cvBillDetails.visibility = View.VISIBLE
    }

    private fun onClearBill() {
        isBillFetched = false
        fetchedBillItem = null
        binding.cvBillDetails.visibility = View.GONE
        binding.etAmount.setText("")
    }

    private fun resetFeeDisplay() {
        val hintColor = mActivity.getThemeColor(R.attr.colorTextHint)
        binding.tvPlatformFee.text = getString(R.string.hintPlatformFee)
        binding.tvPlatformFee.setTextColor(hintColor)
        binding.tvTotalPayable.text = getString(R.string.hintTotalPayable)
        binding.tvTotalPayable.setTextColor(hintColor)
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private fun onFetchBill() {
        if (selectedOperatorId.isNullOrEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgSelectCompany))
            return
        }
        val consumerNumber = binding.etConsumerNumber.text?.toString()?.trim() ?: ""
        if (consumerNumber.isEmpty()) {
            binding.etConsumerNumber.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgConsumerNumberEmpty))
            return
        }
        if (consumerNumber.length < 10) {
            binding.etConsumerNumber.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgConsumerNumberInvalid))
            return
        }
        Utility.hideKeyboard(mActivity)
        fetchBill(consumerNumber)
    }

    private fun onProceedToPay() {
        val consumerNumber = binding.etConsumerNumber.text?.toString()?.trim() ?: ""
        if (consumerNumber.isEmpty()) {
            binding.etConsumerNumber.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgConsumerNumberEmpty))
            return
        }
        if (consumerNumber.length < 10) {
            binding.etConsumerNumber.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgConsumerNumberInvalid))
            return
        }
        if (selectedOperatorId.isNullOrEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgSelectCompany))
            return
        }
        if (!isBillFetched) {
            Utility.hideKeyboard(mActivity)
            fetchBill(consumerNumber)
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
        selectedOperatorId = null
        selectedOperatorName = null
        isBillFetched = false
        fetchedBillItem = null
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
                    // TODO(B2C-70): implement LoanTransactionReportActivity
                }
                binding.llTabStatus -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(B2C-70): implement LoanTransactionStatusActivity
                }
                binding.llTabSmsReceipt -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(B2C-70): implement LoanSmsReceiptActivity
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
                binding.flCompanyAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showCompanyDropdown()
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
                binding.llRecentTransactions -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(B2C-70): implement LoanRecentTransactionActivity
                }
            }
        }
    }
}
