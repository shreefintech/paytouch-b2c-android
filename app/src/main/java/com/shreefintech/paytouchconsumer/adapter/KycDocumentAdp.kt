package com.shreefintech.paytouchconsumer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ItemKycDocumentBinding
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycDocumentDetailItem
import com.shreefintech.paytouchconsumer.utill.PdfThumbnailRepository
import kotlinx.coroutines.Job

class KycDocumentAdp(
    private val urlResolver: (String?) -> String? = { it },
    private val onItemClick: ((url: String, label: String) -> Unit)? = null
) : RecyclerView.Adapter<KycDocumentAdp.ViewHolder>() {

    private val items = mutableListOf<KycDocumentDetailItem>()

    fun updateList(newItems: List<KycDocumentDetailItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemKycDocumentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var thumbnailJob: Job? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemKycDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.thumbnailJob?.cancel()
        holder.thumbnailJob = null
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.thumbnailJob?.cancel()
        holder.thumbnailJob = null

        val item = items[position]
        val resolvedUrl = urlResolver(item.fileUrl)

        holder.binding.root.tag = resolvedUrl

        if (resolvedUrl.isNullOrBlank()) {
            holder.binding.pbItemLoading.visibility = View.GONE
            holder.binding.ivDocument.visibility = View.VISIBLE
            holder.binding.ivDocument.setImageResource(R.drawable.ic_file_not_found)
            return
        }

        holder.binding.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            val clickUrl = urlResolver(items[pos].fileUrl) ?: return@setOnClickListener
            onItemClick?.invoke(clickUrl, items[pos].label ?: "")
        }

        if (isPdf(resolvedUrl)) {
            holder.binding.pbItemLoading.visibility = View.VISIBLE
            holder.binding.ivDocument.visibility = View.INVISIBLE
            val cacheDir = holder.binding.root.context.cacheDir
            holder.thumbnailJob = PdfThumbnailRepository.loadThumbnail(resolvedUrl, cacheDir) { bitmap ->
                if (holder.binding.root.tag == resolvedUrl) {
                    holder.binding.pbItemLoading.visibility = View.GONE
                    holder.binding.ivDocument.visibility = View.VISIBLE
                    if (bitmap != null) holder.binding.ivDocument.setImageBitmap(bitmap)
                    else holder.binding.ivDocument.setImageResource(R.drawable.ic_file_not_found)
                }
            }
        } else {
            holder.binding.pbItemLoading.visibility = View.GONE
            holder.binding.ivDocument.visibility = View.VISIBLE
            Glide.with(holder.binding.ivDocument)
                .load(resolvedUrl)
                .placeholder(R.drawable.ic_file_not_found)
                .error(R.drawable.ic_file_not_found)
                .centerCrop()
                .into(holder.binding.ivDocument)
        }
    }

    private fun isPdf(url: String) = url.lowercase().let {
        it.endsWith(".pdf") || it.contains(".pdf?") || it.contains("type=pdf")
    }

    override fun getItemCount() = items.size
}
