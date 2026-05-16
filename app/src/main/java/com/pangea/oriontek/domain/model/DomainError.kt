package com.pangea.oriontek.domain.model

sealed class DomainError : Exception() {

    class NotFound : DomainError()

    class Network : DomainError()

    class Server : DomainError()

    class Unknown : DomainError()

}