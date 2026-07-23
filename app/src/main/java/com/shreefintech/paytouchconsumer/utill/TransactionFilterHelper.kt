package com.shreefintech.paytouchconsumer.utill

import android.app.Activity
import android.app.DatePickerDialog
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.shreefintech.paytouchconsumer.databinding.SheetFilterBinding
import com.shreefintech.paytouchconsumer.electricity.model.TransactionItem
import com.shreefintech.paytouchconsumer.utill.Utility.gone
import com.shreefintech.paytouchconsumer.utill.Utility.visible
import com.shreefintech.paytouchconsumer.widget.CustomDropdown
import java.util.Calendar

class TransactionFilterHelper(
    private val activity: Activity,
    private val sheetBinding: SheetFilterBinding,
    private val bgOverlay: View,
    private val getList: () -> ArrayList<TransactionItem>,
    private val onApply: (filtered: ArrayList<TransactionItem>) -> Unit,
    private val onClear: () -> Unit
) {

    private lateinit var behavior: BottomSheetBehavior<View>

    private var selectedFromDate: String? = null
    private var selectedToDate:   String? = null
    private var selectedUserId:   String? = null
    private var selectedStatus:   String? = null

    companion object {
        private const val STATUS_SUCCESS = "Success"
        private const val STATUS_FAILED  = "Failed"
        private const val STATUS_PENDING = "Pending"
    }

    // ─── Setup ───────────────────────────────────────────────────────────────

    fun setup() {
        behavior = BottomSheetBehavior.from(sheetBinding.root)
        behavior.state = BottomSheetBehavior.STATE_HIDDEN

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED      -> bgOverlay.visible()
                    BottomSheetBehavior.STATE_HIDDEN        -> bgOverlay.gone()
                    BottomSheetBehavior.STATE_SETTLING      -> bgOverlay.visible()
                    BottomSheetBehavior.STATE_COLLAPSED     -> {}
                    BottomSheetBehavior.STATE_DRAGGING      -> {}
                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {}
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
        sheetBinding.cvSelectEntityType.setOnClickListener { showUserIdDropdown() }
        sheetBinding.cvSelectEntries.setOnClickListener    { showStatusDropdown() }

        sheetBinding.btnReset.setOnClickListener { clearFilter() }
        sheetBinding.btnApply.setOnClickListener { applyFilter() }
    }

    // ─── Show / Hide ─────────────────────────────────────────────────────────

    fun show() {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun hide() {
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    fun isVisible(): Boolean =
        behavior.state != BottomSheetBehavior.STATE_HIDDEN

    // ─── Date picker ─────────────────────────────────────────────────────────

    private fun showDatePicker(textView: AppCompatTextView, onSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            activity,
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

    // ─── Dropdowns ───────────────────────────────────────────────────────────

    private fun showUserIdDropdown() {
        val ids = getList().map { it.transactionId }.distinct()
        if (ids.isEmpty()) return
        CustomDropdown.showDropdown(
            activity   = activity,
            anchorView = sheetBinding.cvSelectEntityType,
            arrowView  = sheetBinding.ivEntityTypeArrow,
            textView   = sheetBinding.tvEntityType,
            items      = ids
        ) { selected, _ ->
            selectedUserId = selected
        }
    }

    private fun showStatusDropdown() {
        CustomDropdown.showDropdown(
            activity   = activity,
            anchorView = sheetBinding.cvSelectEntries,
            arrowView  = sheetBinding.ivEntriesArrow,
            textView   = sheetBinding.tvEntries,
            items      = listOf(STATUS_SUCCESS, STATUS_FAILED, STATUS_PENDING)
        ) { selected, _ ->
            selectedStatus = selected
        }
    }

    // ─── Apply / Clear ───────────────────────────────────────────────────────

    private fun applyFilter() {
        val mobile = sheetBinding.tvSearch.text?.toString()?.trim() ?: ""
        val list   = getList()
        val filtered = ArrayList(list.filter { item ->
            val matchesMobile = mobile.isEmpty() || item.mobileNumber.contains(mobile, ignoreCase = true)
            val matchesUserId = selectedUserId == null || item.transactionId == selectedUserId
            val matchesStatus = selectedStatus == null || item.status == selectedStatus
            // TODO(PAYTOUCH-546): Add date filtering once API returns date field in TransactionItem
            matchesMobile && matchesUserId && matchesStatus
        })
        onApply(filtered)
        hide()
    }

    private fun clearFilter() {
        selectedFromDate = null
        selectedToDate   = null
        selectedUserId   = null
        selectedStatus   = null
        sheetBinding.tvFromDate.text   = ""
        sheetBinding.tvToDate.text     = ""
        sheetBinding.tvEntityType.text = ""
        sheetBinding.tvEntries.text    = ""
        sheetBinding.tvSearch.setText("")
        onClear()
        hide()
    }
}