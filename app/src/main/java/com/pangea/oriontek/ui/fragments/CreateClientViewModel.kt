package com.pangea.oriontek.ui.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pangea.oriontek.R
import com.pangea.oriontek.domain.model.Address
import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.domain.model.DomainError
import com.pangea.oriontek.domain.usecase.client.GetClientByIdUseCase
import com.pangea.oriontek.domain.usecase.client.InsertClientWithAddressesUseCase
import com.pangea.oriontek.domain.usecase.client.UpdateClientWithAddressesUseCase
import com.pangea.oriontek.ui.common.toUiMessageRes
import com.pangea.oriontek.ui.fragments.states.CreateClientEvent
import com.pangea.oriontek.ui.fragments.states.CreateClientFormState
import com.pangea.oriontek.ui.fragments.states.CreateClientUiState
import com.pangea.oriontek.ui.fragments.states.ValidationErrors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CreateClientEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    // Actualizar cualquier campo del Client
    fun updateClientField(transform: Client.() -> Client) {
        updateForm {
            copy(client = client.transform())
        }
    }

    // Actualiza una dirección dentro de la lista addresses del formulario.
    fun updateAddress(index: Int, value: String) {
        updateForm {
            val updated = addresses.toMutableList()

            while (updated.size <= index) {
                updated.add(
                    Address(
                        id = 0,
                        fullAddress = "",
                        clientId = client.id
                    )
                )
            }

            updated[index] = updated[index].copy(fullAddress = value)
            copy(addresses = updated)
        }
    }

    // Actualiza la foto del cliente.
    fun updatePhoto(uri: String) {
        updateClientField { copy(photoUri = uri) }
    }

    private fun updateForm(
        transform: CreateClientFormState.() -> CreateClientFormState
    ) {
        _uiState.update { current ->
            if (current is CreateClientUiState.Form) {
                current.copy(
                    data = current.data.transform(),
                    errors = ValidationErrors()
                )
            } else current
        }
    }

    // Carga un cliente desde la base de datos y actualiza el estado.
    fun loadClient(id: Long) {
        viewModelScope.launch {
            _uiState.value = CreateClientUiState.Loading

            try {

                val result = getClientById(id).firstOrNull()

                if (result != null) {
                    _uiState.value = CreateClientUiState.Form(
                        data = CreateClientFormState(
                            client = result.client,
                            addresses = result.addresses,
                            isEditMode = true
                        )
                    )

                } else {

                    _uiState.value = CreateClientUiState.Error(R.string.message_client_not_found)
                }
            } catch (e: DomainError) {
                _uiState.value = CreateClientUiState.Error(e.toUiMessageRes())
            }
        }
    }

    // Guardar cliente
    fun saveClient() {
        viewModelScope.launch {
            val currentFormState = _uiState.value as? CreateClientUiState.Form ?: return@launch
            val form = currentFormState.data

            val addr1 = form.addresses.getOrNull(0)?.fullAddress.orEmpty()
            val addr2 = form.addresses.getOrNull(1)?.fullAddress.orEmpty()

            val errors = validate(client = form.client, address1 = addr1)

            if (errors.hasErrors()) {
                _uiState.update { current ->
                    (current as? CreateClientUiState.Form)?.copy(errors = errors) ?: current
                }
                if (errors.image != null) {
                    _events.emit(CreateClientEvent.ShowMessage(R.string.message_select_image))
                }
                return@launch
            }

            _uiState.value = CreateClientUiState.Loading

            try {

                val finalAddresses = buildAddresses(
                    clientId = form.client.id,
                    currentAddresses = form.addresses,
                    addr1,
                    addr2
                )

                if (form.isEditMode) {
                    updateClient(form.client, finalAddresses)
                    _events.emit(CreateClientEvent.Updated)
                } else {
                    insertClient(form.client, finalAddresses)
                    _events.emit(CreateClientEvent.Created)
                }
            } catch (e: DomainError) {
                _uiState.value = CreateClientUiState.Error(e.toUiMessageRes())
            }
        }
    }

    private fun validate(client: Client, address1: String): ValidationErrors {
        return ValidationErrors(
            image = if (client.photoUri.isBlank()) R.string.required else null,
            name = if (client.name.isBlank()) R.string.required else null,
            lastname = if (client.lastname.isBlank()) R.string.required else null,
            company = if (client.company.isBlank()) R.string.required else null,
            email = if (client.email.isBlank()) R.string.required else null,
            phone = if (client.phone.isBlank()) R.string.required else null,
            address1 = if (address1.isBlank()) R.string.required else null
        )
    }

    private fun buildAddresses(
        clientId: Long,
        currentAddresses: List<Address>,
        vararg rawAddresses: String
    ): List<Address> {
        return rawAddresses
            .mapIndexed { index, text ->
                if (text.isNotBlank()) {
                    val existingId = currentAddresses.getOrNull(index)?.id ?: 0L
                    Address(
                        id = existingId,
                        fullAddress = text.trim(),
                        clientId = clientId
                    )
                } else null
            }
            .filterNotNull()

    }
}