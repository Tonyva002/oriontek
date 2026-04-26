package com.pangea.oriontek.data.local.dao


import androidx.room.*
import com.pangea.oriontek.data.local.entity.AddressEntity
import com.pangea.oriontek.data.local.entity.ClientEntity
import com.pangea.oriontek.data.local.relation.ClientWithAddresses
import kotlinx.coroutines.flow.Flow


@Dao
interface ClientDao {

    // ====================
    // INSERT
    // ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddresses(addresses: List<AddressEntity>)


    // ====================
    // UPDATE
    // ====================

    @Update
    suspend fun updateClient(client: ClientEntity)


    // ====================
    // DELETE
    // ====================

    @Query("DELETE FROM addresses WHERE clientId = :clientId")
    suspend fun deleteAddressesByClientId(clientId: Long)

    // Eliminar cliente (borra direcciones por CASCADE)
    @Delete
    suspend fun deleteClient(client: ClientEntity)


    // ====================
    // QUERIES
    // ====================

    // Obtener todos los clientes con sus direcciones
    @Transaction
    @Query("SELECT * FROM clients")
    fun getClientsWithAddresses(): Flow<List<ClientWithAddresses>>

    // Obtener un cliente por ID con direcciones
    @Transaction
    @Query("SELECT * FROM clients WHERE id = :clientId")
    fun getClientWithAddressesById(clientId: Long): Flow<ClientWithAddresses?>

    // Buscar por nombre, apellido o compañia
    @Transaction
    @Query(
        """
    SELECT * FROM clients
    WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%'
    OR LOWER(lastName) LIKE '%' || LOWER(:query) || '%'
    OR LOWER(company) LIKE '%' || LOWER(:query) || '%'
"""
    )
    fun searchClients(query: String): Flow<List<ClientWithAddresses>>


    // ====================
    // TRANSACTIONS
    // ====================

    // Insertar (cliente + direcciones)
    @Transaction
    suspend fun insertClientWithAddresses(
        client: ClientEntity,
        addresses: List<AddressEntity>
    ) {

        val clientId = insertClient(client)

        val addressesWithClientId = addresses.map {
            it.copy(clientId = clientId)
        }

        insertAddresses(addressesWithClientId)
    }


    // Actualiza (cliente + direcciones)
    @Transaction
    suspend fun updateClientWithAddresses(
        client: ClientEntity,
        addresses: List<AddressEntity>
    ) {
        // 1. Actualizar cliente
        updateClient(client)

        // 2. Eliminar direcciones anteriores
        deleteAddressesByClientId(client.id)

        // 3. Insertar nuevas direcciones
        val updatedAddresses = addresses.map {
            it.copy(clientId = client.id)
        }

        insertAddresses(updatedAddresses)
    }
}