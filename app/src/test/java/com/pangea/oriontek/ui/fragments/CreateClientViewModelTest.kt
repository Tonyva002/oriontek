package com.pangea.oriontek.ui.fragments

import com.pangea.oriontek.domain.model.Address
import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.domain.model.ClientWithAddresses
import com.pangea.oriontek.domain.usecase.client.GetClientByIdUseCase
import com.pangea.oriontek.domain.usecase.client.InsertClientWithAddressesUseCase
import com.pangea.oriontek.domain.usecase.client.UpdateClientWithAddressesUseCase
import com.pangea.oriontek.ui.fragments.states.CreateClientEvent
import com.pangea.oriontek.ui.fragments.states.CreateClientUiState
import com.pangea.oriontek.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateClientViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val insertClient: InsertClientWithAddressesUseCase = mockk()
    private val updateClient: UpdateClientWithAddressesUseCase = mockk()
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

    // LOAD CLIENT
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

        assert(state is CreateClientUiState.Success)
    }

    @Test
    fun `loadClient - not found`() = runTest {
        coEvery { getClientById(1) } returns flowOf(null)

        viewModel.loadClient(1)

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assert(state is CreateClientUiState.Error)
    }


    // SAVE CLIENT - CREATE
    @Test
    fun `saveClient - create new client`() = runTest {
        val client = Client(id = 0, name = "Tony", lastName = "Test")
        val addresses = emptyList<Address>()
        coEvery { insertClient(client, addresses) } returns Unit

        val eventList = mutableListOf<CreateClientEvent>()
        val job = launch { viewModel.events.toList(eventList) }

        viewModel.saveClient(null, client, addresses)
        advanceUntilIdle()

        coVerify(exactly = 1) { insertClient(client, addresses) }
        assert(eventList.any { it is CreateClientEvent.Created })

        job.cancel()
    }

    // SAVE CLIENT - UPDATE
    @Test
    fun `saveClient - update existing client`() = runTest {
        val original = ClientWithAddresses(
            client = Client(id = 1, name = "Tony", lastName = "Old"),
            addresses = emptyList()
        )

        val updatedClient = original.client.copy(name = "Tony Updated")

        coEvery { updateClient(updatedClient, emptyList()) } returns Unit

        viewModel.saveClient(original, updatedClient, emptyList())

        val event = viewModel.events.first()

        coVerify { updateClient(updatedClient, emptyList()) }
        assert(event is CreateClientEvent.Updated)
    }


    // NO CHANGES
    @Test
    fun `saveClient - no changes detected`() = runTest {
        val client = Client(id = 1, name = "Tony", lastName = "Test")

        val original = ClientWithAddresses(
            client = client,
            addresses = emptyList()
        )

        viewModel.saveClient(original, client, emptyList())

        val event = viewModel.events.first()

        assert(event is CreateClientEvent.ShowMessage)
    }


    // ERROR CASE
    @Test
    fun `saveClient - error while saving`() = runTest {
        val client = Client(id = 0, name = "Tony", lastName = "Test")

        coEvery { insertClient(client, emptyList()) } throws RuntimeException("DB error")

        viewModel.saveClient(null, client, emptyList())

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assert(state is CreateClientUiState.Error)
    }
}