package com.pangea.oriontek.ui.fragments.states

import com.pangea.oriontek.domain.model.ClientWithAddresses

sealed class CreateClientUiState {

    object Idle : CreateClientUiState()

    object Loading : CreateClientUiState()

    data class Success(val client: ClientWithAddresses) : CreateClientUiState()

    data class Error(val message: String) : CreateClientUiState()
}