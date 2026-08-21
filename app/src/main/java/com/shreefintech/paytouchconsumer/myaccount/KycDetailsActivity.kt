package com.shreefintech.paytouchconsumer.myaccount

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.KycDocumentAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityKycDetailsBinding
import com.shreefintech.paytouchconsumer.onboarding.kyc.KycStatusViewModel
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycDocumentDetailItem
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycMyAccountItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class KycDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityKycDetailsBinding
    private val viewModel: KycStatusViewModel by viewModels()

    private val documents = mutableListOf<KycDocumentDetailItem>()
    private lateinit var documentAdp: KycDocumentAdp

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

        binding.onClickListener = onClickListener()
        setupDocumentSlider()
        onBack()
        retryCallback = { loadKycDetails() }
        loadKycDetails()
    }

    private fun setupDocumentSlider() {
        documentAdp = KycDocumentAdp(
            urlResolver = ::resolveFileUrl,
            onItemClick = { url, label -> DocPreviewActivity.start(this, url, label) }
        )
        binding.vpDocuments.adapter = documentAdp
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
        viewModel.fetchMyAccount(
            onLoading = {
                binding.pbLoading.isVisible = true
                binding.nsvContent.isVisible = false
            },
            onReady = { data ->
                binding.pbLoading.isVisible = false
                binding.nsvContent.isVisible = true
                populateData(data)
            },
            onError = { msg ->
                binding.pbLoading.isVisible = false
                if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun populateData(data: KycMyAccountItem) {
        populateIdentity(data)
        populateBank(data)
        populateDocuments(data)
    }

    private fun populateIdentity(data: KycMyAccountItem) {
        val identity = data.identity ?: return
        binding.tvMobile.text = identity.mobile ?: "--"
        binding.tvAadhaar.text = identity.aadhaarNumber ?: "--"
        binding.tvPan.text = identity.panNumber ?: "--"
        binding.tvEmail.text = identity.email ?: "--"
        val identityColor = statusColor(identity.status)
        binding.tvIdentityStatus.text = statusLabel(identity.status)
        binding.tvIdentityStatus.setTextColor(identityColor)

        val avatarUrl = resolveFileUrl(identity.avatarUrl)
        if (!avatarUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.ivAvatar)
        }
    }

    private fun populateBank(data: KycMyAccountItem) {
        val bank = data.bank ?: return
        binding.tvBankStatus.text = statusLabel(bank.status)
        binding.tvBankStatus.setTextColor(statusColor(bank.status))
        val account = bank.accounts?.firstOrNull() ?: return
        binding.tvAccountNumber.text = account.accountNumber ?: "--"
        binding.tvBankName.text = account.bankName ?: "--"
        binding.tvIfsc.text = account.ifsc ?: "--"
        binding.tvBranchName.text = account.branchName ?: "--"
    }

    private fun populateDocuments(data: KycMyAccountItem) {
        val docs = data.documents ?: return
        binding.tvDocumentsStatus.text = statusLabel(docs.status)
        binding.tvDocumentsStatus.setTextColor(statusColor(docs.status))

        val docList = docs.items ?: emptyList()
        documents.clear()
        documents.addAll(docList)
        documentAdp.updateList(docList)

        if (docList.isEmpty()) {
            binding.llDocumentsContent.isVisible = false
            binding.tvNoDocuments.isVisible = true
        } else {
            binding.llDocumentsContent.isVisible = true
            binding.tvNoDocuments.isVisible = false
            setupDots(docList.size)
            binding.tvDocumentTitle.text = docList[0].label ?: ""
            updateDots(0)
        }
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

    private fun statusLabel(status: String?): String = when {
        status.isNullOrEmpty() -> "--"
        status.contains("approved", ignoreCase = true) ||
                status.contains("verified", ignoreCase = true) -> getString(R.string.labelVerified)
        status.contains("reject", ignoreCase = true) -> getString(R.string.labelRejected)
        else -> getString(R.string.labelPending)
    }

    private fun statusColor(status: String?): Int = when {
        status?.contains("approved", ignoreCase = true) == true ||
                status?.contains("verified", ignoreCase = true) == true ->
            ContextCompat.getColor(this, R.color.colorStatusSuccess)
        status?.contains("reject", ignoreCase = true) == true ->
            ContextCompat.getColor(this, R.color.colorStatusFailed)
        else -> ContextCompat.getColor(this, R.color.colorStatusPending)
    }

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
