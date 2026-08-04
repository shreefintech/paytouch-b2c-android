package com.shreefintech.paytouchconsumer.postpaid.transactions

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
import com.shreefintech.paytouchconsumer.databinding.ActivityPostpaidSmsReceiptBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.postpaid.viewmodel.PostpaidSmsReceiptViewModel
import com.shreefintech.paytouchconsumer.retrofit.model.postpaid.PostpaidLatestPaymentDataItem
import com.shreefintech.paytouchconsumer.utill.ReceiptHelper
import com.shreefintech.paytouchconsumer.utill.ToastType
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.visible

class PostpaidSmsReceiptActivity : BaseActivity() {

    private lateinit var binding: ActivityPostpaidSmsReceiptBinding
    private val showProgressReceipt = ObservableBoolean(false)

    private val viewModel: PostpaidSmsReceiptViewModel by viewModels()

    private val writePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) performDownload()
        else ToastUtil.showDelete(mActivity, getString(R.string.msgStoragePermissionRequired))
    }

    private val isFromPayment: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_FROM_PAYMENT, false)
    }

    companion object {
        private const val EXTRA_FROM_PAYMENT = "extra_from_payment"
        private const val TAB_RECEIPT = 0
        private const val TAB_DISPLAY = 1

        fun start(context: Context, fromPayment: Boolean = false) {
            context.startActivity(
                Intent(context, PostpaidSmsReceiptActivity::class.java).apply {
                    putExtra(EXTRA_FROM_PAYMENT, fromPayment)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostpaidSmsReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        LiquidGlassEffect.attach(
            targetView = binding.flCard,
            rootView = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
            strokeColor = Color.argb(180, 213, 38, 98),
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
        loadLatestPayments()
        // TODO(PAYTOUCH-570): Add showNoInternet() / hideNoInternet() / setNoInternetRetryCallback { loadLatestPayments() }
    }

    // ── API Call ──────────────────────────────────────────────

    private fun loadLatestPayments() {
        viewModel.getLatestPayments(
            onLoading = { showReceiptLoading(true) },
            onSuccess = { item ->
                showReceiptLoading(false)
                populateReceiptFromApi(item)
            },
            onError = { msg ->
                showReceiptLoading(false)
                if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    // ── Populate ──────────────────────────────────────────────

    private fun populateReceiptFromApi(item: PostpaidLatestPaymentDataItem) {
        val amount   = Utility.formatAmount(item.totalPayable)
        val mobileNo = item.connectionNumber ?: item.subscriberNo ?: "--"
        val txnId    = item.transactionId ?: "--"
        val date     = Utility.formatDate(item.createdAt)
        val status   = item.status ?: "Pending"

        binding.tvConsumerNoLabel.text = getString(R.string.labelMobileNo)
        binding.tvConsumerNo.text     = mobileNo
        binding.tvCustomerName.text   = item.customerName ?: "--"
        binding.tvCompanyName.text    = item.operatorName ?: item.subservice ?: "--"
        binding.tvReceiptDate.text    = date
        binding.tvAmountPaid.text     = amount
        binding.tvPaytouchTxnId.text  = txnId
        binding.tvBConnectTxnId.text  = txnId
        binding.tvCcf.text            = Utility.formatAmount(item.platformFee)
        binding.tvReceiptStatus.text  = getString(R.string.labelStatusBullet, status)
        ReceiptHelper.applyStatusStyle(mActivity, binding.cvReceiptStatusBadge, binding.tvReceiptStatus, status)

        val smsBodyText = getString(R.string.msgPostpaidSmsBody, amount, mobileNo)
        val spannable   = SpannableString(smsBodyText)
        val amountStart = smsBodyText.indexOf(amount)
        if (amountStart >= 0) {
            val amountEnd = amountStart + amount.length
            spannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(mActivity, R.color.primary)), amountStart, amountEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(Typeface.BOLD), amountStart, amountEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.tvSmsBody.text        = spannable
        binding.tvSmsBConnectTxn.text = txnId
        binding.tvSmsDate.text        = date
    }

    // ── Tab Switching ─────────────────────────────────────────

    private fun selectTab(tab: Int) {
        val isReceipt = tab == TAB_RECEIPT
        binding.llReceiptContent.visibility = if (isReceipt) View.VISIBLE else View.GONE
        binding.llDisplayContent.visibility = if (isReceipt) View.GONE else View.VISIBLE
        binding.llBtnContainer.visibility   = if (isReceipt) View.VISIBLE else View.GONE

        val activeColor      = ContextCompat.getColor(mActivity, R.color.primary)
        val inactiveColor    = Color.TRANSPARENT
        val activeTextColor  = ContextCompat.getColor(mActivity, R.color.white)
        val inactiveTextColor = ContextCompat.getColor(mActivity, R.color.primary)

        binding.cvTabReceipt.setCardBackgroundColor(if (isReceipt) activeColor else inactiveColor)
        binding.cvTabDisplay.setCardBackgroundColor(if (isReceipt) inactiveColor else activeColor)
        binding.tvTabReceipt.setTextColor(if (isReceipt) activeTextColor else inactiveTextColor)
        binding.tvTabDisplay.setTextColor(if (isReceipt) inactiveTextColor else activeTextColor)
    }

    // ── Loading State ─────────────────────────────────────────

    private fun showReceiptLoading(show: Boolean) {
        showProgressReceipt.set(show)
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
                activity    = mActivity,
                message     = getString(R.string.msgReceiptDownloaded),
                type        = ToastType.SUCCESS,
                actionLabel = getString(R.string.btnOpen),
                onAction    = { ReceiptHelper.openImageInGallery(mActivity, uri) }
            )
        } else {
            ToastUtil.showDelete(mActivity, getString(R.string.msgReceiptDownloadFailed))
        }
    }

    private fun shareReceipt() {
        ReceiptHelper.shareReceipt(
            activity  = mActivity,
            view      = binding.cvReceiptCard,
            title     = getString(R.string.titleShareReceipt),
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
