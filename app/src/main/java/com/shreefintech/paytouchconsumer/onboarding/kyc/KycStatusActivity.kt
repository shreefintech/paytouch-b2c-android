package com.shreefintech.paytouchconsumer.onboarding.kyc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.enums.KycSubmissionStatus
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.auth.LoginActivity
import com.shreefintech.paytouchconsumer.databinding.ActivityKycStatusBinding
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycStatusItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class KycStatusActivity : BaseActivity() {

    companion object {
        private const val EXTRA_STATUS = "extra_status"

        fun start(context: Context, status: KycStatusItem) {
            context.startActivity(
                Intent(context, KycStatusActivity::class.java).apply {
                    putExtra(EXTRA_STATUS, Gson().toJson(status))
                }
            )
        }
    }

    private lateinit var binding: ActivityKycStatusBinding
    private val viewModel: KycStatusViewModel by viewModels()

    private val statusItem: KycStatusItem? by lazy {
        intent.getStringExtra(EXTRA_STATUS)?.let { Gson().fromJson(it, KycStatusItem::class.java) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKycStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.swipeRefresh.setColorSchemeColors(ContextCompat.getColor(mActivity, R.color.primary))
        binding.swipeRefresh.setOnRefreshListener { refreshStatus() }

        binding.lytToolbar.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.cvRetry.setOnClickListener {
            if (Utility.stopClick()) return@setOnClickListener
            onRetry()
        }

        onBack()
        statusItem?.let { renderStatus(it) }
    }

    private fun refreshStatus() {
        viewModel.fetchStatus(
            onLoading = {},
            onReady = { item ->
                binding.swipeRefresh.isRefreshing = false
                renderStatus(item)
            },
            onError = { msg ->
                binding.swipeRefresh.isRefreshing = false
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun renderStatus(item: KycStatusItem) {
        when (KycSubmissionStatus.from(item.submission?.status)) {
            KycSubmissionStatus.KYC_APPROVED  -> navigateToLogin()
            KycSubmissionStatus.KYC_REJECTED  -> showRejected()
            KycSubmissionStatus.KYC_SUBMITTED -> showPending()
            KycSubmissionStatus.PENDING_KYC   -> {
                KycActivity.start(mActivity)
                finish()
            }
            null -> showPending()
        }
    }

    private fun showPending() {
        loadGif(R.drawable.gif_kyc_pending)
        binding.tvTitle1.text = getString(R.string.textVerificationPending)
        binding.tvDes.text = getString(R.string.msgVerificationPending)
        binding.cvRetry.visibility = View.GONE
    }

    private fun showRejected() {
        loadGif(R.drawable.gif_kyc_rejected)
        binding.tvTitle1.text = getString(R.string.textVerificationRejected)
        binding.tvDes.text = getString(R.string.msgVerificationRejected)
        binding.cvRetry.visibility = View.VISIBLE
    }

    private fun loadGif(@DrawableRes res: Int) {
        Glide.with(this)
            .asGif()
            .load(res)
            .listener(object : RequestListener<GifDrawable> {
                override fun onResourceReady(
                    resource: GifDrawable, model: Any, target: Target<GifDrawable>?,
                    dataSource: DataSource, isFirstResource: Boolean
                ): Boolean {
                    resource.setLoopCount(1)
                    return false
                }
                override fun onLoadFailed(
                    e: GlideException?, model: Any?, target: Target<GifDrawable>,
                    isFirstResource: Boolean
                ) = false
            })
            .into(binding.iv1)
    }

    private fun navigateToLogin() {
        startActivity(Intent(mActivity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun onRetry() {
        KycActivity.start(mActivity)
        finish()
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
