package com.pangea.oriontek.ui.fragments

import com.pangea.oriontek.domain.model.Address
import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.domain.model.ClientWithAddresses
import com.pangea.oriontek.domain.usecase.client.GetClientByIdUseCase
import com.pangea.oriontek.domain.usecase.client.InsertClientWithAddressesUseCase
import com.pangea.oriontek.domain.usecase.client.UpdateClientWithAddressesUseCase
import com.pangea.oriontek.ui.fragments.states.*
import com.pangea.oriontek.utils.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class CreateClientViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val insertClient: InsertClientWithAddressesUseCase = mockk(relaxed = true)
    private val updateClient: UpdateClientWithAddressesUseCase = mockk(relaxed = true)
    private val getClientById: GetClientByIdUseCase = mockk()

    private lateinit var viewModel: CreateClientViewModel

    @Before
    fun setup() {
        viewModel = CreateClientViewModel(
            insertClient,
            updateClient,
            getClientById
        )
    }

    // -------------------------
    // LOAD CLIENT
    // -------------------------
    @Test
    fun `loadClient - success`() = runTest {
        val client = ClientWithAddresses(
            client = Client(id = 1, name = "Tony", lastName = "Test"),
            addresses = emptyList()
        )

        coEvery { getClientById(1) } returns flowOf(client)

        viewModel.loadClient(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assert(state is CreateClientUiState.Form)
    }

    @Test
    fun `loadClient - not found`() = runTest {
        coEvery { getClientById(1) } returns flowOf(null)

        viewModel.loadClient(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assert(state is CreateClientUiState.Error)
    }

    // -------------------------
    // CREATE CLIENT
    // -------------------------
    @Test
    fun `saveClient - create new client`() = runTest {

        val events = mutableListOf<CreateClientEvent>()
        val job = launch { viewModel.events.toList(events) }

        viewModel.saveClient(
            name = "Tony",
            lastName = "Test",
            email = "test@mail.com",
            company = "Orion",
            phone = "123",
            address1 = "Address 1",
            address2 = "",
            photoResId = 1
        )

        advanceUntilIdle()

        coVerify { insertClient(any(), any()) }
        assert(events.any { it is CreateClientEvent.Created })

        job.cancel()
    }

    // -------------------------
    // UPDATE CLIENT
    // -------------------------
    @Test
    fun `saveClient - update existing client`() = runTest {
        // 1. Definimos el cliente "antiguo" que ya está en el sistema
        val originalClient = Client(
            id = 1,
            name = "Tony",
            lastName = "Old",
            email = "old@mail.com",
            company = "Orion",
            phone = "123",
            photoResId = 1
        )

        val formState = CreateClientFormState(
            client = originalClient,
            addresses = listOf(Address(10, "Calle Antigua", 1)),
            isEditMode = true
        )

        // Seteamos el estado inicial manualmente (usando tu metodo de reflexión)
        viewModel.apply {
            val field = this::class.java.getDeclaredField("_uiState")
            field.isAccessible = true
            val stateFlow = field.get(this) as MutableStateFlow<CreateClientUiState>
            stateFlow.value = CreateClientUiState.Form(formState)
        }

        val events = mutableListOf<CreateClientEvent>()
        val job = launch { viewModel.events.toList(events) }

        // 2. Ejecutamos el guardado con cambios CLAROS
        viewModel.saveClient(
            name = "Tony",           // Igual
            lastName = "Old",        // Igual
            email = "nuevo@mail.com", // CAMBIO
            company = "Orion",       // Igual
            phone = "123",           // Igual
            address1 = "Calle Nueva", // CAMBIO
            address2 = "",
            photoResId = 1           // Igual
        )

        advanceUntilIdle()

        coVerify(exactly = 1) { updateClient(any(), any()) }
        assert(events.any { it is CreateClientEvent.Updated })

        job.cancel()
    }

    // -------------------------
    // NO CHANGES
    // -------------------------
    @Test
    fun `saveClient - no changes detected`() = runTest {

        val client = Client(
            id = 1,
            name = "Tony",
            lastName = "Test",
            email = "test@mail.com",
            company = "Orion",
            phone = "123"
        )

        val formState = CreateClientFormState(
            client = client,
            addresses = listOf(Address(0, "Address 1", 1)),
            isEditMode = true
        )

        // 👇 Setear estado manual
        viewModel.apply {
            val field = this::class.java.getDeclaredField("_uiState")
            field.isAccessible = true
            val stateFlow = field.get(this) as MutableStateFlow<CreateClientUiState>
            stateFlow.value = CreateClientUiState.Form(formState)
        }

        val events = mutableListOf<CreateClientEvent>()
        val job = launch { viewModel.events.toList(events) }

        viewModel.saveClient(
            name = "Tony",
            lastName = "Test",
            email = "test@mail.com",
            company = "Orion",
            phone = "123",
            address1 = "Address 1",
            address2 = "",
            photoResId = 0
        )

        advanceUntilIdle()

        assert(events.any { it is CreateClientEvent.ShowMessage })

        job.cancel()
    }

    // -------------------------
    // ERROR CASE
    // -------------------------
    @Test
    fun `saveClient - error while saving`() = runTest {

        coEvery { insertClient(any(), any()) } throws RuntimeException("DB error")

        viewModel.saveClient(
            name = "Tony",
            lastName = "Test",
            email = "mail@test.com",
            company = "Orion",
            phone = "123",
            address1 = "Address",
            address2 = "",
            photoResId = 0
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assert(state is CreateClientUiState.Error)
    }
}