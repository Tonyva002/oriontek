package com.pangea.oriontek.ui.fragments

import com.pangea.oriontek.R
import com.pangea.oriontek.domain.model.Address
import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.domain.model.ClientWithAddresses
import com.pangea.oriontek.domain.model.DomainError
import com.pangea.oriontek.domain.usecase.client.GetClientByIdUseCase
import com.pangea.oriontek.domain.usecase.client.InsertClientWithAddressesUseCase
import com.pangea.oriontek.domain.usecase.client.UpdateClientWithAddressesUseCase
import com.pangea.oriontek.ui.fragments.states.CreateClientEvent
import com.pangea.oriontek.ui.fragments.states.CreateClientFormState
import com.pangea.oriontek.ui.fragments.states.CreateClientUiState
import com.pangea.oriontek.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateClientViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val insertClient: InsertClientWithAddressesUseCase =
        mockk(relaxed = true)

    private val updateClient: UpdateClientWithAddressesUseCase =
        mockk(relaxed = true)

    private val getClientById: GetClientByIdUseCase =
        mockk()

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
            client = Client(
                id = 1,
                name = "Tony",
                lastname = "Test"
            ),
            addresses = emptyList()
        )

        every {
            getClientById(1)
        } returns flowOf(client)

        viewModel.loadClient(1)

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertTrue(state is CreateClientUiState.Form)
    }

    @Test
    fun `loadClient - not found`() = runTest {

        every {
            getClientById(1)
        } returns flowOf(null)

        viewModel.loadClient(1)

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertTrue(state is CreateClientUiState.Error)
    }

    // -------------------------
    // CREATE CLIENT
    // -------------------------

    @Test
    fun `saveClient - create new client`() = runTest {

        val events = mutableListOf<CreateClientEvent>()

        val job = launch {
            viewModel.events.toList(events)
        }

        viewModel.updateClientField {
            copy(
                name = "Tony",
                lastname = "Test",
                email = "test@mail.com",
                company = "Orion",
                phone = "123",
                photoUri = "content://image"
            )
        }

        viewModel.updateAddress(0, "Address 1")

        viewModel.saveClient()

        advanceUntilIdle()

        coVerify(exactly = 1) {
            insertClient(any(), any())
        }

        assertTrue(
            events.any { it is CreateClientEvent.Created }
        )

        job.cancel()
    }

    // -------------------------
    // UPDATE CLIENT
    // -------------------------

    @Test
    fun `saveClient - update existing client`() = runTest {

        val originalClient = Client(
            id = 1,
            name = "Tony",
            lastname = "Old",
            email = "old@mail.com",
            company = "Orion",
            phone = "123",
            photoUri = "content://image"
        )

        val formState = CreateClientFormState(
            client = originalClient,
            addresses = listOf(
                Address(
                    id = 10,
                    fullAddress = "Calle Antigua",
                    clientId = 1
                )
            ),
            isEditMode = true
        )

        viewModel.apply {

            val field =
                this::class.java.getDeclaredField("_uiState")

            field.isAccessible = true

            @Suppress("UNCHECKED_CAST")
            val stateFlow =
                field.get(this)
                        as MutableStateFlow<CreateClientUiState>

            stateFlow.value =
                CreateClientUiState.Form(formState)
        }

        val events = mutableListOf<CreateClientEvent>()

        val job = launch {
            viewModel.events.toList(events)
        }

        viewModel.updateClientField {
            copy(
                email = "nuevo@mail.com"
            )
        }

        viewModel.updateAddress(0, "Calle Nueva")

        viewModel.saveClient()

        advanceUntilIdle()

        coVerify(exactly = 1) {
            updateClient(any(), any())
        }

        assertTrue(
            events.any { it is CreateClientEvent.Updated }
        )

        job.cancel()
    }

    // -------------------------
    // VALIDATION IMAGE
    // -------------------------

    @Test
    fun `saveClient - image required`() = runTest {

        val events = mutableListOf<CreateClientEvent>()

        val job = launch {
            viewModel.events.toList(events)
        }

        viewModel.updateClientField {
            copy(
                name = "Tony",
                lastname = "Test",
                email = "test@mail.com",
                company = "Orion",
                phone = "123",
                photoUri = ""
            )
        }

        viewModel.updateAddress(0, "Address 1")

        viewModel.saveClient()

        advanceUntilIdle()

        assertTrue(
            events.any {
                it is CreateClientEvent.ShowMessage &&
                        it.resId == R.string.message_select_image
            }
        )

        job.cancel()
    }

    // -------------------------
    // ERROR CASE
    // -------------------------

    @Test
    fun `saveClient - error while saving`() = runTest {

        coEvery {
            insertClient(any(), any())
        } throws DomainError.Unknown()

        viewModel.updateClientField {
            copy(
                name = "Tony",
                lastname = "Test",
                email = "mail@test.com",
                company = "Orion",
                phone = "123",
                photoUri = "content://image"
            )
        }

        viewModel.updateAddress(0, "Address")

        viewModel.saveClient()

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertTrue(state is CreateClientUiState.Error)
    }
}