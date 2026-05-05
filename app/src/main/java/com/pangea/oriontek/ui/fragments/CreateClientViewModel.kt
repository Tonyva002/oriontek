package com.pangea.oriontek.ui.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pangea.oriontek.R
import com.pangea.oriontek.domain.model.Address
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateClientViewModel @Inject constructor(
    private val insertClient: InsertClientWithAddressesUseCase,
    private val updateClient: UpdateClientWithAddressesUseCase,
    private val getClientById: GetClientByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateClientUiState>(
        CreateClientUiState.Form(
            data = CreateClientFormState()
        )
    )
    val uiState: StateFlow<CreateClientUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CreateClientEvent>(
        extraBufferCapacity = 1
    )
    val events = _events.asSharedFlow()


    // -------------------------
    // LOAD
    // -------------------------
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
                    _uiState.value =
                        CreateClientUiState.Error("Client not found")
                }

            } catch (e: Exception) {
                _uiState.value =
                    CreateClientUiState.Error(
                        e.message ?: "Error loading client"
                    )
            }
        }
    }


    // -------------------------
    // SAVE
    // -------------------------
    fun saveClient(
        name: String,
        lastName: String,
        email: String,
        company: String,
        phone: String,
        address1: String,
        address2: String,
        photoResId: Int
    ) {
        viewModelScope.launch {

            val currentState = _uiState.value
            if (currentState !is CreateClientUiState.Form) return@launch

            val form = currentState.data

            // Valida los campos
            val errors = validate(name, lastName, email, company, phone, address1)

            if (errors.hasErrors()) {
                _uiState.value = currentState.copy(errors = errors)
                return@launch
            }

            val updatedClient = form.client.copy(
                name = name.trim(),
                lastName = lastName.trim(),
                email = email.trim(),
                company = company.trim(),
                phone = phone.trim(),
                photoResId = photoResId
            )

            val addresses = buildAddresses(
                clientId = updatedClient.id,
                address1 = address1,
                address2 = address2
            )

            //VALIDAR CAMBIOS
            if (form.isEditMode) {
                val hasAddressChanges =
                    form.addresses.map { it.fullAddress.trim() } !=
                            addresses.map { it.fullAddress.trim() }

                val hasChanges =
                    form.client != updatedClient ||
                            hasAddressChanges

                if (!hasChanges) {
                    _events.emit(
                        CreateClientEvent.ShowMessage(
                            R.string.message_no_changes_detected
                        )
                    )
                    return@launch
                }
            }

            _uiState.value = CreateClientUiState.Loading

            try {
                if (form.isEditMode) {
                    updateClient(updatedClient, addresses)
                    _events.emit(CreateClientEvent.Updated)
                } else {
                    insertClient(updatedClient, addresses)
                    _events.emit(CreateClientEvent.Created)
                }
            } catch (e: Exception) {
                _uiState.value =
                    CreateClientUiState.Error(
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



    // Valida los campos del formulario
    private fun validate(
        name: String,
        lastName: String,
        email: String,
        company: String,
        phone: String,
        address: String
    ): ValidationErrors {

        return ValidationErrors(
            name = if (name.isBlank()) "Required" else null,
            lastName = if (lastName.isBlank()) "Required" else null,
            email = if (email.isBlank()) "Required" else null,
            company = if (company.isBlank()) "Required" else null,
            phone = if (phone.isBlank()) "Required" else null,
            address = if (address.isBlank()) "Required" else null
        )
    }



    // Construye la lista de direcciones válidas a partir de los campos del formulario.
    private fun buildAddresses(
        clientId: Long,
        address1: String,
        address2: String
    ): List<Address> {
        return listOfNotNull(
            address1.takeIf { it.isNotBlank() }?.let {
                Address(0, it.trim(), clientId)
            },
            address2.takeIf { it.isNotBlank() }?.let {
                Address(0, it.trim(), clientId)
            }
        )
    }
}