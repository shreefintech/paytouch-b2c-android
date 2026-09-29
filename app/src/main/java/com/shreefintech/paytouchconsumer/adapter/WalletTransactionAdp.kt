package com.shreefintech.paytouchconsumer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ItemWalletTransactionBinding
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletTransactionItem
import com.shreefintech.paytouchconsumer.utill.AnimationHelper

class WalletTransactionAdp(
    private val mContext: Context,
    private val mArrayList: ArrayList<WalletTransactionItem>
) : RecyclerView.Adapter<WalletTransactionAdp.ViewHolder>() {

    var onClickItem: ((transactionId: String) -> Unit)? = null
    private val animatedPositions = mutableSetOf<Int>()
    private var layoutManager: LinearLayoutManager? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        layoutManager = recyclerView.layoutManager as? LinearLayoutManager
    }

    private fun visibleItemCount(): Int {
        val lm = layoutManager ?: return 10
        val first = lm.findFirstVisibleItemPosition()
        val last = lm.findLastVisibleItemPosition()
        return if (last >= 0 && first >= 0) (last - first + 2).coerceAtLeast(5) else 10
    }

    class ViewHolder(val binding: ItemWalletTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWalletTransactionBinding.inflate(
            LayoutInflater.from(mContext), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mArrayList[position]
        holder.binding.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                val id = mArrayList[pos].transactionId
                if (id.isNotEmpty()) onClickItem?.invoke(id)
            }
        }
        holder.binding.apply {
            tvTitle.text = item.title
            tvDate.text = item.date
            Glide.with(mContext).load(item.categoryIconRes).into(ivIcon)
            if (item.isCredit) {
                cvMain.strokeColor = ContextCompat.getColor(mContext, R.color.form_wizard_success)
                tvAmount.text = root.context.getString(R.string.textCreditSign, item.amount)
                tvAmount.setTextColor(ContextCompat.getColor(mContext, R.color.form_wizard_success))
            } else {
                cvMain.strokeColor = ContextCompat.getColor(mContext, R.color.form_wizard_reject)
                tvAmount.text = root.context.getString(R.string.textDebitSign, item.amount)
                tvAmount.setTextColor(ContextCompat.getColor(mContext, R.color.form_wizard_reject))
            }
        }
        if (position !in animatedPositions) {
            animatedPositions.add(position)
            AnimationHelper.animateListRowEntrance(holder.binding.root, position, visibleItemCount())
        }
    }

    override fun getItemCount(): Int = mArrayList.size

    fun updateList(items: List<WalletTransactionItem>) {
        animatedPositions.clear()
        mArrayList.clear()
        mArrayList.addAll(items)
        notifyDataSetChanged()
    }
}
