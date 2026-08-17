package com.identusbook.flighttix.agent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Startup progress states published during bootstrap — mirrors iOS `Agent/IdentusStatus.swift`.
 * The Loading screen renders [description] for the current state.
 */
sealed class IdentusStatusState {
    object Disconnected : IdentusStatusState()
    object Connected : IdentusStatusState()
    object StartingAgent : IdentusStatusState()
    object StartingDIDCommMessageListener : IdentusStatusState()
    object CreatingConnectionToCloudAgent : IdentusStatusState()
    object IssuerDIDAlreadyExists : IdentusStatusState()
    object CreatingIssuerDID : IdentusStatusState()
    object PublishingIssuerDID : IdentusStatusState()
    object IssuerDIDPublished : IdentusStatusState()
    object CheckingPassportSchema : IdentusStatusState()
    object CreatingPassportSchema : IdentusStatusState()
    object CreatedPassportSchema : IdentusStatusState()
    object Ready : IdentusStatusState()
    data class Error(val message: String) : IdentusStatusState()

    val description: String
        get() = when (this) {
            Disconnected -> "Disconnected"
            Connected -> "Connected"
            StartingAgent -> "Starting Agent"
            StartingDIDCommMessageListener -> "Starting DIDComm Message Listener"
            CreatingConnectionToCloudAgent -> "Creating Connection to Cloud Agent"
            IssuerDIDAlreadyExists -> "Issuer DID Already Exists"
            CreatingIssuerDID -> "Creating New Issuer DID"
            PublishingIssuerDID -> "Publishing Issuer DID"
            IssuerDIDPublished -> "Issuer DID Published"
            CheckingPassportSchema -> "Checking Passport Schema"
            CreatingPassportSchema -> "Creating Passport Schema"
            CreatedPassportSchema -> "Created Passport Schema"
            Ready -> "Ready"
            is Error -> "Error: $message"
        }
}

/** Shared, observable status — the singleton equivalent of iOS `IdentusStatus.shared`. */
object IdentusStatus {
    private val _status = MutableStateFlow<IdentusStatusState>(IdentusStatusState.Disconnected)
    val status: StateFlow<IdentusStatusState> = _status.asStateFlow()

    fun set(state: IdentusStatusState) {
        _status.value = state
    }
}
