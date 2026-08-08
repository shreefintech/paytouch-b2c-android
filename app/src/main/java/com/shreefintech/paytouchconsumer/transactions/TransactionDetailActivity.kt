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
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
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
            strokeColor  = Color.argb(180, 213, 38, 98),
            strokeWidth  = 1,
            solidStroke  = true,
        )

        populateData()
        binding.onClickListener = onClickListener()
        onBack()
    }

    private fun populateData() {
        val item = transactionItem ?: return
        val numberLabelFmt = if (item.isMobileCategory) R.string.labelMobileNoFmt else R.string.labelConsumerNoFmt
        binding.tvMobileNumber.text  = getString(numberLabelFmt, item.mobileNumber)
        binding.tvUsername.text      = getString(R.string.labelUsernameFmt, item.username)
        binding.tvInfoAmount.text    = item.amount
        binding.tvStatus.text        = item.status
        binding.tvDate.text          = Utility.formatDate(item.date, "dd/MM/yyyy")
        binding.tvPaymentAmount.text = item.amount
        binding.tvPlatformFee.text   = item.platformFee
        binding.tvTotalPayable.text  = item.totalPayable
        binding.tvTransactionId.text = item.transactionId

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
            }
        }
    }
}
