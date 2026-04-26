package com.pangea.oriontek.data.repository

import com.pangea.oriontek.data.local.dao.ClientDao
import com.pangea.oriontek.data.mapper.toDomain
import com.pangea.oriontek.data.mapper.toEntity
import com.pangea.oriontek.domain.model.Address
import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.domain.model.ClientWithAddresses
import com.pangea.oriontek.domain.repository.ClientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ClientRepositoryImpl @Inject constructor(
    private val clientDao: ClientDao
) : ClientRepository {

    override fun getClients(): Flow<List<ClientWithAddresses>> {
        return clientDao.getClientsWithAddresses().map { list ->
            list.map { relation ->
                ClientWithAddresses(
                    client = relation.client.toDomain(),
                    addresses = relation.addresses.map { it.toDomain() }
                )
            }
        }
    }

    override fun getClientById(clientId: Long): Flow<ClientWithAddresses?> {
        return clientDao.getClientWithAddressesById(clientId).map { relation ->
            relation?.let {
                ClientWithAddresses(
                    client = it.client.toDomain(),
                    addresses = it.addresses.map { addr -> addr.toDomain() }
                )
            }
        }
    }

    override fun searchClients(query: String): Flow<List<ClientWithAddresses>> {
        return clientDao.searchClients(query).map { list ->
            list.map { relation ->
                ClientWithAddresses(
                    client = relation.client.toDomain(),
                    addresses = relation.addresses.map { it.toDomain() }
                )
            }
        }
    }

    override suspend fun insertClientWithAddresses(
        client: Client,
        addresses: List<Address>
    ) {
        clientDao.insertClientWithAddresses(
            client.toEntity(),
            addresses.map { it.toEntity() }
        )
    }

    override suspend fun updateClientWithAddresses(
        client: Client,
        addresses: List<Address>
    ) {
        clientDao.updateClientWithAddresses(
            client.toEntity(),
            addresses.map { it.toEntity() }
        )
    }


    override suspend fun deleteClient(client: Client) {
        clientDao.deleteClient(client.toEntity())
    }
}