package com.shreefintech.paytouchconsumer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ItemTransactionBinding
import com.shreefintech.paytouchconsumer.transactions.model.TransactionItem
import com.shreefintech.paytouchconsumer.utill.AnimationHelper
import com.shreefintech.paytouchconsumer.utill.Utility

class TransactionAdp(
    private val mContext: Context,
    private val mArrayList: ArrayList<TransactionItem>
) : RecyclerView.Adapter<TransactionAdp.ViewHolder>() {

    var onClickItem: ((TransactionItem) -> Unit)? = null
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

    inner class ViewHolder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(mContext), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mArrayList[position]
        holder.binding.apply {
            Glide.with(mContext).load(item.categoryIconRes).into(ivCategoryIcon)
            tvMobile.text = Utility.maskNumber(item.mobileNumber)
            tvTransactionId.text = item.transactionId
            tvAmount.text = item.amount
            tvStatus.text = item.status

            val (bgRes, textColor) = when (item.status.lowercase()) {
                "success" -> Pair(
                    R.drawable.bg_status_success,
                    ContextCompat.getColor(mContext, R.color.toast_text_success)
                )
                "failed" -> Pair(
                    R.drawable.bg_status_failed,
                    ContextCompat.getColor(mContext, R.color.form_wizard_reject)
                )
                else -> Pair(
                    R.drawable.bg_status_pending,
                    ContextCompat.getColor(mContext, R.color.orange)
                )
            }
            tvStatus.setBackgroundResource(bgRes)
            tvStatus.setTextColor(textColor)
        }

        holder.binding.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onClickItem?.invoke(mArrayList[pos])
            }
        }

        if (position !in animatedPositions) {
            animatedPositions.add(position)
            AnimationHelper.animateListRowEntrance(holder.binding.root, position, visibleItemCount())
        }
    }

    fun updateList(items: List<TransactionItem>) {
        animatedPositions.clear()
        mArrayList.clear()
        mArrayList.addAll(items)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = mArrayList.size
}
