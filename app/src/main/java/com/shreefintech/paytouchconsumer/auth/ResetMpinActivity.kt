package com.shreefintech.paytouchconsumer.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityResetMpinBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class ResetMpinActivity : BaseActivity() {

    private lateinit var binding: ActivityResetMpinBinding
    private val viewModel: ResetMpinViewModel by viewModels()
    private var showProgress = ObservableBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetMpinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets  = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(imeInsets.bottom, systemBars.bottom)
            )
            insets
        }

        LiquidGlassEffect.attach(
            targetView   = binding.flCard,
            rootView     = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion   = 0f,
            blur         = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        binding.onClickListener = onClickListener()
        binding.showProgress    = showProgress

        onBack()
        setupMpinBoxes()
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { navigateToLogin() }
        })
    }

    private fun setupMpinBoxes() {
        val newMpinBoxes = listOf(
            binding.etNewMpin1, binding.etNewMpin2,
            binding.etNewMpin3, binding.etNewMpin4
        )
        val confirmMpinBoxes = listOf(
            binding.etConfirmMpin1, binding.etConfirmMpin2,
            binding.etConfirmMpin3, binding.etConfirmMpin4
        )
        wireBoxes(newMpinBoxes)
        wireBoxes(confirmMpinBoxes)
    }

    private fun wireBoxes(boxes: List<AppCompatEditText>) {
        boxes.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < boxes.lastIndex) {
                        boxes[index + 1].requestFocus()
                    }
                }
            })
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL
                    && event.action == KeyEvent.ACTION_DOWN
                    && editText.text.isNullOrEmpty()
                    && index > 0
                ) {
                    boxes[index - 1].let { prev ->
                        prev.requestFocus()
                        prev.text?.clear()
                    }
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun collectMpin(boxes: List<AppCompatEditText>): String =
        boxes.joinToString("") { it.text.toString() }

    private fun validate(): Boolean {
        Utility.hideKeyboard(binding.clRoot)
        val newMpin     = collectMpin(
            listOf(binding.etNewMpin1, binding.etNewMpin2, binding.etNewMpin3, binding.etNewMpin4)
        )
        val confirmMpin = collectMpin(
            listOf(binding.etConfirmMpin1, binding.etConfirmMpin2, binding.etConfirmMpin3, binding.etConfirmMpin4)
        )

        val msg = when {
            newMpin.isEmpty() -> {
                binding.etNewMpin1.requestFocus()
                getString(R.string.msgNewMpinEmpty)
            }
            newMpin.length != 4 -> {
                binding.etNewMpin1.requestFocus()
                getString(R.string.msgNewMpinIncomplete)
            }
            newMpin.toSet().size == 1 -> {
                binding.etNewMpin1.requestFocus()
                getString(R.string.msgNewMpinRepeated)
            }
            confirmMpin.isEmpty() -> {
                binding.etConfirmMpin1.requestFocus()
                getString(R.string.msgConfirmMpinEmpty)
            }
            confirmMpin.length != 4 -> {
                binding.etConfirmMpin1.requestFocus()
                getString(R.string.msgConfirmMpinIncomplete)
            }
            confirmMpin != newMpin -> {
                binding.etConfirmMpin1.requestFocus()
                getString(R.string.msgMpinMismatch)
            }
            else -> null
        }
        if (msg != null) { ToastUtil.showDelete(mActivity, msg); return false }
        return true
    }

    private fun onChangeMpin() {
        if (!validate()) return
        val newMpin = collectMpin(
            listOf(binding.etNewMpin1, binding.etNewMpin2, binding.etNewMpin3, binding.etNewMpin4)
        )
        viewModel.changeMpin(
            context   = mActivity,
            newMpin   = newMpin,
            onLoading = { showProgress.set(true) },
            onSuccess = { showProgress.set(false); navigateToLogin() },
            onError   = { msg -> showProgress.set(false); ToastUtil.showDelete(mActivity, msg) }
        )
    }

    private fun navigateToLogin() {
        val intent = Intent(mActivity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.llChangeMpin -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onChangeMpin()
                }
                binding.tvBackToSignIn -> {
                    if (Utility.stopClick()) return@OnClickListener
                    navigateToLogin()
                }
            }
        }
    }
}
