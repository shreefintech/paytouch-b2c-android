package com.shreefintech.paytouchconsumer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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

    var onClickItem: ((WalletTransactionItem) -> Unit)? = null
    private val animatedPositions = mutableSetOf<Int>()

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
                val row = mArrayList[pos]
                if (row.isClickable) onClickItem?.invoke(row)
            }
        }
        holder.binding.apply {
            tvTitle.text = item.title
            tvDate.text = item.date
            Glide.with(mContext).load(item.categoryIconRes).placeholder(R.drawable.ic_file_not_found).into(ivIcon)
            when {
                // Failed withdrawal — money went back to the wallet: grey, unsigned
                item.isFailed -> {
                    cvMain.strokeColor = ContextCompat.getColor(mContext, R.color.hint_color)
                    tvAmount.text = item.amount
                    tvAmount.setTextColor(ContextCompat.getColor(mContext, R.color.hint_color))
                }
                item.isCredit -> {
                    cvMain.strokeColor = ContextCompat.getColor(mContext, R.color.form_wizard_success)
                    tvAmount.text = mContext.getString(R.string.textCreditSign, item.amount)
                    tvAmount.setTextColor(ContextCompat.getColor(mContext, R.color.form_wizard_success))
                }
                else -> {
                    cvMain.strokeColor = ContextCompat.getColor(mContext, R.color.form_wizard_reject)
                    tvAmount.text = mContext.getString(R.string.textDebitSign, item.amount)
                    tvAmount.setTextColor(ContextCompat.getColor(mContext, R.color.form_wizard_reject))
                }
            }
            val statusRes = item.statusLabelRes
            if (statusRes != null) {
                tvRowStatus.setText(statusRes)
                tvRowStatus.visibility = View.VISIBLE
            } else {
                tvRowStatus.visibility = View.GONE
            }
        }
        // Only first-screen rows animate, so only they need tracking.
        val visibleCount = AnimationHelper.visibleCount(holder.itemView.parent as? RecyclerView)
        if (position < visibleCount && animatedPositions.add(position)) {
            AnimationHelper.animateListRowEntrance(holder.binding.root, position, visibleCount)
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
