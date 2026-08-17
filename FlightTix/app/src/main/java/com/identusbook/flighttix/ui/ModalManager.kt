package com.identusbook.flighttix.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The single active modal — port of iOS `ModalManager.swift`. Only one at a time. */
enum class ActiveModal { REGISTER, PROFILE }

class ModalManager {
    var activeModal by mutableStateOf<ActiveModal?>(null)
        private set

    fun show(modal: ActiveModal) {
        activeModal = modal
    }

    fun dismiss() {
        activeModal = null
    }
}
