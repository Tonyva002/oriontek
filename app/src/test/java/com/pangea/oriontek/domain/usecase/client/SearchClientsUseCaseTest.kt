package com.pangea.oriontek.domain.usecase.client

import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.domain.model.ClientWithAddresses
import com.pangea.oriontek.domain.repository.ClientRepository
import com.pangea.oriontek.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchClientsUseCaseTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val repository: ClientRepository = mockk(relaxed = false)
    private val useCase = SearchClientsUseCase(repository)

    @Test
    fun `invoke should call repository with query and return matching clients`() = runTest {

        val searchQuery = "Tony"
        val expectedResults = listOf(
            ClientWithAddresses(Client(id = 1, name = "Tony Vasquez"), emptyList())
        )

        coEvery { repository.searchClients(searchQuery) } returns flowOf(expectedResults)

        val result = useCase(searchQuery).first()

        assertEquals(expectedResults, result)
    }

    @Test
    fun `invoke should return empty list when no clients match query`() = runTest {

        val searchQuery = "NonExistent"

        coEvery { repository.searchClients(searchQuery) } returns flowOf(emptyList())

        val result = useCase(searchQuery).first()

        assertEquals(0, result.size)
    }

    @Test
    fun `invoke should throw exception when repository fails`() = runTest {

        val searchQuery = "Tony"

        coEvery { repository.searchClients(searchQuery) } throws RuntimeException("DB error")

        try {
            useCase(searchQuery).first()
            assert(false)
        } catch (e: Exception) {
            assert(e is RuntimeException)
        }
    }
}