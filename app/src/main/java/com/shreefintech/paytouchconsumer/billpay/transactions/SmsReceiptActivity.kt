package com.shreefintech.paytouchconsumer.billpay.transactions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.billpay.viewmodel.BillPayStatusViewModel
import com.shreefintech.paytouchconsumer.billpay.viewmodel.BillPayTransactionsViewModel
import com.shreefintech.paytouchconsumer.databinding.ActivitySmsReceiptBinding
import com.shreefintech.paytouchconsumer.enums.CategoryType
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayTransactionItem
import com.shreefintech.paytouchconsumer.utill.InAppReviewHelper
import com.shreefintech.paytouchconsumer.utill.ReceiptHelper
import com.shreefintech.paytouchconsumer.utill.ToastType
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.visible

/**
 * SMS Receipt for one Bill Pay category. With a client_reference (from a row or the result screen):
 * GET billpay/status/{ref}. Without one (form tab): the latest payment — transactions?per_page=1 → its ref → status.
 */
class SmsReceiptActivity : BaseActivity() {

    private lateinit var binding: ActivitySmsReceiptBinding
    private val statusViewModel: BillPayStatusViewModel by viewModels()
    private val transactionsViewModel: BillPayTransactionsViewModel by viewModels()
    private val showProgressReceipt = ObservableBoolean(false)

    private val writePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) performDownload()
        else ToastUtil.showDelete(mActivity, getString(R.string.msgStoragePermissionRequired))
    }

    private val category: CategoryType by lazy { CategoryType.from(intent.getStringExtra(EXTRA_CATEGORY)) }

    private val isFromPayment: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_FROM_PAYMENT, false)
    }

    private val clientReference: String? by lazy {
        intent.getStringExtra(EXTRA_CLIENT_REFERENCE)
    }

    companion object {
        private const val EXTRA_CATEGORY         = "extra_category"
        private const val EXTRA_FROM_PAYMENT     = "extra_from_payment"
        private const val EXTRA_CLIENT_REFERENCE = "extra_client_reference"
        private const val TAB_RECEIPT = 0
        private const val TAB_DISPLAY = 1

        fun start(context: Context, category: CategoryType, fromPayment: Boolean = false) {
            context.startActivity(
                Intent(context, SmsReceiptActivity::class.java).apply {
                    putExtra(EXTRA_CATEGORY, category.value)
                    putExtra(EXTRA_FROM_PAYMENT, fromPayment)
                }
            )
        }

        fun start(context: Context, category: CategoryType, clientReference: String, fromPayment: Boolean = false) {
            context.startActivity(
                Intent(context, SmsReceiptActivity::class.java).apply {
                    putExtra(EXTRA_CATEGORY, category.value)
                    putExtra(EXTRA_CLIENT_REFERENCE, clientReference)
                    putExtra(EXTRA_FROM_PAYMENT, fromPayment)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySmsReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        LiquidGlassEffect.attach(
            targetView = binding.flCard,
            rootView = binding.root as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
            strokeColor = ContextCompat.getColor(mActivity, R.color.glass_stroke_primary),
            strokeWidth = 1,
            solidStroke = true,
        )

        if (isFromPayment) {
            binding.llTitleRow.visibility = View.GONE
            binding.llReceiptContent.visibility = View.VISIBLE
            binding.llDisplayContent.visibility = View.GONE
            binding.llBtnContainer.visible()
        } else {
            binding.llTitleRow.visibility = View.VISIBLE
            selectTab(TAB_RECEIPT)
        }
        binding.showProgressReceipt = showProgressReceipt
        binding.onClickListener = onClickListener()
        onBack()

        retryCallback = { loadData() }
        loadData()
    }

    // ── API Call ──────────────────────────────────────────────

    private fun loadData() {
        if (!Utility.isInternetAvailable(mActivity)) {
            showNoInternet()
            return
        }
        hideNoInternet()
        val ref = clientReference
        if (ref != null) loadStatus(ref) else loadLatestPayment()
    }

    // The list row already holds the receipt data; the status call then gets its final state
    private fun loadLatestPayment() {
        transactionsViewModel.loadPage(
            category = category,
            status = null,
            page = 1,
            perPage = 1,
            onLoading = { showProgressReceipt.set(true) },
            onSuccess = { rows ->
                if (isDestroyed) return@loadPage
                val latest = rows.firstOrNull()
                val ref = latest?.clientReference
                if (latest == null || ref == null) {
                    showProgressReceipt.set(false)
                    showEmpty()
                    return@loadPage
                }
                populateReceipt(latest)
                loadStatus(ref)
            },
            onError = { msg ->
                showProgressReceipt.set(false)
                if (isDestroyed) return@loadPage
                showEmpty()
                if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun loadStatus(ref: String) {
        statusViewModel.loadStatus(
            clientReference = ref,
            onLoading = { showProgressReceipt.set(true) },
            onSuccess = { item ->
                showProgressReceipt.set(false)
                if (isDestroyed) return@loadStatus
                populateReceipt(item)
            },
            onError = { msg ->
                showProgressReceipt.set(false)
                if (isDestroyed) return@loadStatus
                // Latest-payment path already shows the list row — only an empty receipt needs the empty state
                if (clientReference != null) showEmpty()
                if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    // ── Populate ──────────────────────────────────────────────

    private fun populateReceipt(item: BillPayTransactionItem) {
        val amount     = Utility.formatAmount(item.amount)
        val account    = transactionsViewModel.accountNumber(item)
        val ref        = item.clientReference ?: "--"
        val date       = Utility.formatDate(item.createdAt)
        val status     = item.status ?: getString(R.string.filterStatusPending)
        val fee        = (item.charge ?: 0.0) + (item.gstAmount ?: 0.0)
        val labelRes = when {
            category.isVehicleCategory -> R.string.labelVehicleNumber
            category.isMobileCategory  -> R.string.labelMobileNo
            else                       -> R.string.labelConsumerNo
        }

        binding.tvConfirmationTitle.setText(category.receiptTitleRes)
        // identifier_label is null for imported old payments
        binding.tvConsumerNoLabel.text = item.identifierLabel?.takeIf { it.isNotBlank() } ?: getString(labelRes)
        binding.tvConsumerNo.text      = account
        // No customer name in billpay/status
        (binding.tvCustomerName.parent as? View)?.visibility = View.GONE
        binding.tvCompanyName.text     = item.billerName ?: "--"
        binding.tvReceiptDate.text     = date
        binding.tvAmountPaid.text      = amount
        binding.tvPaytouchTxnId.text   = transactionsViewModel.txnReferenceOf(item)
        // Reference row: provider_ref, else client_reference
        binding.tvBConnectTxnId.text   = item.providerRef?.takeIf { it.isNotBlank() } ?: ref
        binding.tvCcf.text             = Utility.formatAmount(fee)
        binding.tvReceiptStatus.text   = getString(R.string.labelStatusBullet, status)
        ReceiptHelper.applyStatusStyle(mActivity, binding.cvReceiptStatusBadge, binding.tvReceiptStatus, status)
        if (isFromPayment) InAppReviewHelper.onPaymentReceiptShown(this, item.clientReference, status)

        val smsBodyText = getString(category.smsBodyRes, amount, account)
        val spannable   = SpannableString(smsBodyText)
        val amountStart = smsBodyText.indexOf(amount)
        if (amountStart >= 0) {
            val amountEnd = amountStart + amount.length
            spannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(mActivity, R.color.primary)), amountStart, amountEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(Typeface.BOLD), amountStart, amountEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.tvSmsBody.text        = spannable
        binding.tvSmsBConnectTxn.text = ref
        binding.tvSmsDate.text        = date
    }

    // ── Tab Switching ─────────────────────────────────────────

    private fun selectTab(tab: Int) {
        val isReceipt = tab == TAB_RECEIPT
        binding.llReceiptContent.visibility = if (isReceipt) View.VISIBLE else View.GONE
        binding.llDisplayContent.visibility = if (isReceipt) View.GONE else View.VISIBLE
        binding.llBtnContainer.visibility = if (isReceipt) View.VISIBLE else View.GONE

        val activeColor = ContextCompat.getColor(mActivity, R.color.primary)
        val activeTextColor = ContextCompat.getColor(mActivity, R.color.white)
        val inactiveTextColor = ContextCompat.getColor(mActivity, R.color.primary)

        binding.cvTabReceipt.setCardBackgroundColor(if (isReceipt) activeColor else Color.TRANSPARENT)
        binding.cvTabDisplay.setCardBackgroundColor(if (isReceipt) Color.TRANSPARENT else activeColor)
        binding.tvTabReceipt.setTextColor(if (isReceipt) activeTextColor else inactiveTextColor)
        binding.tvTabDisplay.setTextColor(if (isReceipt) inactiveTextColor else activeTextColor)
    }

    private fun showEmpty() {
        binding.llCardContent.visibility = View.GONE
        binding.tvNoData.visibility      = View.VISIBLE
    }

    // ── Download & Share ──────────────────────────────────────

    private fun downloadReceipt() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        performDownload()
    }

    private fun performDownload() {
        val uri = ReceiptHelper.performDownload(mActivity, binding.cvReceiptCard)
        if (uri != null) {
            ToastUtil.showInActivityWithAction(
                activity = mActivity,
                message = getString(R.string.msgReceiptDownloaded),
                type = ToastType.SUCCESS,
                actionLabel = getString(R.string.btnOpen),
                onAction = { ReceiptHelper.openImageInGallery(mActivity, uri) }
            )
        } else {
            ToastUtil.showDelete(mActivity, getString(R.string.msgReceiptDownloadFailed))
        }
    }

    private fun shareReceipt() {
        ReceiptHelper.shareReceipt(
            activity = mActivity,
            view = binding.cvReceiptCard,
            title = getString(R.string.titleShareReceipt),
            onFailure = { ToastUtil.showDelete(mActivity, getString(R.string.msgReceiptShareFailed)) }
        )
    }

    // ── Lifecycle ─────────────────────────────────────────────

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
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
                binding.cvTabReceipt -> {
                    if (Utility.stopClick()) return@OnClickListener
                    selectTab(TAB_RECEIPT)
                }
                binding.cvTabDisplay -> {
                    if (Utility.stopClick()) return@OnClickListener
                    selectTab(TAB_DISPLAY)
                }
                binding.cvDownload -> {
                    if (Utility.stopClick()) return@OnClickListener
                    downloadReceipt()
                }
                binding.cvShare -> {
                    if (Utility.stopClick()) return@OnClickListener
                    shareReceipt()
                }
            }
        }
    }
}
