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
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetClientByIdUseCaseTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val repository: ClientRepository = mockk()
    private val useCase = GetClientByIdUseCase(repository)

    @Test
    fun `invoke should return client when found`() = runTest {
        // Arrange
        val clientId = 1L
        val expectedClient = ClientWithAddresses(
            client = Client(id = clientId, name = "Tony"),
            addresses = emptyList()
        )
        coEvery { repository.getClientById(clientId) } returns flowOf(expectedClient)

        // Act
        val result = useCase(clientId).first()

        // Assert
        assertEquals(expectedClient, result)
    }

    @Test
    fun `invoke should return null when client not found`() = runTest {
        // Arrange
        val clientId = 99L
        coEvery { repository.getClientById(clientId) } returns flowOf(null)

        // Act
        val result = useCase(clientId).first()

        // Assert
        assertNull(result)
    }
}