package com.shreefintech.paytouchconsumer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shreefintech.paytouchconsumer.databinding.ItemBannerBinding

class BannerAdp(
    private val context: Context,
    private val banners: List<Int>
) : RecyclerView.Adapter<BannerAdp.ViewHolder>() {

    inner class ViewHolder(val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemBannerBinding.inflate(LayoutInflater.from(context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(context).load(banners[position]).centerCrop().into(holder.binding.ivBanner)
    }

    override fun getItemCount() = banners.size
}
