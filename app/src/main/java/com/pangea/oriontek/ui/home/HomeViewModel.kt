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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getClients: GetClientsUseCase,
    private val deleteClient: DeleteClientUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _events = MutableSharedFlow<HomeEvent>()
    val events = _events.asSharedFlow()

    private var loadJob: Job? = null

    fun loadClients() {
        loadJob?.cancel()
        viewModelScope.launch {
            getClients()
                .catch { e ->
                    _uiState.value = HomeUiState.Error(e.message ?: "Error loading Clients")
                    _events.emit(HomeEvent.ShowMessage(R.string.message_error_deleting))
                }
                .collect { stores ->
                    _uiState.value = HomeUiState.Success(
                        stores.map { it.client }
                    )
                }
        }
    }


    fun delete(client: Client) {
        viewModelScope.launch {
            try {
                deleteClient(client)
            } catch (_: Exception) {
                _events.emit(HomeEvent.ShowMessage(R.string.message_error_deleting))
            }
        }
    }
}
