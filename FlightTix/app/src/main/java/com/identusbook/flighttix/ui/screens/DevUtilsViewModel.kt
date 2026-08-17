package com.identusbook.flighttix.ui.screens

import androidx.lifecycle.ViewModel
import com.identusbook.flighttix.agent.Identus
import com.identusbook.flighttix.agent.IdentusConfig
import com.identusbook.flighttix.model.Flight
import com.identusbook.flighttix.model.Passport
import com.identusbook.flighttix.net.CreateCredentialOfferRequest
import com.identusbook.flighttix.net.CreateTicketCredentialOfferRequest
import com.identusbook.flighttix.net.PassportClaimsRequest
import com.identusbook.flighttix.net.TicketClaimsRequest
import com.identusbook.flighttix.util.DateUtils

/** Developer tools — port of iOS `DevUtilsModel.swift`. */
class DevUtilsViewModel : ViewModel() {

    suspend fun tearDown() = Identus.tearDown()
    suspend fun startUp() = Identus.startUpAndConnect()
    suspend fun stop() = Identus.stop()

    suspend fun issuePassport(passport: Passport) {
        val issuerDID = Identus.readIssuerDIDFromKeychain()
            ?: throw IllegalStateException("Issuer DID not in keychain")
        val shortForm = Identus.didShortForm(issuerDID)
            ?: throw IllegalStateException("Could not get issuer short form DID")
        if (!Identus.verifyIssuerDIDIsPublished(shortForm)) {
            throw IllegalStateException("Issuer DID is not published")
        }
        val passportSchemaId = Identus.readPassportSchemaIdFromKeychain()
            ?: throw IllegalStateException("Passport schema id not in keychain")
        val connectionId = Identus.readConnectionIdFromKeychain()
            ?: throw IllegalStateException("Connection id not in keychain")

        val request = CreateCredentialOfferRequest(
            validityPeriod = 3600,
            schemaId = "${IdentusConfig.cloudAgentBaseUrl}/schema-registry/schemas/$passportSchemaId/schema",
            credentialFormat = "JWT",
            claims = PassportClaimsRequest(
                name = passport.name,
                dateOfIssuance = DateUtils.iso8601String(),
                passportNumber = passport.passportNumber,
                dob = DateUtils.iso8601String(passport.dob)
            ),
            automaticIssuance = true,
            issuingDID = shortForm,
            connectionId = connectionId
        )
        val offer = Identus.createPassportCredentialOffer(request)
        Identus.storePassportVCThidInKeychain(offer.thid)
    }

    suspend fun issueTicket(flight: Flight) {
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
        Identus.storeTicketVCThidInKeychain(offer.thid)
    }

    suspend fun requestProofOfPassport() {
        val passportSchemaId = Identus.readPassportSchemaIdFromKeychain()
            ?: throw IllegalStateException("Passport schema id not in keychain")
        Identus.createProofRequest(passportSchemaId)
    }

    suspend fun requestProofOfTicket() {
        val ticketSchemaId = Identus.readTicketSchemaIdFromKeychain()
            ?: throw IllegalStateException("Ticket schema id not in keychain")
        Identus.createProofRequest(ticketSchemaId)
    }
}
