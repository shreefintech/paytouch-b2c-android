package com.shreefintech.paytouchconsumer.electricity.transactions

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
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityTransactionDetailBinding
import com.shreefintech.paytouchconsumer.electricity.model.TransactionItem
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.Utility

class TransactionDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityTransactionDetailBinding

    companion object {
        private const val EXTRA_MOBILE = "extra_mobile"
        private const val EXTRA_TXN_ID = "extra_txn_id"
        private const val EXTRA_AMOUNT = "extra_amount"
        private const val EXTRA_STATUS = "extra_status"
        private const val EXTRA_USERNAME = "extra_username"
        private const val EXTRA_DATE = "extra_date"
        private const val EXTRA_PLATFORM_FEE = "extra_platform_fee"
        private const val EXTRA_TOTAL = "extra_total"
        private const val EXTRA_REF_ID = "extra_ref_id"
        private const val EXTRA_USER_ID = "extra_user_id"
        private const val EXTRA_ACCOUNT_NO = "extra_account_no"
        private const val EXTRA_COMPANY = "extra_company"

        fun start(context: Context, item: TransactionItem) {
            context.startActivity(
                Intent(context, TransactionDetailActivity::class.java).apply {
                    putExtra(EXTRA_MOBILE, item.mobileNumber)
                    putExtra(EXTRA_TXN_ID, item.transactionId)
                    putExtra(EXTRA_AMOUNT, item.amount)
                    putExtra(EXTRA_STATUS, item.status)
                    putExtra(EXTRA_USERNAME, item.username)
                    putExtra(EXTRA_DATE, item.date)
                    putExtra(EXTRA_PLATFORM_FEE, item.platformFee)
                    putExtra(EXTRA_TOTAL, item.totalPayable)
                    putExtra(EXTRA_REF_ID, item.referenceId)
                    putExtra(EXTRA_USER_ID, item.userId)
                    putExtra(EXTRA_ACCOUNT_NO, item.accountNumber)
                    putExtra(EXTRA_COMPANY, item.companyName)
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
            targetView = binding.flCard,
            rootView = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
            strokeColor = Color.argb(180, 213, 38, 98),
            strokeWidth = 1,
            solidStroke = true,
        )

        populateData()
        binding.onClickListener = onClickListener()
        onBack()
    }

    private fun openSmsReceipt() {
        SmsReceiptActivity.start(
            context = mActivity,
            mobile = intent.getStringExtra(EXTRA_MOBILE) ?: "",
            txnId = intent.getStringExtra(EXTRA_TXN_ID) ?: "",
            amount = intent.getStringExtra(EXTRA_AMOUNT) ?: "",
            status = intent.getStringExtra(EXTRA_STATUS) ?: "",
            username = intent.getStringExtra(EXTRA_USERNAME) ?: "",
            date = intent.getStringExtra(EXTRA_DATE) ?: "",
            platformFee = intent.getStringExtra(EXTRA_PLATFORM_FEE) ?: "",
            refId = intent.getStringExtra(EXTRA_REF_ID) ?: "",
            accountNo = intent.getStringExtra(EXTRA_ACCOUNT_NO) ?: "",
            companyName = intent.getStringExtra(EXTRA_COMPANY) ?: ""
        )
    }

    private fun populateData() {
        val mobile = intent.getStringExtra(EXTRA_MOBILE) ?: ""
        val txnId = intent.getStringExtra(EXTRA_TXN_ID) ?: ""
        val amount = intent.getStringExtra(EXTRA_AMOUNT) ?: ""
        val status = intent.getStringExtra(EXTRA_STATUS) ?: ""
        val username = intent.getStringExtra(EXTRA_USERNAME) ?: ""
        val date = intent.getStringExtra(EXTRA_DATE) ?: ""
        val platformFee = intent.getStringExtra(EXTRA_PLATFORM_FEE) ?: ""
        val total = intent.getStringExtra(EXTRA_TOTAL) ?: ""
        val refId = intent.getStringExtra(EXTRA_REF_ID) ?: ""
        val userId = intent.getStringExtra(EXTRA_USER_ID) ?: ""

        binding.tvMobileNumber.text = getString(R.string.labelMobileNoFmt, mobile)
        binding.tvUsername.text = getString(R.string.labelUsernameFmt, username)
        binding.tvInfoAmount.text = amount
        binding.tvStatus.text = status
        binding.tvDate.text = date
        binding.tvPaymentAmount.text = amount
        binding.tvPlatformFee.text = platformFee
        binding.tvTotalPayable.text = total
        binding.tvTransactionId.text = txnId
        binding.tvReferenceId.text = refId
        binding.tvUserId.text = userId

        val (bgColor, textColor) = when (status) {
            "Success" -> Pair(R.color.toast_bg_success, R.color.toast_text_success)
            "Failed" -> Pair(R.color.toast_bg_delete, R.color.form_wizard_reject)
            else -> Pair(R.color.toast_bg_warning, R.color.orange)
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
                binding.cvSmsReceipt -> {
                    if (Utility.stopClick()) return@OnClickListener
                    openSmsReceipt()
                }
            }
        }
    }
}
