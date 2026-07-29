package com.shreefintech.paytouchconsumer.utill

import android.app.Activity
import android.app.DatePickerDialog
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.SheetFilterBinding
import com.shreefintech.paytouchconsumer.utill.Utility.gone
import com.shreefintech.paytouchconsumer.utill.Utility.visible
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

    private var appliedFromDate: String? = null
    private var appliedToDate: String? = null
    private var appliedStatus: String? = null
    private var appliedConsumerNo: String? = null

    companion object {
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

        sheetBinding.ivClose.setOnClickListener {
            hide()
        }

        sheetBinding.cvSelectFromDate.setOnClickListener {
            Utility.hideKeyboard(activity)
            val today = Calendar.getInstance()
            val maxCal = parseDate(selectedToDate) ?: today
            showDatePicker(
                textView        = sheetBinding.tvFromDate,
                preSelectedDate = selectedFromDate,
                minCal          = null,
                maxCal          = maxCal
            ) { date -> selectedFromDate = date }
        }
        sheetBinding.cvSelectToDate.setOnClickListener {
            Utility.hideKeyboard(activity)
            showDatePicker(
                textView        = sheetBinding.tvToDate,
                preSelectedDate = selectedToDate,
                minCal          = parseDate(selectedFromDate),
                maxCal          = Calendar.getInstance()
            ) { date -> selectedToDate = date }
        }
        sheetBinding.cvStatusAll.setOnClickListener {
            selectedStatus = null
            updateStatusTabs()
        }
        sheetBinding.cvStatusSuccess.setOnClickListener {
            selectedStatus = STATUS_SUCCESS
            updateStatusTabs()
        }
        sheetBinding.cvStatusFailed.setOnClickListener {
            selectedStatus = STATUS_FAILED
            updateStatusTabs()
        }
        sheetBinding.cvStatusPending.setOnClickListener {
            selectedStatus = STATUS_PENDING
            updateStatusTabs()
        }

        updateStatusTabs()

        sheetBinding.btnReset.setOnClickListener {
            clearFilter()
        }
        sheetBinding.btnApply.setOnClickListener {
            applyFilter()
        }
    }

    // ─── Show / Hide ─────────────────────────────────────────────────────────

    fun show() {
        selectedFromDate = appliedFromDate
        selectedToDate   = appliedToDate
        selectedStatus   = appliedStatus
        sheetBinding.tvFromDate.text = appliedFromDate ?: ""
        sheetBinding.tvToDate.text   = appliedToDate ?: ""
        sheetBinding.tvSearch.setText(appliedConsumerNo ?: "")
        updateStatusTabs()
        bgOverlay.visible()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun hide() {
        Utility.hideKeyboard(activity)
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    fun isVisible(): Boolean = behavior.state != BottomSheetBehavior.STATE_HIDDEN

    // ─── Date picker ─────────────────────────────────────────────────────────

    private fun showDatePicker(
        textView: AppCompatTextView,
        preSelectedDate: String?,
        minCal: Calendar?,
        maxCal: Calendar,
        onSelected: (String) -> Unit
    ) {
        val initCal = parseDate(preSelectedDate) ?: Calendar.getInstance()
        val dialog = DatePickerDialog(
            activity,
            R.style.Paytouch_DatePickerTheme,
            { _, year, month, day ->
                val formatted = "%04d-%02d-%02d".format(year, month + 1, day)
                textView.text = formatted
                onSelected(formatted)
            },
            initCal.get(Calendar.YEAR),
            initCal.get(Calendar.MONTH),
            initCal.get(Calendar.DAY_OF_MONTH)
        )
        minCal?.let { dialog.datePicker.minDate = it.timeInMillis }
        dialog.datePicker.maxDate = maxCal.timeInMillis
        dialog.show()
    }

    private fun parseDate(dateStr: String?): Calendar? {
        if (dateStr.isNullOrEmpty()) return null
        return try {
            val parts = dateStr.split("-")
            Calendar.getInstance().apply {
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            null
        }
    }

    // ─── Status tabs ─────────────────────────────────────────────────────────

    private fun updateStatusTabs() {
        data class TabEntry(val card: MaterialCardView, val text: androidx.appcompat.widget.AppCompatTextView, val status: String?)
        listOf(
            TabEntry(sheetBinding.cvStatusAll,     sheetBinding.tvStatusAll,     null),
            TabEntry(sheetBinding.cvStatusSuccess, sheetBinding.tvStatusSuccess, STATUS_SUCCESS),
            TabEntry(sheetBinding.cvStatusFailed,  sheetBinding.tvStatusFailed,  STATUS_FAILED),
            TabEntry(sheetBinding.cvStatusPending, sheetBinding.tvStatusPending, STATUS_PENDING),
        ).forEach { tab ->
            val selected = selectedStatus == tab.status
            tab.card.setCardBackgroundColor(
                ContextCompat.getColor(activity, if (selected) R.color.primary else R.color.filter_status_bg)
            )
            tab.card.strokeColor = ContextCompat.getColor(activity, R.color.primary)
            tab.text.setTextColor(
                ContextCompat.getColor(activity, if (selected) R.color.white else R.color.primary)
            )
        }
    }

    // ─── Apply / Clear ───────────────────────────────────────────────────────

    private fun applyFilter() {
        val consumerNo = sheetBinding.tvSearch.text?.toString()?.trim() ?: ""
        val fromDateSet = !selectedFromDate.isNullOrEmpty()
        val toDateSet = !selectedToDate.isNullOrEmpty()

        val hasAnyFilter = fromDateSet || toDateSet || consumerNo.isNotEmpty() || selectedStatus != null
        if (!hasAnyFilter) {
            ToastUtil.showDelete(activity, activity.getString(R.string.msgSelectAtLeastOneFilter))
            return
        }

        appliedFromDate   = selectedFromDate
        appliedToDate     = selectedToDate
        appliedStatus     = selectedStatus
        appliedConsumerNo = consumerNo.ifEmpty { null }
        onApply(
            selectedFromDate,
            selectedToDate,
            selectedStatus,
            appliedConsumerNo
        )
        hide()
    }

    private fun clearFilter() {
        selectedFromDate  = null
        selectedToDate    = null
        selectedStatus    = null
        appliedFromDate   = null
        appliedToDate     = null
        appliedStatus     = null
        appliedConsumerNo = null
        sheetBinding.tvFromDate.text = ""
        sheetBinding.tvToDate.text   = ""
        sheetBinding.tvSearch.setText("")
        updateStatusTabs()
        onClear()
        hide()
    }
}
