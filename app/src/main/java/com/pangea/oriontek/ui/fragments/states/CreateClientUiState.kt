package com.pangea.oriontek.ui.fragments.states

import com.pangea.oriontek.domain.model.Address
import com.pangea.oriontek.domain.model.Client


data class CreateClientFormState(
    val client: Client = Client(),
    val addresses: List<Address> = emptyList(),
    val isEditMode: Boolean = false
)

data class ValidationErrors(
    val name: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val company: String? = null,
    val phone: String? = null,
    val address: String? = null
) {
    fun hasErrors() =
        listOf(name, lastName, email, phone, address).any { it != null }
}

sealed class CreateClientUiState {

    object Loading : CreateClientUiState()

    data class Form(
        val data: CreateClientFormState,
        val errors: ValidationErrors = ValidationErrors()
    ) : CreateClientUiState()

    data class Error(
        val message: String
    ) : CreateClientUiState()
}