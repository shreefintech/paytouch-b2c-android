package com.shreefintech.paytouchconsumer.electricity

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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityElectricityBinding
import com.shreefintech.paytouchconsumer.electricity.transactions.RecentTransactionActivity
import com.shreefintech.paytouchconsumer.electricity.transactions.SmsReceiptActivity
import com.shreefintech.paytouchconsumer.electricity.transactions.TransactionReportActivity
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.VpsBalanceItem
import com.shreefintech.paytouchconsumer.retrofit.model.WalletDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityBillItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityProcessPaymentRequest
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.getThemeColor
import androidx.databinding.ObservableBoolean
import com.shreefintech.paytouchconsumer.widget.CustomDropdown
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ElectricityActivity : BaseActivity() {

    private lateinit var binding: ActivityElectricityBinding

    private var operatorItems: List<ElectricityOperatorItem> = emptyList()
    private var selectedOperatorId: String? = null
    private var selectedOperatorName: String? = null
    private var fetchedBillItem: ElectricityBillItem? = null
    private var billFetched = false

    private val showProgressFetch = ObservableBoolean(false)
    private val showProgressPay = ObservableBoolean(false)

    private val adminHttpClient by lazy { okhttp3.OkHttpClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityElectricityBinding.inflate(layoutInflater)
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
                binding.tvPlatformFee.text = "₹%.2f".format(fee)
                binding.tvPlatformFee.setTextColor(black)
                binding.tvTotalPayable.text = "₹%.2f".format(total)
                binding.tvTotalPayable.setTextColor(black)
            }
        })
    }

    private fun setupConsumerNumberWatcher() {
        binding.etConsumerNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (billFetched) {
                    billFetched = false
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

    // ── API Calls ─────────────────────────────────────────────────────────────

    private fun loadOperators() {
        if (!Utility.isInternetAvailable(mActivity)) return
        setOperatorLoading(true)
        ApiClient.apiService.getElectricityOperators(bearerToken())
            .enqueue(object : Callback<General<List<ElectricityOperatorItem>>> {
                override fun onResponse(
                    call: Call<General<List<ElectricityOperatorItem>>>,
                    response: Response<General<List<ElectricityOperatorItem>>>
                ) {
                    setOperatorLoading(false)
                    if (response.isSuccessful && response.body()?.data != null) {
                        operatorItems = response.body()!!.data!!
                    } else {
                        ToastUtil.showDelete(
                            mActivity,
                            ApiHelper.parseErrorMessage(
                                mActivity, response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }
                override fun onFailure(
                    call: Call<General<List<ElectricityOperatorItem>>>,
                    t: Throwable
                ) {
                    setOperatorLoading(false)
                    ToastUtil.showDelete(mActivity, t.localizedMessage ?: getString(R.string.err_generic))
                }
            })
    }

    private fun fetchBill(connectionNumber: String) {
        if (!Utility.isInternetAvailable(mActivity)) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
            return
        }
        showProgressFetch.set(true)
        val request = ElectricityFetchBillRequest(
            connectionNumber = connectionNumber,
            operatorId = selectedOperatorId ?: "",
            circleId = "00"
        )
        ApiClient.apiService.fetchElectricityBill(bearerToken(), request)
            .enqueue(object : Callback<General<List<ElectricityBillItem>>> {
                override fun onResponse(
                    call: Call<General<List<ElectricityBillItem>>>,
                    response: Response<General<List<ElectricityBillItem>>>
                ) {
                    showProgressFetch.set(false)
                    if (response.isSuccessful && response.body()?.data != null) {
                        val bill = response.body()!!.data!!.firstOrNull()
                        if (bill != null) {
                            fetchedBillItem = bill
                            billFetched = true
                            showBillDetails()
                        } else {
                            ToastUtil.showDelete(mActivity, getString(R.string.err_generic))
                        }
                    } else {
                        ToastUtil.showDelete(
                            mActivity,
                            ApiHelper.parseErrorMessage(
                                mActivity, response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }
                override fun onFailure(call: Call<General<List<ElectricityBillItem>>>, t: Throwable) {
                    showProgressFetch.set(false)
                    ToastUtil.showDelete(mActivity, t.localizedMessage ?: getString(R.string.err_generic))
                }
            })
    }

    private fun verifyBalanceAndProcessPayment() {
        val amount = binding.etAmount.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
        val fee = Utility.calculatePlatformFee(amount)
        val total = amount + fee
        showProgressPay.set(true)
        checkVpsBalance(total) { processPayment(amount, fee, total) }
    }

    private fun checkVpsBalance(totalPayable: Double, onSufficient: () -> Unit) {
        val userId = SharedPreferenceHelper.getSharedPreferenceString(
            mActivity, Constant.KEY_USER_ID, ""
        ) ?: ""
        val url = "${Constant.BASE_URL_ADMIN}balance.php?id=$userId"
        val request = okhttp3.Request.Builder().url(url).build()
        adminHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    if (!isFinishing) checkWalletBalance(totalPayable, onSufficient)
                }
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    try {
                        val item = Gson().fromJson(body, VpsBalanceItem::class.java)
                        val balance = item?.balance?.toDoubleOrNull() ?: 0.0
                        if (balance >= totalPayable) onSufficient()
                        else checkWalletBalance(totalPayable, onSufficient)
                    } catch (e: Exception) {
                        checkWalletBalance(totalPayable, onSufficient)
                    }
                }
            }
        })
    }

    private fun checkWalletBalance(totalPayable: Double, onSufficient: () -> Unit) {
        if (!Utility.isInternetAvailable(mActivity)) {
            showProgressPay.set(false)
            ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.getUserWalletData(bearerToken())
            .enqueue(object : Callback<General<WalletDataItem>> {
                override fun onResponse(
                    call: Call<General<WalletDataItem>>,
                    response: Response<General<WalletDataItem>>
                ) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        val balance =
                            response.body()!!.data!!.walletBalance?.toDoubleOrNull() ?: 0.0
                        if (balance >= totalPayable) {
                            onSufficient()
                        } else {
                            showProgressPay.set(false)
                            ToastUtil.showDelete(
                                mActivity, getString(R.string.msgInsufficientBalance)
                            )
                        }
                    } else {
                        showProgressPay.set(false)
                        ToastUtil.showDelete(
                            mActivity,
                            ApiHelper.parseErrorMessage(
                                mActivity, response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }
                override fun onFailure(call: Call<General<WalletDataItem>>, t: Throwable) {
                    showProgressPay.set(false)
                    ToastUtil.showDelete(mActivity, t.localizedMessage ?: getString(R.string.err_generic))
                }
            })
    }

    private fun processPayment(amount: Double, fee: Double, total: Double) {
        if (!Utility.isInternetAvailable(mActivity)) {
            showProgressPay.set(false)
            ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
            return
        }
        val connectionNumber = binding.etConsumerNumber.text?.toString()?.trim() ?: ""
        val request = ElectricityProcessPaymentRequest(
            connectionNumber = connectionNumber,
            operatorId = selectedOperatorId ?: "",
            circleId = "00",
            amount = amount,
            platformFee = fee,
            totalPayable = total
        )
        ApiClient.apiService.processElectricityPayment(bearerToken(), request)
            .enqueue(object : Callback<ElectricityPaymentItem> {
                override fun onResponse(
                    call: Call<ElectricityPaymentItem>,
                    response: Response<ElectricityPaymentItem>
                ) {
                    showProgressPay.set(false)
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        navigateToReceipt(amount, fee, body)
                    } else {
                        val errMsg = body?.message
                            ?: ApiHelper.parseErrorMessage(
                                mActivity, response.code(), response.errorBody()?.string()
                            )
                        ToastUtil.showDelete(mActivity, errMsg)
                    }
                }
                override fun onFailure(call: Call<ElectricityPaymentItem>, t: Throwable) {
                    showProgressPay.set(false)
                    ToastUtil.showDelete(mActivity, t.localizedMessage ?: getString(R.string.err_generic))
                }
            })
    }

    private fun navigateToReceipt(amount: Double, fee: Double, paymentItem: ElectricityPaymentItem) {
        val mobile = SharedPreferenceHelper.getSharedPreferenceString(
            mActivity, Constant.KEY_MOBILE, ""
        ) ?: ""
        val date = SimpleDateFormat("dd-MM-yyyy, hh:mm a", Locale.getDefault()).format(Date())
        SmsReceiptActivity.start(
            context = mActivity,
            mobile = mobile,
            txnId = paymentItem.transactionId ?: "",
            amount = "₹%.2f".format(paymentItem.amount ?: amount),
            status = paymentItem.status ?: "Pending",
            username = fetchedBillItem?.userName ?: "",
            date = date,
            platformFee = "₹%.2f".format(paymentItem.platformFee ?: fee),
            refId = paymentItem.reqId ?: "",
            accountNo = binding.etConsumerNumber.text?.toString()?.trim() ?: "",
            companyName = selectedOperatorName ?: ""
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
            if (billFetched) {
                billFetched = false
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
        binding.tvBillCustomerName.text = bill.userName ?: "-"
        binding.tvBillDueDate.text = bill.dueDate ?: "-"
        binding.tvBillDate.text = bill.billDate ?: "-"
        binding.tvBillAmount.text = bill.billAmount ?: "-"
        binding.tvBillConnectionNumber.text =
            bill.cellNumber ?: binding.etConsumerNumber.text?.toString()?.trim() ?: "-"
        binding.tvBillOperator.text = selectedOperatorName ?: "-"
        binding.etAmount.setText(bill.billAmount ?: "")
        binding.cvBillDetails.visibility = View.VISIBLE
    }

    private fun onClearBill() {
        billFetched = false
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

    private fun bearerToken(): String {
        val token = SharedPreferenceHelper.getSharedPreferenceString(
            mActivity, Constant.KEY_TOKEN, ""
        ) ?: ""
        return "Bearer $token"
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private fun onFetchBill() {
        if (selectedOperatorId.isNullOrEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgSelectCompany))
            return
        }
        val connectionNumber = binding.etConsumerNumber.text?.toString()?.trim() ?: ""
        if (connectionNumber.isEmpty()) {
            binding.etConsumerNumber.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgConsumerNumberEmpty))
            return
        }
        if (connectionNumber.length < 10) {
            binding.etConsumerNumber.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgConsumerNumberInvalid))
            return
        }
        Utility.hideKeyboard(mActivity)
        fetchBill(connectionNumber)
    }

    private fun onProceedToPay() {
        val connectionNumber = binding.etConsumerNumber.text?.toString()?.trim() ?: ""
        if (connectionNumber.isEmpty()) {
            binding.etConsumerNumber.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgConsumerNumberEmpty))
            return
        }
        if (connectionNumber.length < 10) {
            binding.etConsumerNumber.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgConsumerNumberInvalid))
            return
        }
        if (selectedOperatorId.isNullOrEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgSelectCompany))
            return
        }
        if (!billFetched) {
            Utility.hideKeyboard(mActivity)
            fetchBill(connectionNumber)
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
        billFetched = false
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
                    startActivity(Intent(mActivity, TransactionReportActivity::class.java))
                }
                binding.llTabStatus -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-546): Navigate to transaction status check screen
                }
                binding.llTabSmsReceipt -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-546): Pass real transaction data once API is wired
                    SmsReceiptActivity.start(
                        context = mActivity,
                        mobile = "9876543210",
                        txnId = "BC88213045",
                        amount = "₹149.00",
                        status = "Success",
                        username = "Ravi Kumar",
                        date = "18-07-2026, 09:15 am",
                        platformFee = "₹3.00",
                        refId = "TXN10235",
                        accountNo = "30723111936",
                        companyName = "Paschim Gujarat Vij Company Ltd"
                    )
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
                    startActivity(Intent(mActivity, RecentTransactionActivity::class.java))
                }
            }
        }
    }
}
