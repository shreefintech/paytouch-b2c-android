package com.shreefintech.paytouchconsumer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.shreefintech.paytouchconsumer.databinding.ActivityHomeBinding
import com.shreefintech.paytouchconsumer.electricity.ElectricityActivity
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        LiquidGlassEffect.attach(
            targetView = binding.flCard,
            rootView = binding.root as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        binding.onClickListener = onClickListener()
        onBack()
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
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

                binding.llElectricity -> {
                    if (Utility.stopClick()) return@OnClickListener
                    startActivity(Intent(mActivity, ElectricityActivity::class.java))
                }

                binding.llGas -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-520): Navigate to Gas bill payment screen
                    ToastUtil.showDelete(mActivity, getString(R.string.msgComingSoon))
                }

                binding.llPrepaid -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-520): Navigate to Prepaid recharge screen
                    ToastUtil.showDelete(mActivity, getString(R.string.msgComingSoon))
                }

                binding.llTvCable -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-520): Navigate to TV Cable payment screen
                    ToastUtil.showDelete(mActivity, getString(R.string.msgComingSoon))
                }

                binding.llDth -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-520): Navigate to DTH recharge screen
                    ToastUtil.showDelete(mActivity, getString(R.string.msgComingSoon))
                }

                binding.llFastag -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-520): Navigate to Fastag recharge screen
                    ToastUtil.showDelete(mActivity, getString(R.string.msgComingSoon))
                }

                binding.llLoan -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-520): Navigate to Loan payment screen
                    ToastUtil.showDelete(mActivity, getString(R.string.msgComingSoon))
                }

                binding.llMyAccount -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-520): Navigate to My Account screen
                    ToastUtil.showDelete(mActivity, getString(R.string.msgComingSoon))
                }

                binding.llTax -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-520): Navigate to Tax payment screen
                    ToastUtil.showDelete(mActivity, getString(R.string.msgComingSoon))
                }

                binding.cvLoadWallet -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-520): Navigate to Load Wallet screen
                    ToastUtil.showDelete(mActivity, getString(R.string.msgComingSoon))
                }
            }
        }
    }
}
