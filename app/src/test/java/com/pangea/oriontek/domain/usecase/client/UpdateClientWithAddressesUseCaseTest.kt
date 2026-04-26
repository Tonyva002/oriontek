package com.pangea.oriontek.domain.usecase.client


import com.pangea.oriontek.domain.model.Address
import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.domain.repository.ClientRepository
import com.pangea.oriontek.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateClientWithAddressesUseCaseTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val repository: ClientRepository = mockk(relaxed = false)
    private val useCase = UpdateClientWithAddressesUseCase(repository)

    @Test
    fun `should call repository with correct client and addresses`() = runTest {

        val client = Client(id = 1, name = "Tony")
        val addresses = listOf(
            Address(id = 1, fullAddress = "Street 1", clientId = 1),
            Address(id = 2, fullAddress = "Street 2", clientId = 1)
        )

        coEvery {
            repository.updateClientWithAddresses(client, addresses)
        } returns Unit

        useCase(client, addresses)

        coVerify(exactly = 1) {
            repository.updateClientWithAddresses(client, addresses)
        }
    }
}