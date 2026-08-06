package com.shreefintech.paytouchconsumer.myaccount

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityMyAccountBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.myaccount.viewmodel.MyAccountViewModel
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.AccountInfoDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.ReferralDataItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class MyAccountActivity : BaseActivity() {

    private lateinit var binding: ActivityMyAccountBinding
    private val viewModel: MyAccountViewModel by viewModels()

    private var isAccountInfoLoading = true
    private var isReferEarnLoading = false
    private var currentTab = TAB_ACCOUNT_INFO
    private val showProgressRefresh = ObservableBoolean(false)

    companion object {
        private const val TAB_ACCOUNT_INFO = 0
        private const val TAB_REFER_EARN = 1

        fun start(context: Context) {
            context.startActivity(Intent(context, MyAccountActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAccountBinding.inflate(layoutInflater)
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
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
            strokeColor = ContextCompat.getColor(mActivity, R.color.glass_stroke_primary),
            strokeWidth = 1,
            solidStroke = true,
        )

        binding.onClickListener = onClickListener()
        binding.showProgressRefresh = showProgressRefresh
        selectTab(TAB_ACCOUNT_INFO)
        onBack()

        loadAccountInfo()
        loadReferralInfo()
    }

    // ── API Calls ─────────────────────────────────────────────

    private fun loadAccountInfo() {
        viewModel.getAccountInfo(
            onLoading = {
                isAccountInfoLoading = true
                showProgressRefresh.set(true)
                binding.shimmerAccountInfo.visibility = View.VISIBLE
                binding.shimmerAccountInfo.startShimmer()
                binding.llAccountInfoContent.visibility = View.GONE
            },
            onSuccess = { data ->
                isAccountInfoLoading = false
                showProgressRefresh.set(false)
                binding.shimmerAccountInfo.stopShimmer()
                binding.shimmerAccountInfo.visibility = View.GONE
                binding.llAccountInfoContent.visibility = View.VISIBLE
                populateAccountInfo(data)
            },
            onError = { msg ->
                isAccountInfoLoading = false
                showProgressRefresh.set(false)
                binding.shimmerAccountInfo.stopShimmer()
                binding.shimmerAccountInfo.visibility = View.GONE
                binding.llAccountInfoContent.visibility = View.VISIBLE
                if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun loadReferralInfo() {
        viewModel.getReferralInfo(
            onLoading = {
                isReferEarnLoading = true
                // shimmer only shown if Refer & Earn tab is active when loading starts
                if (binding.shimmerReferEarn.visibility == View.VISIBLE ||
                    binding.llReferEarnContent.visibility == View.VISIBLE
                ) {
                    binding.shimmerReferEarn.visibility = View.VISIBLE
                    binding.shimmerReferEarn.startShimmer()
                    binding.llReferEarnContent.visibility = View.GONE
                }
            },
            onSuccess = { data ->
                isReferEarnLoading = false
                binding.shimmerReferEarn.stopShimmer()
                binding.shimmerReferEarn.visibility = View.GONE
                binding.llReferEarnContent.visibility =
                    if (currentTab == TAB_REFER_EARN) View.VISIBLE else View.GONE
                populateReferralInfo(data)
            },
            onError = { msg ->
                isReferEarnLoading = false
                binding.shimmerReferEarn.stopShimmer()
                binding.shimmerReferEarn.visibility = View.GONE
                if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    // ── Populate ──────────────────────────────────────────────

    private fun populateAccountInfo(data: AccountInfoDataItem) {
        binding.tvMemberId.text = data.memberId ?: "--"
        binding.tvMemberNo.text = data.memberNo ?: "--"
        binding.tvMemberCode.text = data.memberCode ?: "--"
        binding.tvMemberName.text = data.memberName ?: "--"
        binding.tvMobileNo.text = data.mobileNo ?: "--"
        binding.tvEmail.text = data.email ?: "--"
        binding.tvStatus.text = data.status ?: "--"
        binding.tvCity.text = data.cityName ?: "--"
        binding.tvHomeAddress.text = data.homeAddress ?: "--"
        binding.tvRegistrationDate.text = data.registrationDate ?: "--"
        binding.tvActivationDate.text = data.activationDate ?: "--"

        val balanceRaw = data.balance ?: "--"
        val (amount, words) = parseBalance(balanceRaw)
        binding.tvBalance.text = amount
        if (words.isNotEmpty()) {
            binding.tvBalanceWords.text = "($words)"
            binding.tvBalanceWords.visibility = View.VISIBLE
        }
    }

    private fun populateReferralInfo(data: ReferralDataItem) {
        val code = data.referralCode ?: "--"
        val link = data.referralLink ?: "--"

        binding.tvReferralCode.text = code
        binding.tvReferralLink.text = link

        // Store referral code in SharedPreferences for sharing
        if (code != "--") {
            SharedPreferenceHelper.setSharedPreferenceString(
                mActivity, com.shreefintech.paytouchconsumer.Constant.KEY_REFERRAL_CODE, code
            )
        }

        // TODO(PAYTOUCH-523): Expose tvEarningsRow1/2/3 and bind to total_earnings / earning_potential
    }

    // ── Helpers ───────────────────────────────────────────────

    // balance API field format: "100.00 [ Rupees One Hundred Only ]"
    private fun parseBalance(raw: String): Pair<String, String> {
        val bracketIdx = raw.indexOf('[')
        return if (bracketIdx > 0) {
            val amount = raw.substring(0, bracketIdx).trim()
            val words = raw.substringAfter('[').substringBefore(']').trim()
            Pair(amount, words)
        } else {
            Pair(raw, "")
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        ToastUtil.showSuccess(mActivity, getString(R.string.msgCopiedToClipboard))
    }

    // ── Tab Switching ─────────────────────────────────────────

    private fun selectTab(tab: Int) {
        currentTab = tab
        val isAccountInfo = tab == TAB_ACCOUNT_INFO

        // Account Info section
        if (isAccountInfo) {
            if (isAccountInfoLoading) {
                binding.shimmerAccountInfo.visibility = View.VISIBLE
                binding.shimmerAccountInfo.startShimmer()
                binding.llAccountInfoContent.visibility = View.GONE
            } else {
                binding.shimmerAccountInfo.visibility = View.GONE
                binding.llAccountInfoContent.visibility = View.VISIBLE
            }
            binding.shimmerReferEarn.stopShimmer()
            binding.shimmerReferEarn.visibility = View.GONE
            binding.llReferEarnContent.visibility = View.GONE
        } else {
            if (isReferEarnLoading) {
                binding.shimmerReferEarn.visibility = View.VISIBLE
                binding.shimmerReferEarn.startShimmer()
                binding.llReferEarnContent.visibility = View.GONE
            } else {
                binding.shimmerReferEarn.visibility = View.GONE
                binding.llReferEarnContent.visibility = View.VISIBLE
            }
            binding.shimmerAccountInfo.stopShimmer()
            binding.shimmerAccountInfo.visibility = View.GONE
            binding.llAccountInfoContent.visibility = View.GONE
        }

        binding.cvRefresh.visibility = if (isAccountInfo) View.VISIBLE else View.GONE
        binding.tvTitle.setText(if (isAccountInfo) R.string.titleMyAccount else R.string.titleReferAndEarn)

        val activeColor = ContextCompat.getColor(mActivity, R.color.primary)
        val inactiveColor = android.graphics.Color.TRANSPARENT
        val activeTextColor = ContextCompat.getColor(mActivity, R.color.white)
        val inactiveTextColor = ContextCompat.getColor(mActivity, R.color.primary)

        binding.cvTabAccountInfo.setCardBackgroundColor(if (isAccountInfo) activeColor else inactiveColor)
        binding.cvTabReferEarn.setCardBackgroundColor(if (isAccountInfo) inactiveColor else activeColor)
        binding.tvTabAccountInfo.setTextColor(if (isAccountInfo) activeTextColor else inactiveTextColor)
        binding.tvTabReferEarn.setTextColor(if (isAccountInfo) inactiveTextColor else activeTextColor)
    }

    // ── Back ──────────────────────────────────────────────────

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    // ── Clicks ────────────────────────────────────────────────

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.lytToolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBackPressedDispatcher.onBackPressed()
                }
                binding.cvTabAccountInfo -> {
                    if (Utility.stopClick()) return@OnClickListener
                    selectTab(TAB_ACCOUNT_INFO)
                }
                binding.cvTabReferEarn -> {
                    if (Utility.stopClick()) return@OnClickListener
                    selectTab(TAB_REFER_EARN)
                }
                binding.cvRefresh -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressRefresh.get()) return@OnClickListener
                    loadAccountInfo()
                }
                binding.ivCopyReferralCode -> {
                    if (Utility.stopClick()) return@OnClickListener
                    val code = binding.tvReferralCode.text.toString()
                    if (code != "--") copyToClipboard("Referral Code", code)
                }
                binding.cvCopyReferralLink -> {
                    if (Utility.stopClick()) return@OnClickListener
                    val link = binding.tvReferralLink.text.toString()
                    if (link != "--") copyToClipboard("Referral Link", link)
                }
                binding.cvShareWhatsapp -> {
                    if (Utility.stopClick()) return@OnClickListener
                    shareViaWhatsApp()
                }
                binding.cvShareFacebook -> {
                    if (Utility.stopClick()) return@OnClickListener
                    shareViaFacebook()
                }
                binding.cvShareEmail -> {
                    if (Utility.stopClick()) return@OnClickListener
                    shareViaEmail()
                }
            }
        }
    }

    // ── Share Helpers ─────────────────────────────────────────

    private fun shareViaWhatsApp() {
        val link = binding.tvReferralLink.text.toString().takeIf { it != "--" } ?: return
        val message = getString(R.string.msgShareReferralInfo) + "\n\n$link"
        try {
            startActivity(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, message)
            })
        } catch (e: Exception) {
            shareGeneric(message)
        }
    }

    private fun shareViaFacebook() {
        val link = binding.tvReferralLink.text.toString().takeIf { it != "--" } ?: return
        try {
            startActivity(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.facebook.katana")
                putExtra(Intent.EXTRA_TEXT, link)
            })
        } catch (e: Exception) {
            shareGeneric(link)
        }
    }

    private fun shareViaEmail() {
        val link = binding.tvReferralLink.text.toString().takeIf { it != "--" } ?: return
        val message = getString(R.string.msgShareReferralInfo) + "\n\n$link"
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.titleReferAndEarn))
            putExtra(Intent.EXTRA_TEXT, message)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            shareGeneric(message)
        }
    }

    private fun shareGeneric(text: String) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                getString(R.string.titleReferAndEarn)
            )
        )
    }
}
