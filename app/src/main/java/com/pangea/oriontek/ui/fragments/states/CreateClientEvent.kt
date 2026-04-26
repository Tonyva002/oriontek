package com.pangea.oriontek.ui.fragments.states

sealed class CreateClientEvent {
    data class ShowMessage(val resId: Int) : CreateClientEvent()

    object Created : CreateClientEvent()

    object Updated : CreateClientEvent()
}