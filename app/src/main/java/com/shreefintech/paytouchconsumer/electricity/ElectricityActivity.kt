package com.shreefintech.paytouchconsumer.electricity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityElectricityBinding
import com.shreefintech.paytouchconsumer.electricity.transactions.RecentTransactionActivity
import com.shreefintech.paytouchconsumer.electricity.transactions.SmsReceiptActivity
import com.shreefintech.paytouchconsumer.electricity.transactions.TransactionReportActivity
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.getThemeColor
import com.shreefintech.paytouchconsumer.widget.CustomDropdown

class ElectricityActivity : BaseActivity() {

    private lateinit var binding: ActivityElectricityBinding

    private val operatorList = listOf(
        "MSEDCL", "TPDDL", "BSES Rajdhani", "BSES Yamuna",
        "BESCOM", "TNEB", "KSEB", "CESC", "Torrent Power", "UPPCL"
    )
    private var selectedOperator: String? = null

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
        onBack()
    }

    private fun showCompanyDropdown() {
        Utility.hideKeyboard(binding.clRoot)
        CustomDropdown.showDropdown(
            activity = mActivity,
            anchorView = binding.flCompanyAnchor,
            arrowView = binding.ivCompanyArrow,
            textView = binding.tvCompany,
            items = operatorList
        ) { selected, _ ->
            selectedOperator = selected
            binding.tvCompany.setTextColor(ContextCompat.getColor(mActivity, R.color.black))
        }
    }

    private fun onProceedToPay() {
        val consumerNumber = binding.etConsumerNumber.text?.toString()?.trim() ?: ""
        val amount = binding.etAmount.text?.toString()?.trim() ?: ""

        if (consumerNumber.isEmpty()) {
            binding.etConsumerNumber.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgConsumerNumberEmpty))
            return
        }
        if (selectedOperator.isNullOrEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgSelectCompany))
            return
        }
        if (amount.isEmpty()) {
            binding.etAmount.requestFocus()
            ToastUtil.showDelete(mActivity, getString(R.string.msgAmountEmpty))
            return
        }
        if (!binding.cbTerms.isChecked) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgTermsNotAccepted))
            return
        }
        // TODO(PAYTOUCH-520): Call fetch-bill API then navigate to payment confirmation
    }

    private fun onReset() {
        binding.etConsumerNumber.setText("")
        binding.etAmount.setText("")
        binding.tvPlatformFee.text = getString(R.string.hintPlatformFee)
        binding.tvTotalPayable.text = getString(R.string.hintTotalPayable)
        binding.tvCompany.text = getString(R.string.hintSelectCompany)
        binding.tvCompany.setTextColor(mActivity.getThemeColor(R.attr.colorTextHint))
        binding.cbTerms.isChecked = false
        selectedOperator = null
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

                binding.flCompanyAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showCompanyDropdown()
                }
                binding.llProceed -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onProceedToPay()
                }
                binding.llReset -> {
                    if (Utility.stopClick()) return@OnClickListener
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
