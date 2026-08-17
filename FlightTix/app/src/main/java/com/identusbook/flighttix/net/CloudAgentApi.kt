package com.identusbook.flighttix.net

import com.identusbook.flighttix.agent.IdentusConfig
import com.identusbook.flighttix.model.PassportSchema
import com.identusbook.flighttix.model.TicketSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Cloud Agent REST client — port of iOS `Networking/API+CloudAgent.swift` + `APIClient.swift`.
 * Base URL, headers and 30s timeouts mirror `FlightTixURLSessionConfig.swift`.
 */
class CloudAgentApiException(val code: Int, val bodyText: String) :
    Exception("Cloud Agent HTTP $code: $bodyText")

object CloudAgentApi {

    private val baseUrl: String get() = IdentusConfig.cloudAgentBaseUrl.trimEnd('/')

    private val jsonMedia = "application/json".toMediaType()

    // ignoreUnknownKeys: tolerate the many response fields the app doesn't read.
    // explicitNulls=false: drop null optionals so bodies match iOS (e.g. `format: nil`, `guid: nil`).
    // Forward slashes are NOT escaped (matches Swift .withoutEscapingSlashes).
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── low-level helpers ──────────────────────────────────────────────────────

    private data class HttpResult(val code: Int, val body: String)

    private suspend fun execute(request: Request): HttpResult = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { resp ->
            HttpResult(resp.code, resp.body?.string() ?: "")
        }
    }

    private fun baseRequest(path: String): Request.Builder =
        Request.Builder()
            .url("$baseUrl$path")
            .header("Accept", "application/json")

    private fun <T> encode(serializer: KSerializer<T>, value: T) =
        json.encodeToString(serializer, value).toRequestBody(jsonMedia)

    private fun <T> HttpResult.decodeOrThrow(serializer: KSerializer<T>): T {
        if (code !in 200..299) throw CloudAgentApiException(code, body)
        return json.decodeFromString(serializer, body)
    }

    /** For GETs used in iOS `try?` contexts: null on non-2xx instead of throwing. */
    private fun <T> HttpResult.decodeOrNull(serializer: KSerializer<T>): T? {
        if (code !in 200..299) return null
        return try {
            json.decodeFromString(serializer, body)
        } catch (e: Exception) {
            null
        }
    }

    // ── Connections ──────────────────────────────────────────────────────────────

    suspend fun getConnections(): ConnectionResponse? =
        execute(baseRequest("/connections").get().build())
            .decodeOrNull(ConnectionResponse.serializer())

    suspend fun createInvitation(): CreateInvitationResponse =
        execute(
            baseRequest("/connections")
                .post(encode(CreateInvitationRequest.serializer(),
                    CreateInvitationRequest(IdentusConfig.cloudAgentConnectionLabel)))
                .build()
        ).decodeOrThrow(CreateInvitationResponse.serializer())

    // ── DIDs ────────────────────────────────────────────────────────────────────

    suspend fun createIssuerDID(request: CreateDIDRequest): CreateDIDResponse =
        execute(
            baseRequest("/did-registrar/dids")
                .post(encode(CreateDIDRequest.serializer(), request))
                .build()
        ).decodeOrThrow(CreateDIDResponse.serializer())

    suspend fun didsOnCloudAgent(): DIDsOnCloudAgentResponse =
        execute(baseRequest("/did-registrar/dids").get().build())
            .decodeOrThrow(DIDsOnCloudAgentResponse.serializer())

    suspend fun didStatus(shortOrLongFormDID: String): DIDStatusResponse? =
        execute(baseRequest("/did-registrar/dids/$shortOrLongFormDID").get().build())
            .decodeOrNull(DIDStatusResponse.serializer())

    suspend fun requestDIDPublication(request: PublishDIDRequest): PublishDIDResponse =
        execute(
            baseRequest("/did-registrar/dids/${request.didRef}/publications")
                .post(encode(PublishDIDRequest.serializer(), request))
                .build()
        ).decodeOrThrow(PublishDIDResponse.serializer())

    // ── Schemas ──────────────────────────────────────────────────────────────────

    suspend fun createPassportSchema(schema: PassportSchema): PassportSchema =
        execute(
            baseRequest("/schema-registry/schemas")
                .post(encode(PassportSchema.serializer(), schema))
                .build()
        ).decodeOrThrow(PassportSchema.serializer())

    suspend fun createTicketSchema(schema: TicketSchema): TicketSchema =
        execute(
            baseRequest("/schema-registry/schemas")
                .post(encode(TicketSchema.serializer(), schema))
                .build()
        ).decodeOrThrow(TicketSchema.serializer())

    suspend fun getPassportSchemaByGuid(guid: String): PassportSchema? =
        execute(baseRequest("/schema-registry/schemas/$guid").get().build())
            .decodeOrNull(PassportSchema.serializer())

    suspend fun getTicketSchemaByGuid(guid: String): TicketSchema? =
        execute(baseRequest("/schema-registry/schemas/$guid").get().build())
            .decodeOrNull(TicketSchema.serializer())

    suspend fun listSchemas(): SchemaSummaryPage? =
        execute(baseRequest("/schema-registry/schemas").get().build())
            .decodeOrNull(SchemaSummaryPage.serializer())

    // ── Credential offers ──────────────────────────────────────────────────────────

    suspend fun createPassportCredentialOffer(request: CreateCredentialOfferRequest): CreateCredentialOfferResponse =
        execute(
            baseRequest("/issue-credentials/credential-offers")
                .post(encode(CreateCredentialOfferRequest.serializer(), request))
                .build()
        ).decodeOrThrow(CreateCredentialOfferResponse.serializer())

    suspend fun createTicketCredentialOffer(request: CreateTicketCredentialOfferRequest): CreateCredentialOfferResponse =
        execute(
            baseRequest("/issue-credentials/credential-offers")
                .post(encode(CreateTicketCredentialOfferRequest.serializer(), request))
                .build()
        ).decodeOrThrow(CreateCredentialOfferResponse.serializer())

    suspend fun credentialRecordStatus(recordId: String): CredentialRecordStatus? =
        execute(baseRequest("/issue-credentials/records/$recordId").get().build())
            .decodeOrNull(CredentialRecordStatus.serializer())

    // ── Present proof ───────────────────────────────────────────────────────────────

    suspend fun createProofPresentation(request: CreateProofPresentationRequest): PresentationResponseContent =
        execute(
            baseRequest("/present-proof/presentations")
                .post(encode(CreateProofPresentationRequest.serializer(), request))
                .build()
        ).decodeOrThrow(PresentationResponseContent.serializer())

    suspend fun getPresentations(): PresentationsResponse =
        execute(baseRequest("/present-proof/presentations").get().build())
            .decodeOrThrow(PresentationsResponse.serializer())

    suspend fun getProofPresentationRecord(presentationId: String): PresentationResponseContent? =
        execute(baseRequest("/present-proof/presentations/$presentationId").get().build())
            .decodeOrNull(PresentationResponseContent.serializer())

    suspend fun updatePresentationProof(
        presentationId: String,
        request: VerifierPresentationActionRequest
    ): PresentationResponseContent? =
        execute(
            baseRequest("/present-proof/presentations/$presentationId")
                .patch(encode(VerifierPresentationActionRequest.serializer(), request))
                .build()
        ).decodeOrNull(PresentationResponseContent.serializer())
}
