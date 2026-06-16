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
    fun updateClientField(transform: Client.() -> Client) {  // Recibe una función que transforma un Client.
        updateForm {
            copy(client = client.transform())    // Toma el cliente actual, ejecuta la tramsformacion y lo reemplaza
        }
    }

    // Actualiza una dirección específica.
    fun updateAddress(index: Int, value: String) {
        updateForm {
            val updated = addresses.toMutableList() // Convierte lista inmutable → mutable.

            while (updated.size <= index) { // Si no existe esa posición: crea direcciones vacías
                updated.add(
                    Address(
                        id = 0,
                        fullAddress = "",
                        clientId = client.id
                    )
                )
            }

            updated[index] = updated[index].copy(fullAddress = value) // Actualiza solo el texto (fullAddress).
            copy(addresses = updated)   // Crea nuevo estado del formulario.
        }
    }

    // Actualiza la foto del cliente.
    fun updatePhoto(uri: String) {
        updateClientField { copy(photoUri = uri) }  // Reutiliza updateClientField
    }

    // Agrega un campo de dirección vacío
    fun addAddressField() {
        updateForm {
            val updated = addresses.toMutableList()
            // Usamos un ID negativo temporal para que DiffUtil lo reconozca como el mismo item al escribir
            val tempId = if (updated.isEmpty()) -1L else updated.minOf { it.id }.coerceAtMost(0L) - 1L
            updated.add(Address(id = tempId, fullAddress = "", clientId = client.id))
            copy(addresses = updated)
        }
    }

    fun resetForm() {
        _uiState.value = CreateClientUiState.Form(data = CreateClientFormState())
    }

    // Elimina una dirección por índice
    fun removeAddressField(index: Int) {
        updateForm {
            val updated = addresses.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            copy(addresses = updated)
        }
    }

    // Actualiza el formulario de manera genérica.
    private fun updateForm(
        transform: CreateClientFormState.() -> CreateClientFormState
    ) {
        _uiState.update { current ->       // Obtiene el estado actual.
            if (current is CreateClientUiState.Form) {  // Verifica que el estado sea Form
                current.copy(
                    data = current.data.transform(),    // Actualiza data
                    errors = ValidationErrors()       // Limpia errores
                )
            } else current
        }
    }

    // Carga un cliente desde la base de datos y actualiza el estado.
    fun loadClient(id: Long) {
        viewModelScope.launch {  // Inicia corrutina.
            _uiState.value = CreateClientUiState.Loading  // La UI muestra loading.

            try {

                val result = getClientById(id).firstOrNull()    // Busca cliente.

                if (result != null) {   // Si existe
                    _uiState.value = CreateClientUiState.Form( // Carga datos al formulario.
                        data = CreateClientFormState(
                            client = result.client,
                            addresses = result.addresses,
                            isEditMode = true              // Activa modo edición
                        )
                    )

                } else {

                    _uiState.value = CreateClientUiState.Error(R.string.message_client_not_found) // Si no existe
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

            // Validación
            val errors = validate(client = form.client, addresses = form.addresses)

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
                // Filtrar direcciones vacías y resetear IDs temporales a 0 para la DB
                val finalAddresses = form.addresses
                    .filter { it.fullAddress.isNotBlank() }
                    .map { 
                        it.copy(
                            id = if (it.id < 0) 0L else it.id,
                            clientId = form.client.id 
                        ) 
                    }

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


    // Valida campos requeridos.
    private fun validate(client: Client, addresses: List<Address>): ValidationErrors {
        val hasAtLeastOneAddress = addresses.any { it.fullAddress.isNotBlank() }
        return ValidationErrors(
            image = if (client.photoUri.isBlank()) R.string.required else null,
            name = if (client.name.isBlank()) R.string.required else null,
            lastname = if (client.lastname.isBlank()) R.string.required else null,
            company = if (client.company.isBlank()) R.string.required else null,
            email = if (client.email.isBlank()) R.string.required else null,
            phone = if (client.phone.isBlank()) R.string.required else null,
            address1 = if (!hasAtLeastOneAddress) R.string.required else null
        )
    }
}
