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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetClientsUseCaseTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val repository: ClientRepository = mockk()
    private val useCase = GetClientsUseCase(repository)

    @Test
    fun `invoke should return list of clients when repository has data`() = runTest {
        // Arrange
        val clientList = listOf(
            ClientWithAddresses(Client(id = 1, name = "Tony"), emptyList()),
            ClientWithAddresses(Client(id = 2, name = "Juan"), emptyList())
        )
        coEvery { repository.getClients() } returns flowOf(clientList)

        // Act
        val result = useCase().first()

        // Assert
        assertEquals(2, result.size)
        assertEquals("Tony", result[0].client.name)
    }

    @Test
    fun `invoke should return empty list when repository is empty`() = runTest {
        // Arrange
        coEvery { repository.getClients() } returns flowOf(emptyList())

        // Act
        val result = useCase().first()

        // Assert
        assertTrue(result.isEmpty())
    }
}