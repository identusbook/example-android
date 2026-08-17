package com.identusbook.flighttix.ui.screens

import androidx.lifecycle.ViewModel
import com.identusbook.flighttix.agent.Identus
import com.identusbook.flighttix.agent.IdentusConfig
import com.identusbook.flighttix.model.Flight
import com.identusbook.flighttix.net.CreateTicketCredentialOfferRequest
import com.identusbook.flighttix.net.TicketClaimsRequest
import com.identusbook.flighttix.ui.FlightSearch
import com.identusbook.flighttix.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Ticket purchase / issuance — port of iOS `PurchaseViewModel.swift`. */
class PurchaseViewModel : ViewModel() {

    private val _purchaseComplete = MutableStateFlow(false)
    val purchaseComplete: StateFlow<Boolean> = _purchaseComplete.asStateFlow()

    val availableFlights: List<Flight> = FlightSearch.availableFlights()

    fun clearPurchaseComplete() {
        _purchaseComplete.value = false
    }

    suspend fun purchaseTicket(flight: Flight) {
        Identus.createTicketSchemaIfNotExists()

        val issuerDID = Identus.readIssuerDIDFromKeychain()
            ?: throw IllegalStateException("Issuer DID not in keychain")
        val shortForm = Identus.didShortForm(issuerDID)
            ?: throw IllegalStateException("Could not get issuer short form DID")
        if (!Identus.verifyIssuerDIDIsPublished(shortForm)) {
            throw IllegalStateException("Issuer DID is not published")
        }
        val ticketSchemaId = Identus.readTicketSchemaIdFromKeychain()
            ?: throw IllegalStateException("Ticket schema id not in keychain")
        val connectionId = Identus.readConnectionIdFromKeychain()
            ?: throw IllegalStateException("Connection id not in keychain")

        val request = CreateTicketCredentialOfferRequest(
            validityPeriod = 3600,
            schemaId = "${IdentusConfig.cloudAgentBaseUrl}/schema-registry/schemas/$ticketSchemaId/schema",
            credentialFormat = "JWT",
            claims = TicketClaimsRequest(
                name = flight.id.toString(),
                dateOfIssuance = DateUtils.iso8601String(),
                flightId = flight.id.toString(),
                price = flight.price,
                departure = flight.departure,
                arrival = flight.arrival
            ),
            automaticIssuance = true,
            issuingDID = shortForm,
            connectionId = connectionId
        )
        val offer = Identus.createTicketCredentialOffer(request)
        if (!Identus.storeTicketVCThidInKeychain(offer.thid)) {
            throw IllegalStateException("Failed to store ticket thid")
        }
        awaitCredentialIssued(offer.recordId)
        _purchaseComplete.value = true
    }

    /** Poll the Cloud Agent until the ticket record reaches "CredentialSent" (or time out). */
    private suspend fun awaitCredentialIssued(recordId: String, timeoutSeconds: Int = 90) {
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000L
        while (System.currentTimeMillis() < deadline) {
            val record = Identus.credentialRecordStatus(recordId)
            if (record?.protocolState == "CredentialSent") return
            delay(2000)
        }
        throw IllegalStateException("Timed out waiting for ticket credential to be issued")
    }
}
