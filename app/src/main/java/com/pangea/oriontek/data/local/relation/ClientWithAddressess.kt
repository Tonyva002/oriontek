package com.pangea.oriontek.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.pangea.oriontek.data.local.entity.AddressEntity
import com.pangea.oriontek.data.local.entity.ClientEntity

data class ClientWithAddresses(
    @Embedded val client: ClientEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "clientId"
    )
    val addresses: List<AddressEntity>
)