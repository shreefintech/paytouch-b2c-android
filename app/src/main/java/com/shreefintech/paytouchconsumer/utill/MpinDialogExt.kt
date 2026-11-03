package com.shreefintech.paytouchconsumer.utill

import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import com.shreefintech.paytouchconsumer.databinding.DialogMpinBinding

// 4-box MPIN entry in dialog_mpin.xml — shared by Edit Profile and Edit Bank Details

private val DialogMpinBinding.mpinBoxes
    get() = listOf(etMpin1, etMpin2, etMpin3, etMpin4)

// Same behaviour as LoginActivity: typing moves forward, backspace on an empty box moves back
fun DialogMpinBinding.setupMpinBoxes() {
    val boxes = mpinBoxes
    boxes.forEachIndexed { index, editText ->
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 1 && index < boxes.lastIndex) boxes[index + 1].requestFocus()
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
    etMpin1.requestFocus()
}

fun DialogMpinBinding.getMpin(): String =
    mpinBoxes.joinToString("") { it.text?.toString().orEmpty() }

fun DialogMpinBinding.clearMpin() {
    mpinBoxes.forEach { it.text?.clear() }
    etMpin1.requestFocus()
}
