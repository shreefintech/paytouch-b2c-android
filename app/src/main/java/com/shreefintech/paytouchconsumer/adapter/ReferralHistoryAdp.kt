package com.shreefintech.paytouchconsumer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ItemWalletTransactionBinding
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.BonusWalletHistoryItem
import com.shreefintech.paytouchconsumer.utill.Utility

class ReferralHistoryAdp(
    private val mContext: Context,
    private val mArrayList: ArrayList<BonusWalletHistoryItem>
) : RecyclerView.Adapter<ReferralHistoryAdp.ViewHolder>() {

    class ViewHolder(val binding: ItemWalletTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWalletTransactionBinding.inflate(LayoutInflater.from(mContext), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = holder.binding
        val row = mArrayList[position]
        item.tvTitle.text = row.description.orEmpty()
        item.tvDate.text = Utility.formatDate(row.createdAt)
        Glide.with(mContext).load(R.drawable.img_load_wallet).placeholder(R.drawable.ic_file_not_found).into(item.ivIcon)
        val amount = Utility.formatAmount(row.amount?.toString())
        if (row.isCredit) {
            item.cvMain.strokeColor = ContextCompat.getColor(mContext, R.color.form_wizard_success)
            item.tvAmount.text = mContext.getString(R.string.textCreditSign, amount)
            item.tvAmount.setTextColor(ContextCompat.getColor(mContext, R.color.form_wizard_success))
        } else {
            item.cvMain.strokeColor = ContextCompat.getColor(mContext, R.color.form_wizard_reject)
            item.tvAmount.text = mContext.getString(R.string.textDebitSign, amount)
            item.tvAmount.setTextColor(ContextCompat.getColor(mContext, R.color.form_wizard_reject))
        }
    }

    override fun getItemCount(): Int = mArrayList.size

    fun updateList(items: List<BonusWalletHistoryItem>) {
        mArrayList.clear()
        mArrayList.addAll(items)
        notifyDataSetChanged()
    }
}
