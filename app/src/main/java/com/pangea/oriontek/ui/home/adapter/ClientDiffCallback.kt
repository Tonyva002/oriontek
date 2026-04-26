package com.pangea.oriontek.ui.home.adapter

import androidx.recyclerview.widget.DiffUtil
import com.pangea.oriontek.domain.model.Client

class ClientDiffCallback : DiffUtil.ItemCallback<Client>() {
    override fun areItemsTheSame(
        oldItem: Client,
        newItem: Client
    ): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(
        oldItem: Client,
        newItem: Client
    ): Boolean {
        return oldItem == newItem
    }

}