package com.pangea.oriontek.data.mapper

import com.pangea.oriontek.data.local.entity.ClientEntity
import com.pangea.oriontek.domain.model.Client

fun ClientEntity.toDomain() = Client(
    id = id,
    name = name,
    lastName = lastName,
    company = company,
    email = email,
    phone = phone,
    photoResId = photoResId

)

fun Client.toEntity() = ClientEntity(
    id = id,
    name = name,
    lastName = lastName,
    company = company,
    email = email,
    phone = phone,
    photoResId = photoResId
)
