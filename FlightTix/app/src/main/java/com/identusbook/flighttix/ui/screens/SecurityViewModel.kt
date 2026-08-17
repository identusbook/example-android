package com.identusbook.flighttix.ui.screens

import androidx.lifecycle.ViewModel
import com.identusbook.flighttix.agent.Identus
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Airport security verifier flow — port of iOS `SecurityViewModel.swift`. */
class SecurityViewModel : ViewModel() {

    sealed class RequestState {
        object Idle : RequestState()
        object Requesting : RequestState()
        object Awaiting : RequestState()
        data class Error(val message: String) : RequestState()
    }

    data class ProofReview(
        val ticketPresentationId: String,
        val passportPresentationId: String,
        val ticketValid: Boolean,
        val passportValid: Boolean,
        val ticketStatus: String,
        val passportStatus: String
    ) {
        val presentationIds: List<String> get() = listOf(ticketPresentationId, passportPresentationId)
        val allValid: Boolean get() = ticketValid && passportValid
    }

    data class PresentationRow(val presentationId: String, val status: String)

    private val _requestState = MutableStateFlow<RequestState>(RequestState.Idle)
    val requestState: StateFlow<RequestState> = _requestState.asStateFlow()

    private val _proofUnderReview = MutableStateFlow<ProofReview?>(null)
    val proofUnderReview: StateFlow<ProofReview?> = _proofUnderReview.asStateFlow()

    private val _presentations = MutableStateFlow<List<PresentationRow>>(emptyList())
    val presentations: StateFlow<List<PresentationRow>> = _presentations.asStateFlow()

    private val validStatus = "PresentationVerified"

    val isBusy: Boolean
        get() = _requestState.value is RequestState.Requesting || _requestState.value is RequestState.Awaiting

    suspend fun requestProofOfTicketAndPassport() {
        try {
            _requestState.value = RequestState.Requesting

            val ticketSchemaId = Identus.readTicketSchemaIdFromKeychain()
                ?: throw IllegalStateException("Ticket schema id unavailable")
            val passportSchemaId = Identus.readPassportSchemaIdFromKeychain()
                ?: throw IllegalStateException("Passport schema id unavailable")

            val ticketRequest = Identus.createProofRequest(ticketSchemaId)
                ?: throw IllegalStateException("Failed to create ticket proof request")
            val passportRequest = Identus.createProofRequest(passportSchemaId)
                ?: throw IllegalStateException("Failed to create passport proof request")

            _requestState.value = RequestState.Awaiting

            val (ticketOutcome, passportOutcome) = coroutineScope {
                val t = async { Identus.awaitPresentationOutcome(ticketRequest.presentationId) }
                val p = async { Identus.awaitPresentationOutcome(passportRequest.presentationId) }
                t.await() to p.await()
            }

            val ticketStatus = ticketOutcome?.status ?: "Unknown"
            val passportStatus = passportOutcome?.status ?: "Unknown"

            _proofUnderReview.value = ProofReview(
                ticketPresentationId = ticketRequest.presentationId,
                passportPresentationId = passportRequest.presentationId,
                ticketValid = ticketStatus == validStatus,
                passportValid = passportStatus == validStatus,
                ticketStatus = ticketStatus,
                passportStatus = passportStatus
            )
            _requestState.value = RequestState.Idle
            loadPresentations()
        } catch (e: Exception) {
            _requestState.value = RequestState.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun accept() = recordDecision(accept = true)

    suspend fun deny() = recordDecision(accept = false)

    private suspend fun recordDecision(accept: Boolean) {
        val review = _proofUnderReview.value ?: return
        for (presentationId in review.presentationIds) {
            if (accept) Identus.acceptPresentation(presentationId)
            else Identus.denyPresentation(presentationId)
        }
        _proofUnderReview.value = null
        loadPresentations()
    }

    suspend fun loadPresentations() {
        val response = Identus.getPresentations()
        _presentations.value = response.contents
            .filter { it.role == "Verifier" }
            .map { PresentationRow(it.presentationId, it.status) }
    }
}
