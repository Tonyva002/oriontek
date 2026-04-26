package com.pangea.oriontek.ui.home.states

sealed class HomeEvent {
    data class ShowMessage(val resId: Int) : HomeEvent()
}