package com.pangea.oriontek.domain.model

data class Client(
    val id: Long = 0,
    val name: String = "",
    val lastName: String = "",
    val company: String = "",
    val email: String = "",
    val phone: String = "",
    val photoResId: Int = 0,

    )