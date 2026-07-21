package com.shreefintech.paytouchconsumer.electricity.transactions

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.TransactionAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityTransactionReportBinding
import com.shreefintech.paytouchconsumer.electricity.model.TransactionItem
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.TransactionFilterHelper
import com.shreefintech.paytouchconsumer.utill.Utility

class TransactionReportActivity : BaseActivity() {

    private lateinit var binding: ActivityTransactionReportBinding
    private lateinit var transactionAdp: TransactionAdp
    private lateinit var filterHelper: TransactionFilterHelper

    private val mAllList = ArrayList<TransactionItem>()
    private val mDisplayList = ArrayList<TransactionItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(imeInsets.bottom, systemBars.bottom)
            )
            binding.incFilterSheet.root.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        LiquidGlassEffect.attach(
            targetView = binding.flCard,
            rootView = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
            strokeColor = Color.argb(180, 213, 38, 98),
            strokeWidth = 1,
            solidStroke = true,
        )

        loadDummyData()
        setupRecyclerView()
        setupSearch()
        setupFilterSheet()

        binding.onClickListener = onClickListener()
        onBack()
    }

    private fun loadDummyData() {
        mAllList.apply {
            add(
                TransactionItem(
                    "9876*****0", "BC88213045", "₹149.00", "Success",
                    R.drawable.ic_electricity, "Ravi Kumar", "18-07-2026, 09:15 am",
                    "₹3.00", "₹152.00", "TXN10235", "USR001",
                    "30723111936", "Paschim Gujarat Vij Company Ltd"
                )
            )
            add(
                TransactionItem(
                    "9823*****1", "BC88214234", "₹320.00", "Failed",
                    R.drawable.ic_electricity, "Priya Sharma", "17-07-2026, 02:45 pm",
                    "₹5.00", "₹325.00", "TXN10236", "USR002",
                    "30723222047", "Torrent Power Limited"
                )
            )
            add(
                TransactionItem(
                    "9845*****2", "BC88215678", "₹85.50", "Pending",
                    R.drawable.ic_gas, "Arjun Patel", "17-07-2026, 11:30 am",
                    "₹2.00", "₹87.50", "TXN10237", "USR003",
                    "30723333158", "Gujarat Gas Company Ltd"
                )
            )
            add(
                TransactionItem(
                    "9812*****3", "BC88216543", "₹450.00", "Success",
                    R.drawable.ic_electricity, "Sunita Desai", "16-07-2026, 04:20 pm",
                    "₹8.00", "₹458.00", "TXN10238", "USR004",
                    "30723444269", "MSEDCL Nagpur Zone"
                )
            )
            add(
                TransactionItem(
                    "9867*****4", "BC88217890", "₹200.00", "Success",
                    R.drawable.ic_gas, "Mohan Verma", "16-07-2026, 01:00 pm",
                    "₹4.00", "₹204.00", "TXN10239", "USR005",
                    "30723555370", "Mahanagar Gas Ltd"
                )
            )
            add(
                TransactionItem(
                    "9834*****5", "BC88218123", "₹75.00", "Failed",
                    R.drawable.ic_electricity, "Anita Singh", "15-07-2026, 10:10 am",
                    "₹2.00", "₹77.00", "TXN10240", "USR006",
                    "30723666481", "BSES Rajdhani Power Ltd"
                )
            )
            add(
                TransactionItem(
                    "9856*****6", "BC88219456", "₹560.00", "Pending",
                    R.drawable.ic_electricity, "Vikram Nair", "15-07-2026, 05:50 pm",
                    "₹9.00", "₹569.00", "TXN10241", "USR007",
                    "30723777592", "KSEB Kerala"
                )
            )
            add(
                TransactionItem(
                    "9801*****7", "BC88220789", "₹130.00", "Success",
                    R.drawable.ic_gas, "Kavita Mehta", "14-07-2026, 03:30 pm",
                    "₹3.00", "₹133.00", "TXN10242", "USR008",
                    "30723888603", "Indraprastha Gas Ltd"
                )
            )
            add(
                TransactionItem(
                    "9878*****8", "BC88221012", "₹298.00", "Success",
                    R.drawable.ic_electricity, "Deepak Sharma", "14-07-2026, 08:45 am",
                    "₹5.00", "₹303.00", "TXN10243", "USR009",
                    "30723999714", "TPDDL Delhi"
                )
            )
            add(
                TransactionItem(
                    "9890*****9", "BC88222345", "₹695.00", "Failed",
                    R.drawable.ic_electricity, "Radha Krishnan", "13-07-2026, 12:15 pm",
                    "₹10.00", "₹705.00", "TXN10244", "USR010",
                    "30724000825", "TANGEDCO Tamil Nadu"
                )
            )
        }
        mDisplayList.addAll(mAllList)
    }

    private fun setupRecyclerView() {
        transactionAdp = TransactionAdp(mActivity, mDisplayList)
        transactionAdp.onClickItem = { item ->
            TransactionDetailActivity.start(mActivity, item)
        }
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(mActivity)
            adapter = transactionAdp
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterList(s?.toString()?.trim() ?: "")
            }
        })
    }

    private fun setupFilterSheet() {
        filterHelper = TransactionFilterHelper(
            activity = mActivity,
            sheetBinding = binding.incFilterSheet,
            bgOverlay = binding.viewBg,
            getList = { mAllList },
            onApply = { filtered ->
                mDisplayList.clear()
                mDisplayList.addAll(filtered)
                transactionAdp.notifyDataSetChanged()
                binding.tvEmpty.visibility =
                    if (mDisplayList.isEmpty()) View.VISIBLE else View.GONE
            },
            onClear = {
                binding.etSearch.setText("")
                mDisplayList.clear()
                mDisplayList.addAll(mAllList)
                transactionAdp.notifyDataSetChanged()
                binding.tvEmpty.visibility = View.GONE
            }
        )
        filterHelper.setup()
    }

    private fun filterList(query: String) {
        mDisplayList.clear()
        if (query.isEmpty()) {
            mDisplayList.addAll(mAllList)
        } else {
            val lower = query.lowercase()
            mAllList.filterTo(mDisplayList) {
                it.mobileNumber.lowercase().contains(lower) ||
                        it.transactionId.lowercase().contains(lower)
            }
        }
        transactionAdp.notifyDataSetChanged()
        binding.tvEmpty.visibility =
            if (mDisplayList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (filterHelper.isVisible()) {
                    filterHelper.hide()
                } else {
                    finish()
                }
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

                binding.flFilter -> {
                    if (Utility.stopClick()) return@OnClickListener
                    filterHelper.show()
                }
            }
        }
    }
}
