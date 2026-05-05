package com.pangea.oriontek.ui.home.states

import com.pangea.oriontek.domain.model.Client

sealed class HomeUiState {

    object Loading : HomeUiState()

    data class Success(val clients: List<Client>) : HomeUiState()

    data class Error(val message: String) : HomeUiState()
}