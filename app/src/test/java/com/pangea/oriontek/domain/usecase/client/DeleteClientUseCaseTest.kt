package com.pangea.oriontek.domain.usecase.client

import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.domain.repository.ClientRepository
import com.pangea.oriontek.utils.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteClientUseCaseTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule() // ¡Mantenla aquí!

    @Test
    fun `should call deleteClient in repository when invoke is called`() = runTest {
        // Arrange
        val repository = mockk<ClientRepository>(relaxed = true)
        val useCase = DeleteClientUseCase(repository)
        val client = Client(id = 1, name = "Tony")

        // Act
        useCase(client)

        // Assert
        coVerify(exactly = 1) { repository.deleteClient(client) }
    }
}