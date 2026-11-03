package com.shreefintech.paytouchconsumer.myaccount

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.KycDocumentAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityKycDetailsBinding
import com.shreefintech.paytouchconsumer.databinding.ItemKycBankAccountBinding
import com.shreefintech.paytouchconsumer.enums.NextStep
import com.shreefintech.paytouchconsumer.kyc.bank.model.EditBankPassItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MeDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycDocumentDetailItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletBankAccountItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import kotlin.math.abs

class KycDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityKycDetailsBinding
    private val viewModel: KycDetailsViewModel by viewModels()

    private val documents = mutableListOf<KycDocumentDetailItem>()
    private lateinit var documentAdp: KycDocumentAdp

    private val editBankLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val msg = result.data?.getStringExtra(EditBankDetailsActivity.EXTRA_SUCCESS_MSG)
            if (!msg.isNullOrEmpty()) ToastUtil.showSuccess(mActivity, msg)
            loadKycDetails()
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, KycDetailsActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKycDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val listener = onClickListener()
        binding.onClickListener = listener
        setupDocumentSlider()
        onBack()
        retryCallback = { loadKycDetails() }
        loadKycDetails()
    }

    private fun setupDocumentSlider() {
        documentAdp = KycDocumentAdp(urlResolver = ::resolveFileUrl)
        documentAdp.onItemClick = { url, label -> DocPreviewActivity.start(this, url, label) }
        binding.vpDocuments.adapter = documentAdp
        binding.vpDocuments.setPageTransformer { page, position ->
            page.alpha = 1f - abs(position) * 0.4f
        }
        binding.vpDocuments.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                binding.tvDocumentTitle.text = documents.getOrNull(position)?.label ?: ""
            }
        })
    }

    private fun loadKycDetails() {
        if (!Utility.isInternetAvailable(mActivity)) {
            showNoInternet()
            return
        }
        hideNoInternet()
        viewModel.fetchDetails(
            onLoading = {
                binding.pbLoading.isVisible = true
                binding.nsvContent.isVisible = false
            },
            onReady = { me, bank, isBankFailed ->
                if (isFinishing || isDestroyed) return@fetchDetails
                binding.pbLoading.isVisible = false
                binding.nsvContent.isVisible = true
                populateData(me, bank)
                // The empty card alone would read as "no bank on file"
                if (isBankFailed) ToastUtil.showWarning(mActivity, getString(R.string.msgBankDetailsLoadFailed))
            },
            onError = { msg ->
                binding.pbLoading.isVisible = false
                binding.nsvContent.isVisible = true
                if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun populateData(me: MeDataItem, bank: WalletBankAccountItem?) {
        val isComplete = NextStep.from(me.nextStep) == NextStep.COMPLETE
        populateIdentity(me, badge(
            isRejected = me.isAadhaarRejected,
            isComplete = isComplete,
            isOnFile   = me.aadhaarAdded == true
        ))
        populateBank(bank, badge(
            isRejected = MeDataItem.KYC_ITEM_BANK_DETAILS in me.kycRejectedItems,
            isComplete = isComplete,
            isOnFile   = me.bankDetailsAdded == true
        ))
        // TODO(B2C-287): no documents source in B2C yet — the section stays hidden until one exists
        populateDocuments(emptyList())
    }

    private fun populateIdentity(me: MeDataItem, badge: KycBadge) {
        binding.tvMobile.text = me.user?.mobile ?: "--"
        binding.tvAadhaar.text = me.user?.aadhaarMasked ?: "--"
        binding.tvEmail.text = me.user?.email ?: "--"
        applyBadge(binding.tvIdentityStatus, badge)
        // B2C returns no avatar or PAN number — hidden rather than shown empty
        binding.cvAvatar.isVisible = false
        binding.viewPanDivider.isVisible = false
        binding.llPanColumn.isVisible = false
    }

    // B2C has one withdrawal bank (bank_id 0); its card and Edit are always shown so a bank can be added
    private fun populateBank(bank: WalletBankAccountItem?, badge: KycBadge) {
        applyBadge(binding.tvBankStatus, badge)

        binding.llBankAccounts.removeAllViews()
        val hasBank = bank?.hasBankAccount == true
        val item = ItemKycBankAccountBinding.inflate(layoutInflater, binding.llBankAccounts, false)
        item.tvAccountNumber.text = bank?.accountMasked?.takeIf { hasBank } ?: "--"
        item.tvBankName.text = bank?.bankName?.takeIf { hasBank } ?: "--"
        item.tvIfsc.text = bank?.ifsc?.takeIf { hasBank } ?: "--"
        // Branch is not returned by wallet/bank-account
        item.llBranchName.isVisible = false
        item.ivEditBank.visibility = View.VISIBLE
        item.ivEditBank.setOnClickListener {
            if (Utility.stopClick()) return@setOnClickListener
            onEditBank(bank?.takeIf { hasBank })
        }
        binding.llBankAccounts.addView(item.root)
    }

    private fun onEditBank(bank: WalletBankAccountItem?) {
        val passItem = EditBankPassItem(
            bankId            = EditBankPassItem.WITHDRAWAL_BANK_ID,
            isPrimary         = true,
            accountHolderName = bank?.holderName,
            accountNumber     = bank?.accountMasked,
            bankName          = bank?.bankName,
            ifsc              = bank?.ifsc,
            branchName        = null,
            bankProofUrl      = null
        )
        editBankLauncher.launch(EditBankDetailsActivity.createIntent(this, passItem))
    }

    private fun populateDocuments(docList: List<KycDocumentDetailItem>) {
        documents.clear()
        documents.addAll(docList)
        documentAdp.updateList(docList)

        val hasDocs = docList.isNotEmpty()
        binding.llDocumentsHeader.isVisible = hasDocs
        binding.view2.isVisible = hasDocs
        binding.llDocumentsContent.isVisible = hasDocs
        binding.tvNoDocuments.isVisible = false
        if (hasDocs) {
            setupDots(docList.size)
            binding.tvDocumentTitle.text = docList[0].label ?: ""
            updateDots(0)
        }
    }

    // iOS parity: rejected when kyc_rejection.items names it, verified once complete, submitted while on file
    private fun badge(isRejected: Boolean, isComplete: Boolean, isOnFile: Boolean): KycBadge = when {
        isRejected -> KycBadge.REJECTED
        isComplete -> KycBadge.VERIFIED
        isOnFile   -> KycBadge.SUBMITTED
        else       -> KycBadge.PENDING
    }

    private fun applyBadge(view: TextView, badge: KycBadge) {
        view.text = getString(badge.labelRes)
        view.setTextColor(ContextCompat.getColor(this, badge.colorRes))
    }

    private enum class KycBadge(@param:StringRes val labelRes: Int, @param:ColorRes val colorRes: Int) {
        VERIFIED(R.string.labelVerified, R.color.colorStatusSuccess),
        SUBMITTED(R.string.labelSubmitted, R.color.colorStatusPending),
        PENDING(R.string.labelPending, R.color.colorStatusPending),
        REJECTED(R.string.labelRejected, R.color.colorStatusFailed)
    }

    private fun setupDots(count: Int) {
        binding.llDots.removeAllViews()
        val dotSize = resources.getDimensionPixelSize(R.dimen.kyc_dot_size)
        val dotMargin = resources.getDimensionPixelSize(R.dimen.kyc_dot_margin)
        repeat(count) {
            val dot = ImageView(this)
            val params = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                marginStart = dotMargin
                marginEnd = dotMargin
            }
            dot.layoutParams = params
            dot.setImageResource(R.drawable.ic_dot_empty)
            binding.llDots.addView(dot)
        }
    }

    private fun updateDots(selected: Int) {
        for (i in 0 until binding.llDots.childCount) {
            val dot = binding.llDots.getChildAt(i) as? ImageView ?: continue
            dot.setImageResource(if (i == selected) R.drawable.ic_dot_filled else R.drawable.ic_dot_empty)
        }
    }

    private fun resolveFileUrl(fileUrl: String?): String? = fileUrl?.ifBlank { null }

    private fun onClickListener() = View.OnClickListener { view ->
        when (view) {
            binding.lytToolbar.ivBack -> {
                if (Utility.stopClick()) return@OnClickListener
                onBackPressedDispatcher.onBackPressed()
            }
            binding.cvPreview -> {
                if (Utility.stopClick()) return@OnClickListener
                val doc = documents.getOrNull(binding.vpDocuments.currentItem) ?: return@OnClickListener
                val url = resolveFileUrl(doc.fileUrl)
                if (url.isNullOrBlank()) {
                    ToastUtil.showDelete(mActivity, getString(R.string.msgDocumentNotAvailable))
                    return@OnClickListener
                }
                DocPreviewActivity.start(this, url, doc.label ?: "")
            }
        }
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }
}
