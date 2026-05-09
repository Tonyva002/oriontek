package com.pangea.oriontek.ui.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pangea.oriontek.domain.model.Address
import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.domain.usecase.client.GetClientByIdUseCase
import com.pangea.oriontek.domain.usecase.client.InsertClientWithAddressesUseCase
import com.pangea.oriontek.domain.usecase.client.UpdateClientWithAddressesUseCase
import com.pangea.oriontek.ui.fragments.states.CreateClientEvent
import com.pangea.oriontek.ui.fragments.states.CreateClientFormState
import com.pangea.oriontek.ui.fragments.states.CreateClientUiState
import com.pangea.oriontek.ui.fragments.states.ValidationErrors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateClientViewModel @Inject constructor(
    private val insertClient: InsertClientWithAddressesUseCase,
    private val updateClient: UpdateClientWithAddressesUseCase,
    private val getClientById: GetClientByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateClientUiState>(
        CreateClientUiState.Form(data = CreateClientFormState())
    )
    val uiState: StateFlow<CreateClientUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CreateClientEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    // --- UPDATE FORM ---

    fun updateClientField(transform: Client.() -> Client) {
        updateForm { copy(client = client.transform()) }
    }

    fun updateAddress(index: Int, value: String) {
        updateForm {
            val updated = addresses.toMutableList()
            while (updated.size <= index) {
                updated.add(Address(
                    id = 0,
                    fullAddress = "",
                    clientId = client.id))
            }
            updated[index] = updated[index].copy(fullAddress = value)
            copy(addresses = updated)
        }
    }

    fun updatePhoto(photoResId: Int) {
        updateClientField { copy(photoResId = photoResId) }
    }

    private fun updateForm(transform: CreateClientFormState.() -> CreateClientFormState) {
        _uiState.update { current ->
            if (current is CreateClientUiState.Form) {
                current.copy(data = current.data.transform(), errors = ValidationErrors())
            } else current
        }
    }

    // --- LOAD ---

    fun loadClient(id: Long) {
        viewModelScope.launch {
            _uiState.value = CreateClientUiState.Loading
            try {
                val result = getClientById(id).first()
                if (result != null) {
                    _uiState.value = CreateClientUiState.Form(
                        data = CreateClientFormState(
                            client = result.client,
                            addresses = result.addresses,
                            isEditMode = true
                        )
                    )
                } else {
                    _uiState.value = CreateClientUiState.Error("Client not found")
                }
            } catch (e: Exception) {
                _uiState.value = CreateClientUiState.Error(e.message ?: "Error loading client")
            }
        }
    }

    // --- SAVE ---

    fun saveClient() {
        viewModelScope.launch {
            val current = _uiState.value as? CreateClientUiState.Form ?: return@launch
            val form = current.data

            val addr1 = form.addresses.getOrNull(0)?.fullAddress.orEmpty()
            val addr2 = form.addresses.getOrNull(1)?.fullAddress.orEmpty()

            val errors = validate(form.client, addr1)
            if (errors.hasErrors()) {
                _uiState.update { (it as CreateClientUiState.Form).copy(errors = errors) }
                return@launch
            }

            _uiState.value = CreateClientUiState.Loading
            try {
                val finalAddresses = buildAddresses(form.client.id, addr1, addr2)
                if (form.isEditMode) {
                    updateClient(form.client, finalAddresses)
                    _events.emit(CreateClientEvent.Updated)
                } else {
                    insertClient(form.client, finalAddresses)
                    _events.emit(CreateClientEvent.Created)
                }
            } catch (e: Exception) {
                _uiState.value = CreateClientUiState.Error(e.message ?: "Error saving client")
            }
        }
    }

    private fun validate(client: Client, address1: String): ValidationErrors {
        return ValidationErrors(
            name = if (client.name.isBlank()) "Required" else null,
            lastName = if (client.lastName.isBlank()) "Required" else null,
            email = if (client.email.isBlank()) "Required" else null,
            company = if (client.company.isBlank()) "Required" else null,
            phone = if (client.phone.isBlank()) "Required" else null,
            address = if (address1.isBlank()) "Required" else null
        )
    }

    private fun buildAddresses(clientId: Long, vararg rawAddresses: String): List<Address> {
        return rawAddresses.filter { it.isNotBlank() }.map {
            Address(
                id = 0,
                fullAddress = it.trim(),
                clientId = clientId
            )
        }
    }
}