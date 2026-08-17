package com.identusbook.flighttix.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.identusbook.flighttix.agent.Identus
import com.identusbook.flighttix.agent.IdentusConfig
import com.identusbook.flighttix.model.Passport
import com.identusbook.flighttix.net.CreateCredentialOfferRequest
import com.identusbook.flighttix.net.PassportClaimsRequest
import com.identusbook.flighttix.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Passport registration / issuance — port of iOS `RegisterViewModel.swift`. */
class RegisterViewModel : ViewModel() {

    private val _isIssuerReady = MutableStateFlow(false)
    val isIssuerReady: StateFlow<Boolean> = _isIssuerReady.asStateFlow()

    /** Poll until the issuer DID is published, then flip [isIssuerReady]. */
    fun confirmIssuerReady() {
        viewModelScope.launch {
            while (!isIssuerPublished()) {
                delay(2000)
            }
            _isIssuerReady.value = true
        }
    }

    private suspend fun isIssuerPublished(): Boolean {
        return try {
            val issuerDID = Identus.readIssuerDIDFromKeychain() ?: return false
            val shortForm = Identus.didShortForm(issuerDID) ?: return false
            Identus.verifyIssuerDIDIsPublished(shortForm)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun register(passport: Passport) {
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
        if (!Identus.storePassportVCThidInKeychain(offer.thid)) {
            throw IllegalStateException("Failed to store passport thid")
        }
    }

    suspend fun requestProof() {
        val passportSchemaId = Identus.readPassportSchemaIdFromKeychain()
            ?: throw IllegalStateException("Passport schema id not in keychain")
        Identus.createProofRequest(passportSchemaId)
    }
}
