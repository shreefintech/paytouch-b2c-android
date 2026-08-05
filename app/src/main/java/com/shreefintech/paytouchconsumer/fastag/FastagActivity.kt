package com.shreefintech.paytouchconsumer.fastag

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
import com.shreefintech.paytouchconsumer.databinding.ActivityFastagBinding
import com.shreefintech.paytouchconsumer.fastag.transactions.FastagTransactionReportActivity
import com.shreefintech.paytouchconsumer.fastag.transactions.FastagTransactionStatusActivity
import com.shreefintech.paytouchconsumer.fastag.viewmodel.FastagViewModel
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.retrofit.model.fastag.FastagOperatorItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.getThemeColor
import com.shreefintech.paytouchconsumer.widget.CustomDropdown

class FastagActivity : BaseActivity() {

    private lateinit var binding: ActivityFastagBinding
    private val viewModel: FastagViewModel by viewModels()

    private var operatorItems: List<FastagOperatorItem> = emptyList()
    private var selectedOperatorBillerId: String? = null
    private var selectedOperatorName: String? = null
    private var selectedCircleId: String? = null
    private val showProgressPay = ObservableBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFastagBinding.inflate(layoutInflater)
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
        binding.showProgressPay = showProgressPay
        setupInputFilters()
        setupAmountWatcher()
        setupTermsText()
        loadOperators()
        onBack()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupInputFilters() {
        binding.etVehicleNumber.filters = arrayOf(Utility.EmojiExcludeFilter())
        binding.etAmount.filters = arrayOf(Utility.EmojiExcludeFilter())
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
                binding.tvPlatformFee.text = Utility.formatAmount(fee.toString())
                binding.tvPlatformFee.setTextColor(black)
                binding.tvTotalPayable.text = Utility.formatAmount(total.toString())
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

    private fun verifyBalanceAndProcessPayment() {
        val amount = binding.etAmount.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
        val fee = Utility.calculatePlatformFee(amount)
        val total = amount + fee
        val vehicleNumber = binding.etVehicleNumber.text?.toString()?.trim() ?: ""
        viewModel.verifyAndPay(
            vehicleNumber = vehicleNumber,
            operator = selectedOperatorBillerId ?: "",
            operatorName = selectedOperatorName ?: "",
            circle = selectedCircleId ?: "",
            amount = amount,
            fee = fee,
            total = total,
            onLoading = { showProgressPay.set(true) },
            onSuccess = { _ ->
                showProgressPay.set(false)
                // TODO(PAYTOUCH-69): navigate to FastagSmsReceiptActivity
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
            val op = operatorItems.getOrNull(index)
            selectedOperatorBillerId = op?.billerId
            selectedOperatorName = selected
            selectedCircleId = op?.circleId
            binding.tvCompany.setTextColor(ContextCompat.getColor(mActivity, R.color.black))
        }
    }

    private fun setOperatorLoading(loading: Boolean) {
        binding.pbCompanyLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.ivCompanyArrow.visibility = if (loading) View.GONE else View.VISIBLE
        binding.flCompanyAnchor.isClickable = !loading
        binding.flCompanyAnchor.isFocusable = !loading
    }

    private fun resetFeeDisplay() {
        val hintColor = mActivity.getThemeColor(R.attr.colorTextHint)
        binding.tvPlatformFee.text = getString(R.string.hintPlatformFee)
        binding.tvPlatformFee.setTextColor(hintColor)
        binding.tvTotalPayable.text = getString(R.string.hintTotalPayable)
        binding.tvTotalPayable.setTextColor(hintColor)
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private fun onProceedToPay() {
        val vehicleNumber = binding.etVehicleNumber.text?.toString()?.trim() ?: ""
        if (vehicleNumber.isEmpty()) {
            binding.etVehicleNumber.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgVehicleNumberEmpty))
            return
        }
        if (selectedOperatorBillerId.isNullOrEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgSelectCompany))
            return
        }
        val amount = binding.etAmount.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
        if (amount <= 0) {
            binding.etAmount.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgAmountEmpty))
            return
        }
        if (!binding.cbTerms.isChecked) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgTermsNotAccepted))
            return
        }
        Utility.hideKeyboard(mActivity)
        verifyBalanceAndProcessPayment()
    }

    private fun onReset() {
        binding.etVehicleNumber.setText("")
        binding.etAmount.setText("")
        binding.tvCompany.text = getString(R.string.hintSelectCompany)
        binding.tvCompany.setTextColor(mActivity.getThemeColor(R.attr.colorTextHint))
        binding.cbTerms.isChecked = false
        selectedOperatorBillerId = null
        selectedOperatorName = null
        selectedCircleId = null
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
                    startActivity(Intent(mActivity, FastagTransactionReportActivity::class.java))
                }
                binding.llTabStatus -> {
                    if (Utility.stopClick()) return@OnClickListener
                    startActivity(Intent(mActivity, FastagTransactionStatusActivity::class.java))
                }
                binding.llTabSmsReceipt -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-69): FastagSmsReceiptActivity.start(mActivity)
                }
                binding.llRecentTransactions -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-69): FastagRecentTransactionActivity.start(mActivity)
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
            }
        }
    }
}
