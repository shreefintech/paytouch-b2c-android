package com.shreefintech.paytouchconsumer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ItemTransactionBinding
import com.shreefintech.paytouchconsumer.electricity.model.TransactionItem

class TransactionAdp(
    private val mContext: Context,
    private val mArrayList: ArrayList<TransactionItem>
) : RecyclerView.Adapter<TransactionAdp.ViewHolder>() {

    var onClickItem: ((TransactionItem) -> Unit)? = null

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
            ivCategoryIcon.setImageResource(item.categoryIconRes)
            tvMobile.text = item.mobileNumber
            tvTransactionId.text = item.transactionId
            tvAmount.text = item.amount
            tvStatus.text = item.status

            val (bgRes, textColor) = when (item.status) {
                "Success" -> Pair(
                    R.drawable.bg_status_success,
                    ContextCompat.getColor(mContext, R.color.toast_text_success)
                )
                "Failed" -> Pair(
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
    }

    override fun getItemCount(): Int = mArrayList.size
}
