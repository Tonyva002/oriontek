package com.pangea.oriontek.domain.usecase.client

import com.pangea.oriontek.domain.model.ClientWithAddresses
import com.pangea.oriontek.domain.repository.ClientRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchClientsUseCase @Inject constructor(
    private val repository: ClientRepository
) {
    operator fun invoke(query: String): Flow<List<ClientWithAddresses>> {
        return repository.searchClients(query)
    }
}