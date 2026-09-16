package com.shreefintech.paytouchconsumer.transactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityTransactionHistoryDetailBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.retrofit.model.transactions.TransactionHistoryDetailItem
import com.shreefintech.paytouchconsumer.transactions.viewmodel.TransactionHistoryDetailViewModel
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class TransactionReportDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityTransactionHistoryDetailBinding
    private val viewModel: TransactionHistoryDetailViewModel by viewModels()

    private val transactionId: String by lazy {
        intent.getStringExtra(EXTRA_TRANSACTION_ID) ?: ""
    }

    companion object {
        private const val EXTRA_TRANSACTION_ID = "extra_transaction_id"

        fun start(context: Context, transactionId: String) {
            context.startActivity(
                Intent(context, TransactionReportDetailActivity::class.java).apply {
                    putExtra(EXTRA_TRANSACTION_ID, transactionId)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        LiquidGlassEffect.attach(
            targetView   = binding.flCard,
            rootView     = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion   = 0f,
            blur         = resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
            strokeColor  = ContextCompat.getColor(mActivity, R.color.glass_stroke_primary),
            strokeWidth  = 1,
            solidStroke  = true,
        )

        binding.onClickListener = onClickListener()
        onBack()

        retryCallback = { loadDetail() }
        loadDetail()
    }

    private fun loadDetail() {
        if (!Utility.isInternetAvailable(mActivity)) {
            showNoInternet()
            return
        }
        hideNoInternet()
        viewModel.loadDetail(
            transactionId = transactionId,
            onLoading     = { showLoading(true) },
            onSuccess     = { item ->
                showLoading(false)
                populateData(item)
            },
            onError       = { msg ->
                showLoading(false)
                binding.tvEmpty.visibility = View.VISIBLE
                if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun populateData(item: TransactionHistoryDetailItem) {
        binding.svContent.visibility = View.VISIBLE

        binding.tvAmount.text          = Utility.formatAmount(item.amount)
        binding.tvTransactionId.text   = item.referenceId ?: "--"
        binding.tvIdentifierLabel.text = item.identifierLabel ?: "--"
        binding.tvIdentifier.text      = item.identifier ?: "--"
        binding.tvDate.text            = Utility.formatDate(item.createdAt, "dd/MM/yyyy, h:mm a")

        val status = item.status ?: "--"
        binding.tvStatus.text = status.replaceFirstChar { it.uppercaseChar() }
        val (bgColor, textColor) = when (status.lowercase()) {
            "success" -> Pair(R.color.toast_bg_success, R.color.toast_text_success)
            "failed"  -> Pair(R.color.toast_bg_delete, R.color.form_wizard_reject)
            else      -> Pair(R.color.toast_bg_warning, R.color.orange)
        }
        binding.cvStatus.setCardBackgroundColor(ContextCompat.getColor(mActivity, bgColor))
        binding.tvStatus.setTextColor(ContextCompat.getColor(mActivity, textColor))

        val isHdfc = item.type == "hdfc_smartgateway"
        if (isHdfc) {
            item.paymentMethodDisplayName?.let { method ->
                binding.llPaymentMethod.visibility = View.VISIBLE
                binding.tvPaymentMethod.text       = method
            }
            item.rrn?.let { rrn ->
                binding.llBankRefNo.visibility = View.VISIBLE
                binding.tvBankRefNo.text       = rrn
            }
        } else {
            item.platformFee?.let { fee ->
                binding.llPlatformFee.visibility = View.VISIBLE
                binding.tvPlatformFee.text       = Utility.formatAmount(fee)
            }
            item.totalPayable?.let { total ->
                binding.llTotalPayable.visibility = View.VISIBLE
                binding.tvTotalPayable.text       = Utility.formatAmount(total)
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) binding.svContent.visibility = View.GONE
    }

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
            }
        }
    }
}
