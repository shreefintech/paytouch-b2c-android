package com.shreefintech.paytouchconsumer.kyc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.HomeActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.auth.NextStepRouter
import com.shreefintech.paytouchconsumer.databinding.ActivityKycStatusBinding
import com.shreefintech.paytouchconsumer.enums.NextStep
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.gone

class KycStatusActivity : BaseActivity() {

    companion object {
        private const val EXTRA_NEXT_STEP = "extra_next_step"

        /** [nextStep] is `pending_approval` or `registration_rejected`; null shows pending until a refresh. */
        fun buildIntent(context: Context, nextStep: String?): Intent =
            Intent(context, KycStatusActivity::class.java).putExtra(EXTRA_NEXT_STEP, nextStep)
    }

    private lateinit var binding: ActivityKycStatusBinding
    private val viewModel: KycStatusViewModel by viewModels()

    private val nextStep: String? by lazy { intent.getStringExtra(EXTRA_NEXT_STEP) }

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

        binding.lytToolbar.ivBack.gone()
        binding.onClickListener = onClickListener()

        renderStatus(nextStep, null)
    }

    private fun refreshStatus() {
        viewModel.fetchStatus(
            onLoading = {},
            onReady = { item ->
                if (isFinishing || isDestroyed) return@fetchStatus
                binding.swipeRefresh.isRefreshing = false
                renderStatus(item.nextStep, item.mpinRequired)
            },
            onError = { msg ->
                binding.swipeRefresh.isRefreshing = false
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun renderStatus(nextStep: String?, mpinRequired: Boolean?) {
        // Missing step = nothing known yet; an unrecognised one goes to the router (Home), never a stuck pending screen
        if (nextStep == null) {
            showPending()
            return
        }
        when (NextStep.from(nextStep)) {
            NextStep.PENDING_APPROVAL      -> showPending()
            NextStep.REGISTRATION_REJECTED -> showRejected()
            // Any other step has moved on (complete, MPIN, bank, Aadhaar) — the router decides where
            else -> navigateTo(NextStepRouter.intentFor(mActivity, nextStep, mpinRequired))
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
            .placeholder(R.drawable.ic_file_not_found)
            .error(R.drawable.ic_file_not_found)
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

    private fun navigateTo(target: Intent) {
        startActivity(target.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    // Home's auth/me check opens the re-upload dialog when kyc_rejection names both Aadhaar items
    private fun onRetry() {
        navigateTo(Intent(mActivity, HomeActivity::class.java))
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener {
            when (it) {
                binding.cvRetry -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onRetry()
                }
            }
        }
    }
}
