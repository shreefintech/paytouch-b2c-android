package com.shreefintech.paytouchconsumer.utill

import android.app.Activity
import android.app.DatePickerDialog
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.SheetFilterBinding
import com.shreefintech.paytouchconsumer.utill.Utility.gone
import com.shreefintech.paytouchconsumer.utill.Utility.visible
import com.shreefintech.paytouchconsumer.widget.CustomDropdown
import java.util.Calendar

class TransactionFilterHelper(
    private val activity: Activity,
    private val sheetBinding: SheetFilterBinding,
    private val bgOverlay: View,
    private val onApply: (fromDate: String?, toDate: String?, status: String?, consumerNo: String?) -> Unit,
    private val onClear: () -> Unit
) {

    private lateinit var behavior: BottomSheetBehavior<View>

    private var selectedFromDate: String? = null
    private var selectedToDate: String? = null
    private var selectedStatus: String? = null

    companion object {
        private const val STATUS_ALL = "All Status"
        private const val STATUS_SUCCESS = "Success"
        private const val STATUS_FAILED = "Failed"
        private const val STATUS_PENDING = "Pending"
    }

    // ─── Setup ───────────────────────────────────────────────────────────────

    fun setup() {
        behavior = BottomSheetBehavior.from(sheetBinding.root)
        behavior.state = BottomSheetBehavior.STATE_HIDDEN

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> bgOverlay.visible()
                    BottomSheetBehavior.STATE_HIDDEN -> bgOverlay.gone()
                    BottomSheetBehavior.STATE_SETTLING -> bgOverlay.visible()
                    else -> {}
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                bgOverlay.alpha = slideOffset.coerceIn(0f, 1f)
            }
        })

        bgOverlay.setOnClickListener { hide() }
        sheetBinding.ivClose.setOnClickListener { hide() }

        sheetBinding.cvSelectFromDate.setOnClickListener {
            showDatePicker(sheetBinding.tvFromDate) { date -> selectedFromDate = date }
        }
        sheetBinding.cvSelectToDate.setOnClickListener {
            showDatePicker(sheetBinding.tvToDate) { date -> selectedToDate = date }
        }
        sheetBinding.cvSelectEntries.setOnClickListener { showStatusDropdown() }

        sheetBinding.btnReset.setOnClickListener { clearFilter() }
        sheetBinding.btnApply.setOnClickListener { applyFilter() }
    }

    // ─── Show / Hide ─────────────────────────────────────────────────────────

    fun show() {
        bgOverlay.visible()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun hide() {
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    fun isVisible(): Boolean = behavior.state != BottomSheetBehavior.STATE_HIDDEN

    // ─── Date picker ─────────────────────────────────────────────────────────

    private fun showDatePicker(textView: AppCompatTextView, onSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            activity,
            R.style.Paytouch_DatePickerTheme,
            { _, year, month, day ->
                val formatted = "%04d-%02d-%02d".format(year, month + 1, day)
                textView.text = formatted
                onSelected(formatted)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ─── Status dropdown ─────────────────────────────────────────────────────

    private fun showStatusDropdown() {
        CustomDropdown.showDropdown(
            activity = activity,
            anchorView = sheetBinding.cvSelectEntries,
            arrowView = sheetBinding.ivEntriesArrow,
            textView = sheetBinding.tvEntries,
            items = listOf(STATUS_ALL, STATUS_SUCCESS, STATUS_FAILED, STATUS_PENDING)
        ) { selected, _ ->
            selectedStatus = if (selected == STATUS_ALL) null else selected
        }
    }

    // ─── Apply / Clear ───────────────────────────────────────────────────────

    private fun applyFilter() {
        val consumerNo = sheetBinding.tvSearch.text?.toString()?.trim() ?: ""
        val fromDateSet = !selectedFromDate.isNullOrEmpty()
        val toDateSet = !selectedToDate.isNullOrEmpty()

        if (fromDateSet != toDateSet) {
            ToastUtil.showDelete(activity, activity.getString(R.string.msgSelectBothDates))
            return
        }

        // Status alone does not count as a filter
        if (consumerNo.isEmpty() && !fromDateSet) {
            ToastUtil.showDelete(activity, activity.getString(R.string.msgSelectAtLeastOneFilter))
            return
        }

        onApply(
            selectedFromDate,
            selectedToDate,
            selectedStatus,
            consumerNo.ifEmpty { null }
        )
        hide()
    }

    private fun clearFilter() {
        selectedFromDate = null
        selectedToDate = null
        selectedStatus = null
        sheetBinding.tvFromDate.text = ""
        sheetBinding.tvToDate.text = ""
        sheetBinding.tvEntries.text = ""
        sheetBinding.tvSearch.setText("")
        onClear()
        hide()
    }
}
