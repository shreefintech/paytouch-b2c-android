package com.shreefintech.paytouchconsumer.operator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.adapter.OperatorSelectionAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityOperatorSelectionBinding
import com.shreefintech.paytouchconsumer.operator.model.OperatorSelectionItem
import com.shreefintech.paytouchconsumer.utill.Utility

class OperatorSelectionActivity : BaseActivity() {

    private lateinit var binding: ActivityOperatorSelectionBinding
    private lateinit var operatorAdp: OperatorSelectionAdp

    private val items: List<OperatorSelectionItem> by lazy {
        val json = intent.getStringExtra(EXTRA_ITEMS) ?: return@lazy emptyList<OperatorSelectionItem>()
        val type = object : TypeToken<List<OperatorSelectionItem>>() {}.type
        Gson().fromJson(json, type) ?: emptyList()
    }

    private val preSelectedId: String? by lazy {
        intent.getStringExtra(EXTRA_SELECTED_ID)
    }

    companion object {
        private const val EXTRA_ITEMS = "extra_items"
        private const val EXTRA_SELECTED_ID = "extra_selected_id"
        const val EXTRA_SELECTED = "extra_selected"

        fun newIntent(
            context: Context,
            items: List<OperatorSelectionItem>,
            selectedId: String?
        ): Intent = Intent(context, OperatorSelectionActivity::class.java).apply {
            putExtra(EXTRA_ITEMS, Gson().toJson(items))
            selectedId?.let { putExtra(EXTRA_SELECTED_ID, it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperatorSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, maxOf(imeInsets.bottom, systemBars.bottom))
            insets
        }

        binding.onClickListener = onClickListener()
        setupRecyclerView()
        setupSearch()
        onBack()
    }

    private fun setupRecyclerView() {
        operatorAdp = OperatorSelectionAdp(items)
        operatorAdp.selectedId = preSelectedId
        operatorAdp.onSelectItem = { item ->
            setResult(RESULT_OK, Intent().putExtra(EXTRA_SELECTED, Gson().toJson(item)))
            finish()
        }
        binding.rvOperators.apply {
            layoutManager = LinearLayoutManager(mActivity)
            adapter = operatorAdp
        }
        binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                binding.ivClear.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                operatorAdp.filter(query)
                binding.tvEmpty.visibility = if (operatorAdp.itemCount == 0) View.VISIBLE else View.GONE
            }
        })
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
                binding.ivClear -> {
                    if (Utility.stopClick()) return@OnClickListener
                    binding.etSearch.setText("")
                    Utility.hideKeyboard(mActivity)
                }
            }
        }
    }
}
