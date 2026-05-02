package com.pangea.oriontek.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pangea.oriontek.R
import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.domain.usecase.client.DeleteClientUseCase
import com.pangea.oriontek.domain.usecase.client.GetClientsUseCase
import com.pangea.oriontek.ui.home.states.HomeEvent
import com.pangea.oriontek.ui.home.states.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getClients: GetClientsUseCase,
    private val deleteClient: DeleteClientUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events = _events.asSharedFlow()

    private var loadJob: Job? = null

    // Cargar clientes
    fun loadClients() {
        // Cancelar carga anterior si existe
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            try {
                _uiState.value = HomeUiState.Loading

                getClients().collect { clients ->
                    _uiState.value = HomeUiState.Success(
                        clients.map { it.client }
                    )
                }

            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(
                    e.message ?: "Error loading clients"
                )

                _events.emit(
                    HomeEvent.ShowMessage(R.string.message_error_loading)
                )
            }
        }
    }

    // Eliminar cliente
    fun delete(client: Client) {
        viewModelScope.launch {
            runCatching {
                deleteClient(client)
            }.onFailure {
                _events.emit(
                    HomeEvent.ShowMessage(R.string.message_error_deleting)
                )
            }
        }
    }
}