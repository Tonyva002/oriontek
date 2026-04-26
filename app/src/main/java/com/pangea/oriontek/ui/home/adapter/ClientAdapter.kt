package com.pangea.oriontek.ui.home.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.pangea.oriontek.R
import com.pangea.oriontek.databinding.ItemClientBinding
import com.pangea.oriontek.domain.model.Client

class ClientAdapter(
    private val onClick: (Client) -> Unit,
    private val onDelete: (Client) -> Unit

) : ListAdapter<Client, ClientViewHolder>(ClientDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ClientViewHolder {

            val binding = ItemClientBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            return ClientViewHolder(binding, onClick, onDelete)

    }

    override fun onBindViewHolder(
        holder: ClientViewHolder,
        position: Int
    ) {
        holder.render(getItem(position))
    }
}