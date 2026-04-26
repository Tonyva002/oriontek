package com.pangea.oriontek.domain.usecase.client

import com.pangea.oriontek.domain.model.Address
import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.domain.repository.ClientRepository
import javax.inject.Inject

class InsertClientWithAddressesUseCase @Inject constructor(
    private val repository: ClientRepository
) {
    suspend operator fun invoke(
        client: Client,
        addresses: List<Address>
    ) {
        repository.insertClientWithAddresses(client, addresses)
    }
}