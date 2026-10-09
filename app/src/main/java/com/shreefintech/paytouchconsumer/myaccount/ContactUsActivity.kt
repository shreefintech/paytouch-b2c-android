package com.shreefintech.paytouchconsumer.myaccount

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.databinding.ActivityContactUsBinding
import com.shreefintech.paytouchconsumer.utill.Utility

class ContactUsActivity : BaseActivity() {

    private lateinit var binding: ActivityContactUsBinding

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ContactUsActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactUsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.onClickListener = onClickListener()
        onBack()
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
                binding.llPhone1 -> {
                    if (Utility.stopClick()) return@OnClickListener
                    dialPhone(Constant.SUPPORT_PHONE_1)
                }
                binding.llPhone2 -> {
                    if (Utility.stopClick()) return@OnClickListener
                    dialPhone(Constant.SUPPORT_PHONE_2)
                }
                binding.llPhone3 -> {
                    if (Utility.stopClick()) return@OnClickListener
                    dialPhone(Constant.SUPPORT_PHONE_3)
                }
                binding.llEmail -> {
                    if (Utility.stopClick()) return@OnClickListener
                    openEmail(Constant.SUPPORT_EMAIL)
                }
            }
        }
    }

    private fun dialPhone(number: String) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    private fun openEmail(address: String) {
        startActivity(Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$address")
        })
    }
}
