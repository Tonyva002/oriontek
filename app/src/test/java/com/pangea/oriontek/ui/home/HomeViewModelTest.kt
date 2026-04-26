package com.pangea.oriontek.ui.home

import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.domain.model.ClientWithAddresses
import com.pangea.oriontek.domain.usecase.client.DeleteClientUseCase
import com.pangea.oriontek.domain.usecase.client.GetClientsUseCase
import com.pangea.oriontek.ui.home.states.HomeEvent
import com.pangea.oriontek.ui.home.states.HomeUiState
import com.pangea.oriontek.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val getClients: GetClientsUseCase = mockk()
    private val deleteClient: DeleteClientUseCase = mockk()

    @Test
    fun `should load clients successfully`() = runTest {
        val clients = listOf(
            ClientWithAddresses(client = Client(id = 1, name = "Tony"), addresses = emptyList())
        )
        coEvery { getClients() } returns flowOf(clients)

        val viewModel = HomeViewModel(getClients, deleteClient)
        viewModel.loadClients()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assert(state is HomeUiState.Success)
        assert((state as HomeUiState.Success).clients.size == 1)
        assert(state.clients[0].name == "Tony")
    }

    @Test
    fun `should emit error state and event when loading fails`() = runTest {
        coEvery { getClients() } returns flow { throw RuntimeException("Error") }

        // 1. PRIMERO instancia el ViewModel
        val viewModel = HomeViewModel(getClients, deleteClient)

        // 2. AHORA lanza la recolección de eventos
        val eventList = mutableListOf<HomeEvent>()
        val job = launch { viewModel.events.toList(eventList) }

        // 3. Ejecuta la acción
        viewModel.loadClients()
        advanceUntilIdle()

        // 4. Valida
        assert(viewModel.uiState.value is HomeUiState.Error)
        assert(eventList.any { it is HomeEvent.ShowMessage })

        job.cancel()
    }

    @Test
    fun `should delete client successfully`() = runTest {
        val client = Client(id = 1, name = "Tony")
        coEvery { getClients() } returns flowOf(emptyList())
        coEvery { deleteClient(client) } returns Unit

        val viewModel = HomeViewModel(getClients, deleteClient)
        viewModel.delete(client)
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteClient(client) }
    }

    @Test
    fun `should emit event when delete fails`() = runTest {
        val client = Client(id = 1, name = "Tony")
        coEvery { getClients() } returns flowOf(emptyList())
        coEvery { deleteClient(client) } throws RuntimeException()

        val viewModel = HomeViewModel(getClients, deleteClient)

        val eventList = mutableListOf<HomeEvent>()
        val job = launch { viewModel.events.toList(eventList) }

        viewModel.delete(client)
        advanceUntilIdle()

        assert(eventList.any { it is HomeEvent.ShowMessage })

        job.cancel()
    }
}