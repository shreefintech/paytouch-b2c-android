package com.shreefintech.paytouchconsumer.utill

import android.app.Activity
import android.app.Dialog
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.DialogMpinBinding

// 4-box MPIN entry in dialog_mpin.xml — shared by Bill Pay, Edit Profile, Edit Bank Details, Load Wallet and Earning Wallet

private val DialogMpinBinding.mpinBoxes
    get() = listOf(etMpin1, etMpin2, etMpin3, etMpin4)

fun DialogMpinBinding.setupMpinBoxes() {
    mpinBoxes.setupPinBoxes()
    etMpin1.requestFocus()
}

// Non-cancelable MPIN popup window; the caller sets its dismiss listener and shows it
fun DialogMpinBinding.createDialog(activity: Activity): Dialog = Dialog(activity).apply {
    setContentView(root)
    window?.setBackgroundDrawableResource(android.R.color.transparent)
    window?.setWindowAnimations(R.style.DialogScaleFadeAnimation)
    window?.setLayout(
        (activity.resources.displayMetrics.widthPixels * 0.88).toInt(),
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    // requestFocus() alone doesn't raise the keyboard inside a Dialog
    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    setCancelable(false)
}

fun DialogMpinBinding.getMpin(): String =
    mpinBoxes.joinToString("") { it.text?.toString().orEmpty() }

fun DialogMpinBinding.clearMpin() {
    mpinBoxes.forEach { it.text?.clear() }
    etMpin1.requestFocus()
}

// Same behaviour as LoginActivity / ResetMpinActivity: typing moves forward, backspace on an empty box moves back
fun List<EditText>.setupPinBoxes() {
    forEachIndexed { index, editText ->
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 1 && index < lastIndex) this@setupPinBoxes[index + 1].requestFocus()
            }
        })
        editText.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL
                && event.action == KeyEvent.ACTION_DOWN
                && editText.text.isNullOrEmpty()
                && index > 0
            ) {
                this[index - 1].let { prev ->
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
