package com.shreefintech.paytouchconsumer.transactions

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityTransactionDetailBinding
import com.shreefintech.paytouchconsumer.dth.transactions.DthSmsReceiptActivity
import com.shreefintech.paytouchconsumer.electricity.transactions.SmsReceiptActivity
import com.shreefintech.paytouchconsumer.fastag.transactions.FastagSmsReceiptActivity
import com.shreefintech.paytouchconsumer.gas.transactions.GasSmsReceiptActivity
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.loan.transactions.LoanSmsReceiptActivity
import com.shreefintech.paytouchconsumer.municipaltax.transactions.MunicipalTaxSmsReceiptActivity
import com.shreefintech.paytouchconsumer.postpaid.transactions.PostpaidSmsReceiptActivity
import com.shreefintech.paytouchconsumer.prepaid.transactions.PrepaidSmsReceiptActivity
import com.shreefintech.paytouchconsumer.transactions.model.TransactionItem
import com.shreefintech.paytouchconsumer.utill.Utility

class TransactionDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityTransactionDetailBinding

    private val transactionItem: TransactionItem? by lazy {
        intent.getStringExtra(EXTRA_ITEM)?.let { Gson().fromJson(it, TransactionItem::class.java) }
    }

    companion object {
        private const val EXTRA_ITEM = "extra_item"

        fun start(context: Context, item: TransactionItem) {
            context.startActivity(
                Intent(context, TransactionDetailActivity::class.java).apply {
                    putExtra(EXTRA_ITEM, Gson().toJson(item))
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
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

        populateData()
        binding.onClickListener = onClickListener()
        onBack()
    }

    private fun populateData() {
        val item = transactionItem ?: return
        val numberLabelFmt = when {
            item.isVehicleCategory -> R.string.labelVehicleNumberFmt
            item.isMobileCategory  -> R.string.labelMobileNoFmt
            else                   -> R.string.labelConsumerNoFmt
        }
        binding.tvMobileNumber.text  = getString(numberLabelFmt, item.mobileNumber)
        binding.tvUsername.text      = getString(R.string.labelUsernameFmt, item.username)
        binding.tvStatus.text        = item.status
        binding.tvDate.text          = Utility.formatDate(item.date, "dd/MM/yyyy")
        binding.tvPaymentAmount.text = item.amount
        binding.tvPlatformFee.text   = item.platformFee
        binding.tvTotalPayable.text  = item.totalPayable
        binding.tvTransactionId.text = item.transactionId
        binding.llViewReceipt.visibility = if (item.categoryType.isNotEmpty() && item.transactionId != "--") View.VISIBLE else View.GONE

        val (bgColor, textColor) = when (item.status.lowercase()) {
            "success" -> Pair(R.color.toast_bg_success, R.color.toast_text_success)
            "failed"  -> Pair(R.color.toast_bg_delete, R.color.form_wizard_reject)
            else      -> Pair(R.color.toast_bg_warning, R.color.orange)
        }
        binding.cvStatus.setCardBackgroundColor(ContextCompat.getColor(mActivity, bgColor))
        binding.tvStatus.setTextColor(ContextCompat.getColor(mActivity, textColor))
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
                binding.llViewReceipt -> {
                    if (Utility.stopClick()) return@OnClickListener
                    val item = transactionItem ?: return@OnClickListener
                    val txnId = item.transactionId.takeIf { it != "--" } ?: return@OnClickListener
                    when (item.categoryType) {
                        "electricity"  -> SmsReceiptActivity.start(mActivity, txnId)
                        "gas"          -> GasSmsReceiptActivity.start(mActivity, txnId)
                        "prepaid"      -> PrepaidSmsReceiptActivity.start(mActivity, txnId)
                        "postpaid"     -> PostpaidSmsReceiptActivity.start(mActivity, txnId)
                        "dth"          -> DthSmsReceiptActivity.start(mActivity, txnId)
                        "fastag"       -> FastagSmsReceiptActivity.start(mActivity, txnId)
                        "loan"         -> LoanSmsReceiptActivity.start(mActivity, txnId)
                        "municipaltax" -> MunicipalTaxSmsReceiptActivity.start(mActivity, txnId)
                    }
                }
            }
        }
    }
}
