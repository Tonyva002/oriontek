package com.pangea.oriontek.domain.usecase.client

import com.pangea.oriontek.domain.model.ClientWithAddresses
import com.pangea.oriontek.domain.repository.ClientRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetClientByIdUseCase @Inject constructor(
    private val repository: ClientRepository
) {
    operator fun invoke(clientId: Long): Flow<ClientWithAddresses?> {
        return repository.getClientById(clientId)
    }
}