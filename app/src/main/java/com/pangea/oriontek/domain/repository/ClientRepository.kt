package com.pangea.oriontek.domain.repository

import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.domain.model.Address
import com.pangea.oriontek.domain.model.ClientWithAddresses
import kotlinx.coroutines.flow.Flow

interface ClientRepository {

    fun getClients(): Flow<List<ClientWithAddresses>>

    fun getClientById(clientId: Long): Flow<ClientWithAddresses?>

    fun searchClients(query: String): Flow<List<ClientWithAddresses>>

    suspend fun insertClientWithAddresses(
        client: Client,
        addresses: List<Address>
    )

    suspend fun updateClientWithAddresses(
        client: Client,
        addresses: List<Address>
    )


    suspend fun deleteClient(client: Client)
}