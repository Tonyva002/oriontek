package com.pangea.oriontek.ui.home.adapter

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.pangea.oriontek.databinding.ItemClientBinding
import com.pangea.oriontek.domain.model.Client

class ClientViewHolder(
    private val binding: ItemClientBinding,
    private val onClick: (Client) -> Unit,
    private val onDelete: (Client) -> Unit
) : RecyclerView.ViewHolder(binding.root) {


    fun render(client: Client) = with(binding) {
        tvName.text = buildString {
            append(client.name)
            append(" ")
            append(client.lastName)
        }
        tvCompany.text = client.company

        Glide.with(root.context)
            .load(client.photoResId)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .circleCrop()
            .into(imgPhoto)

        root.setOnClickListener { onClick(client) }

        root.setOnLongClickListener {
            onDelete(client)
            true
        }


    }

}
