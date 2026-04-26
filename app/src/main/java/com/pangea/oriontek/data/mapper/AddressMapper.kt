package com.pangea.oriontek.data.mapper

import com.pangea.oriontek.data.local.entity.AddressEntity
import com.pangea.oriontek.domain.model.Address

fun AddressEntity.toDomain() = Address(
    id = id,
    fullAddress = fullAddress,
    clientId = clientId
)

fun Address.toEntity() = AddressEntity(
    id = id,
    fullAddress = fullAddress,
    clientId = clientId
)