package com.shreefintech.paytouchconsumer.electricity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.RecentTransactionAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityRecentTransactionBinding
import com.shreefintech.paytouchconsumer.electricity.model.RecentTransactionItem
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.Utility

class RecentTransactionActivity : BaseActivity() {

    private lateinit var binding: ActivityRecentTransactionBinding
    private lateinit var recentTransactionAdp: RecentTransactionAdp

    private val mList = ArrayList<RecentTransactionItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecentTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        LiquidGlassEffect.attach(
            targetView   = binding.flCard,
            rootView     = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion   = 0f,
            blur         = resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
            strokeColor  = Color.argb(180, 213, 38, 98),
            strokeWidth  = 1,
            solidStroke  = true,
        )

        loadDummyData()
        setupRecyclerView()

        binding.onClickListener = onClickListener()
        onBack()
    }

    private fun loadDummyData() {
        mList.apply {
            add(
                RecentTransactionItem(
                    "Electricity Bill",
                    "Jadav Jayesh Trikambhai",
                    "25 Apr 2026",
                    "Pending",
                    "₹1,724",
                    "30723111936",
                    "PYTCH250426195020166",
                    R.drawable.ic_electricity
                )
            )
            add(
                RecentTransactionItem(
                    "Electricity Bill",
                    "Ravi Kumar",
                    "24 Apr 2026",
                    "Success",
                    "₹892",
                    "45612347891",
                    "PYTCH240426084532177",
                    R.drawable.ic_electricity
                )
            )
            add(
                RecentTransactionItem(
                    "Gas Bill",
                    "Priya Sharma",
                    "23 Apr 2026",
                    "Failed",
                    "₹530",
                    "78451236542",
                    "PYTCH230426153218288",
                    R.drawable.ic_gas
                )
            )
            add(
                RecentTransactionItem(
                    "Electricity Bill",
                    "Arjun Patel",
                    "22 Apr 2026",
                    "Success",
                    "₹2,140",
                    "56789012345",
                    "PYTCH220426112245399",
                    R.drawable.ic_electricity
                )
            )
            add(
                RecentTransactionItem(
                    "Gas Bill",
                    "Sunita Desai",
                    "21 Apr 2026",
                    "Pending",
                    "₹318",
                    "89034567890",
                    "PYTCH210426091830410",
                    R.drawable.ic_gas
                )
            )
            add(
                RecentTransactionItem(
                    "Electricity Bill",
                    "Mohan Verma",
                    "20 Apr 2026",
                    "Success",
                    "₹1,456",
                    "12345678901",
                    "PYTCH200426145612521",
                    R.drawable.ic_electricity
                )
            )
        }
    }

    private fun setupRecyclerView() {
        recentTransactionAdp = RecentTransactionAdp(mActivity, mList)
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(mActivity)
            adapter = recentTransactionAdp
        }
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.lytToolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }
}
