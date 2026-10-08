package com.shreefintech.paytouchconsumer.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ItemBannerBinding

class BannerAdp(
    private val banners: List<Int>
) : RecyclerView.Adapter<BannerAdp.ViewHolder>() {

    class ViewHolder(val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.itemView.context).load(banners[position]).placeholder(ColorDrawable(Color.LTGRAY)).error(R.drawable.ic_file_not_found).centerCrop().into(holder.binding.ivBanner)
    }

    override fun getItemCount() = banners.size

    override fun onViewRecycled(holder: ViewHolder) {
        Glide.with(holder.itemView.context).clear(holder.binding.ivBanner)
        super.onViewRecycled(holder)
    }
}
