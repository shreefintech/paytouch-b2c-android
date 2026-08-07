package com.shreefintech.paytouchconsumer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ItemWalletTransactionBinding
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletTransactionItem

class WalletTransactionAdp(
    private val mContext: Context,
    private val mArrayList: ArrayList<WalletTransactionItem>
) : RecyclerView.Adapter<WalletTransactionAdp.ViewHolder>() {

    inner class ViewHolder(val binding: ItemWalletTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWalletTransactionBinding.inflate(
            LayoutInflater.from(mContext), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mArrayList[position]
        holder.binding.apply {
            tvTitle.text = item.title
            tvDate.text = item.date
            if (item.isCredit) {
                ivIcon.setImageResource(R.drawable.ic_creadit)
                tvAmount.text = root.context.getString(R.string.textCreditSign, item.amount)
                tvAmount.setTextColor(ContextCompat.getColor(mContext, R.color.form_wizard_success))
            } else {
                ivIcon.setImageResource(R.drawable.ic_debit)
                tvAmount.text = root.context.getString(R.string.textDebitSign, item.amount)
                tvAmount.setTextColor(ContextCompat.getColor(mContext, R.color.form_wizard_reject))
            }
        }
    }

    override fun getItemCount(): Int = mArrayList.size
}
