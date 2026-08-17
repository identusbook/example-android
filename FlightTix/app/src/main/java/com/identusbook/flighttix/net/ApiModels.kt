package com.identusbook.flighttix.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cloud Agent REST request/response models — ports of the iOS `Networking/APIModels` structs.
 * The JSON client uses ignoreUnknownKeys, so only fields the app reads/writes are declared.
 */

// ── Connections ───────────────────────────────────────────────────────────────

@Serializable
data class CreateInvitationRequest(val label: String)

@Serializable
data class InvitationAPIModel(
    val from: String? = null,
    val id: String? = null,
    val invitationUrl: String,
    val type: String? = null
)

@Serializable
data class CreateInvitationResponse(
    val connectionId: String,
    val label: String? = null,
    val invitation: InvitationAPIModel
)

@Serializable
data class ConnectionContentAPIModel(
    val connectionId: String,
    val label: String? = null,
    val state: String? = null,
    val role: String? = null
)

@Serializable
data class ConnectionResponse(
    val contents: List<ConnectionContentAPIModel> = emptyList()
)

// ── DIDs ──────────────────────────────────────────────────────────────────────

@Serializable
data class DIDPublicKey(val id: String, val purpose: String)

@Serializable
data class DocumentTemplate(
    val publicKeys: List<DIDPublicKey>,
    val services: List<String> = emptyList()
)

@Serializable
data class CreateDIDRequest(val documentTemplate: DocumentTemplate)

@Serializable
data class CreateDIDResponse(val longFormDid: String)

@Serializable
data class DIDContents(
    val did: String? = null,
    val status: String? = null
)

@Serializable
data class DIDsOnCloudAgentResponse(
    val contents: List<DIDContents> = emptyList()
)

@Serializable
data class DIDStatusResponse(
    val did: String,
    val status: String
)

@Serializable
data class PublishDIDRequest(val didRef: String)

@Serializable
data class ScheduledOperation(
    val id: String? = null,
    val didRef: String
)

@Serializable
data class PublishDIDResponse(val scheduledOperation: ScheduledOperation)

// ── Schemas ────────────────────────────────────────────────────────────────────

@Serializable
data class SchemaSummary(
    val guid: String,
    val name: String,
    val version: String? = null,
    val author: String? = null
)

@Serializable
data class SchemaSummaryPage(
    val contents: List<SchemaSummary> = emptyList()
)

// ── Credential claims ───────────────────────────────────────────────────────────

@Serializable
data class PassportClaimsRequest(
    val name: String,
    val dateOfIssuance: String,
    val passportNumber: String,
    val dob: String
)

@Serializable
data class TicketClaimsRequest(
    val name: String,
    val dateOfIssuance: String,
    val flightId: String,
    val price: Double,
    val departure: String,
    val arrival: String
)

// ── Credential offers ────────────────────────────────────────────────────────────

@Serializable
data class CreateCredentialOfferRequest(
    val validityPeriod: Int,
    val schemaId: String,
    val credentialFormat: String,
    val claims: PassportClaimsRequest,
    val automaticIssuance: Boolean,
    val issuingDID: String,
    val connectionId: String
)

@Serializable
data class CreateTicketCredentialOfferRequest(
    val validityPeriod: Int,
    val schemaId: String,
    val credentialFormat: String,
    val claims: TicketClaimsRequest,
    val automaticIssuance: Boolean,
    val issuingDID: String,
    val connectionId: String
)

@Serializable
data class CreateCredentialOfferResponse(
    val recordId: String,
    val thid: String
)

@Serializable
data class CredentialRecordStatus(
    val recordId: String,
    val protocolState: String
)

// ── Present proof ─────────────────────────────────────────────────────────────────

@Serializable
data class ProofRequestAux(
    val schemaId: String,
    val trustIssuers: List<String>? = null
)

@Serializable
data class ProofPresentationOptions(
    val challenge: String,
    val domain: String
)

@Serializable
data class CreateProofPresentationRequest(
    val connectionId: String,
    val options: ProofPresentationOptions,
    val proofs: List<ProofRequestAux>
)

@Serializable
data class PresentationResponseContent(
    val presentationId: String,
    val thid: String? = null,
    val role: String? = null,
    val status: String,
    val connectionId: String? = null
)

@Serializable
data class PresentationsResponse(
    val contents: List<PresentationResponseContent> = emptyList()
)

/**
 * Verifier accept/reject action — port of iOS VerifierPresentationActionRequest.
 * PATCH /present-proof/presentations/{id}
 */
@Serializable
data class VerifierPresentationActionRequest(val action: String) {
    companion object {
        val accept = VerifierPresentationActionRequest("presentation-accept")
        val reject = VerifierPresentationActionRequest("presentation-reject")
    }
}
