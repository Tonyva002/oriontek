package com.pangea.oriontek.ui.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pangea.oriontek.R
import com.pangea.oriontek.domain.model.Address
import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.domain.model.ClientWithAddresses
import com.pangea.oriontek.domain.usecase.client.GetClientByIdUseCase
import com.pangea.oriontek.domain.usecase.client.InsertClientWithAddressesUseCase
import com.pangea.oriontek.domain.usecase.client.UpdateClientWithAddressesUseCase
import com.pangea.oriontek.ui.fragments.states.CreateClientEvent
import com.pangea.oriontek.ui.fragments.states.CreateClientUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateClientViewModel @Inject constructor(
    private val insertClient: InsertClientWithAddressesUseCase,
    private val updateClient: UpdateClientWithAddressesUseCase,
    private val getClientById: GetClientByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateClientUiState>(CreateClientUiState.Idle)
    val uiState: StateFlow<CreateClientUiState> = _uiState

    private val _events = MutableSharedFlow<CreateClientEvent>()
    val events = _events.asSharedFlow()

    // ====================
    // LOAD
    // ====================

    fun loadClient(id: Long) {
        viewModelScope.launch {
            _uiState.value = CreateClientUiState.Loading

            getClientById(id).collect { result ->

                if (result != null) {
                    _uiState.value = CreateClientUiState.Success(result)
                } else {
                    _uiState.value = CreateClientUiState.Error("Client not found")
                }
            }
        }
    }

    // ====================
    // SAVE
    // ====================

    fun saveClient(
        original: ClientWithAddresses?,
        updatedClient: Client,
        updatedAddresses: List<Address>
    ) {
        viewModelScope.launch {

            val isEditMode = original != null

            if (isEditMode && !hasChanges(original, updatedClient, updatedAddresses)) {
                _events.emit(
                    CreateClientEvent.ShowMessage(
                        R.string.message_no_changes_detected
                    )
                )
                return@launch
            }

            _uiState.value = CreateClientUiState.Loading

            try {
                if (isEditMode) {
                    updateClient(updatedClient, updatedAddresses)
                    _events.emit(CreateClientEvent.Updated)
                } else {
                    insertClient(updatedClient, updatedAddresses)
                    _events.emit(CreateClientEvent.Created)
                }
            } catch (e: Exception) {
                _uiState.value = CreateClientUiState.Error(
                    e.message ?: "Error saving client"
                )
                _events.emit(
                    CreateClientEvent.ShowMessage(
                        R.string.message_error_save
                    )
                )
            }
        }
    }


    // Verifica si hubo cambios entre los datos originales y los nuevos.
    private fun hasChanges(
        original: ClientWithAddresses,
        updatedClient: Client,
        updatedAddresses: List<Address>
    ): Boolean {
        return original.client != updatedClient ||
                original.addresses != updatedAddresses
    }
}