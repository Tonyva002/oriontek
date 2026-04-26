package com.pangea.oriontek.domain.model

data class ClientWithAddresses(
    val client: Client,
    val addresses: List<Address>
)
