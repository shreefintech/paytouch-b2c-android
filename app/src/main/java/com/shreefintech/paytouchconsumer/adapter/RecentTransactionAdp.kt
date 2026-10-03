package com.shreefintech.paytouchconsumer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ItemRecentTransactionBinding
import com.shreefintech.paytouchconsumer.transactions.model.RecentTransactionItem
import com.shreefintech.paytouchconsumer.utill.AnimationHelper

class RecentTransactionAdp(
    private val mContext: Context,
    private val mArrayList: ArrayList<RecentTransactionItem>
) : RecyclerView.Adapter<RecentTransactionAdp.ViewHolder>() {

    private val animatedPositions = mutableSetOf<Int>()

    inner class ViewHolder(val binding: ItemRecentTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentTransactionBinding.inflate(
            LayoutInflater.from(mContext), parent, false
        )
        return ViewHolder(binding)
    }

    private var expandedPosition = -1

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_EXPAND)) {
            bindExpandState(holder.binding, mArrayList[position], animate = true)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mArrayList[position]
        bindItem(holder.binding, item)

        holder.binding.llHeader.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            if (pos == expandedPosition) {
                mArrayList[pos].isExpanded = false
                expandedPosition = -1
                notifyItemChanged(pos, PAYLOAD_EXPAND)
            } else {
                val prev = expandedPosition
                if (prev != -1) {
                    mArrayList[prev].isExpanded = false
                    notifyItemChanged(prev, PAYLOAD_EXPAND)
                }
                mArrayList[pos].isExpanded = true
                expandedPosition = pos
                notifyItemChanged(pos, PAYLOAD_EXPAND)
            }
        }

        if (position !in animatedPositions) {
            animatedPositions.add(position)
            AnimationHelper.animateListRowEntrance(holder.binding.root, position, AnimationHelper.visibleCount(holder.itemView.parent as? RecyclerView))
        }
    }

    private fun bindItem(binding: ItemRecentTransactionBinding, item: RecentTransactionItem) {
        with(binding) {
            val context = root.context
            Glide.with(mContext).load(item.categoryIconRes).placeholder(R.drawable.ic_file_not_found).into(ivCategoryIcon)
            tvCategoryName.text = item.categoryName
            tvCollapsedDate.text = item.date

            val statusText = item.status.replaceFirstChar { it.uppercaseChar() }
            val textColor = when (item.status.lowercase()) {
                "success" -> ContextCompat.getColor(context, R.color.toast_text_success)
                "failed"  -> ContextCompat.getColor(context, R.color.form_wizard_reject)
                else      -> ContextCompat.getColor(context, R.color.orange)
            }
            tvStatus.text = context.getString(R.string.labelStatusBullet, statusText)
            tvStatus.setTextColor(textColor)

            tvDetailAmount.text = context.getString(R.string.labelDetailAmount, item.amount)
            val accountLabelRes = when {
                item.isVehicleCategory -> R.string.labelDetailVehicleNo
                item.isMobileCategory  -> R.string.labelDetailMobileNo
                else                   -> R.string.labelDetailConsumerNo
            }
            tvDetailAccountNumber.text = context.getString(accountLabelRes, item.accountNumber)
            tvDetailReference.text = context.getString(R.string.labelDetailReference, item.reference)
        }
        bindExpandState(binding, item, animate = false)
    }

    private fun bindExpandState(binding: ItemRecentTransactionBinding, item: RecentTransactionItem, animate: Boolean) {
        with(binding) {
            val targetRotation = if (item.isExpanded) 180f else 0f
            ivChevron.animate().cancel()
            if (animate) {
                ivChevron.animate().rotation(targetRotation).setDuration(200)
                    .setInterpolator(if (item.isExpanded) DecelerateInterpolator() else AccelerateInterpolator()).start()
            } else {
                ivChevron.rotation = targetRotation
            }
            llExpandedContent.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
        }
    }

    fun updateList(items: List<RecentTransactionItem>) {
        expandedPosition = -1
        animatedPositions.clear()
        mArrayList.clear()
        mArrayList.addAll(items)
        notifyDataSetChanged()
    }

    fun appendList(items: List<RecentTransactionItem>) {
        val insertStart = mArrayList.size
        mArrayList.addAll(items)
        notifyItemRangeInserted(insertStart, items.size)
    }

    override fun getItemCount(): Int = mArrayList.size

    companion object {
        private const val PAYLOAD_EXPAND = "payload_expand"
    }
}
