package com.pangea.oriontek.ui.fragments.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pangea.oriontek.databinding.ItemAddressFieldBinding
import com.pangea.oriontek.domain.model.Address

class AddressAdapter(
    private val onAddressChanged: (Int, String) -> Unit,
    private val onRemoveAddress: (Int) -> Unit
) : ListAdapter<Address, AddressAdapter.AddressViewHolder>(AddressDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val binding = ItemAddressFieldBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AddressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class AddressViewHolder(private val binding: ItemAddressFieldBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var textWatcher: android.text.TextWatcher? = null

        fun bind(address: Address, position: Int) {
            binding.etAddress.removeTextChangedListener(textWatcher)
            
            if (binding.etAddress.text.toString() != address.fullAddress) {
                binding.etAddress.setText(address.fullAddress)
                // Mantener el cursor al final después de actualizar el texto
                binding.etAddress.setSelection(binding.etAddress.text?.length ?: 0)
            }

            textWatcher = binding.etAddress.doAfterTextChanged {
                onAddressChanged(bindingAdapterPosition, it.toString())
            }

            binding.btnRemoveAddress.setOnClickListener {
                onRemoveAddress(adapterPosition)
            }
        }
    }

    class AddressDiffCallback : DiffUtil.ItemCallback<Address>() {
        override fun areItemsTheSame(oldItem: Address, newItem: Address): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Address, newItem: Address): Boolean {
            return oldItem.fullAddress == newItem.fullAddress
        }
    }
}
