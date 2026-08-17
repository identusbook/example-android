package com.identusbook.flighttix.agent

import android.content.Context
import com.identusbook.flighttix.net.CloudAgentApi
import com.identusbook.flighttix.net.CreateCredentialOfferRequest
import com.identusbook.flighttix.net.CreateDIDRequest
import com.identusbook.flighttix.net.CreateProofPresentationRequest
import com.identusbook.flighttix.net.CreateTicketCredentialOfferRequest
import com.identusbook.flighttix.net.DIDPublicKey
import com.identusbook.flighttix.net.DocumentTemplate
import com.identusbook.flighttix.net.PresentationResponseContent
import com.identusbook.flighttix.net.PresentationsResponse
import com.identusbook.flighttix.net.ProofPresentationOptions
import com.identusbook.flighttix.net.ProofRequestAux
import com.identusbook.flighttix.net.PublishDIDRequest
import com.identusbook.flighttix.net.VerifierPresentationActionRequest
import com.identusbook.flighttix.model.PassportProperties
import com.identusbook.flighttix.model.PassportSchema
import com.identusbook.flighttix.model.PassportSchemaData
import com.identusbook.flighttix.model.PropertyDetails
import com.identusbook.flighttix.model.TicketProperties
import com.identusbook.flighttix.model.TicketSchema
import com.identusbook.flighttix.model.TicketSchemaData
import com.identusbook.flighttix.store.SecureStore
import com.identusbook.flighttix.util.decodeBase64
import com.identusbook.flighttix.util.encodeBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hyperledger.identus.walletsdk.apollo.ApolloImpl
import org.hyperledger.identus.walletsdk.castor.CastorImpl
import org.hyperledger.identus.walletsdk.domain.buildingblocks.Apollo
import org.hyperledger.identus.walletsdk.domain.buildingblocks.Castor
import org.hyperledger.identus.walletsdk.domain.buildingblocks.Mercury
import org.hyperledger.identus.walletsdk.domain.buildingblocks.Pluto
import org.hyperledger.identus.walletsdk.domain.buildingblocks.Pollux
import org.hyperledger.identus.walletsdk.domain.models.ApiImpl
import org.hyperledger.identus.walletsdk.domain.models.Credential
import org.hyperledger.identus.walletsdk.domain.models.DID
import org.hyperledger.identus.walletsdk.domain.models.Message
import org.hyperledger.identus.walletsdk.domain.models.Seed
import org.hyperledger.identus.walletsdk.domain.models.httpClient
import org.hyperledger.identus.walletsdk.edgeagent.EdgeAgent
import org.hyperledger.identus.walletsdk.edgeagent.mediation.BasicMediatorHandler
import org.hyperledger.identus.walletsdk.edgeagent.mediation.MediationHandler
import org.hyperledger.identus.walletsdk.edgeagent.protocols.ProtocolType
import org.hyperledger.identus.walletsdk.edgeagent.protocols.issueCredential.IssueCredential
import org.hyperledger.identus.walletsdk.edgeagent.protocols.issueCredential.OfferCredential
import org.hyperledger.identus.walletsdk.edgeagent.protocols.outOfBand.OutOfBandInvitation
import org.hyperledger.identus.walletsdk.edgeagent.protocols.proofOfPresentation.RequestPresentation
import org.hyperledger.identus.walletsdk.mercury.MercuryImpl
import org.hyperledger.identus.walletsdk.mercury.resolvers.DIDCommWrapper
import org.hyperledger.identus.walletsdk.pluto.PlutoImpl
import org.hyperledger.identus.walletsdk.pluto.data.DbConnectionImpl
import org.hyperledger.identus.walletsdk.pollux.PolluxImpl
import org.hyperledger.identus.walletsdk.pollux.models.JWTCredential
import java.util.UUID

/**
 * The heart of FlightTix — port of iOS `Agent/Identus.swift`.
 *
 * Bootstraps the on-device edge agent (EdgeAgent), achieves mediation, pairs with the
 * Cloud Agent, publishes an issuer DID + schemas, and drives credential issuance and
 * proof presentation over DIDComm + the Cloud Agent REST API.
 *
 * Singleton (like iOS `Identus.shared`); [init] must be called once with an app Context.
 */
object Identus {

    private lateinit var appContext: Context
    private var started = false

    // Building blocks (constructed lazily on first start).
    private val apollo: Apollo = ApolloImpl()
    private val castor: Castor = CastorImpl(apollo)
    private val pluto: Pluto = PlutoImpl(DbConnectionImpl())
    private val pollux: Pollux = PolluxImpl(apollo, castor)
    private val mercury: Mercury = MercuryImpl(castor, DIDCommWrapper(castor, pluto, apollo), ApiImpl(httpClient()))

    private var handler: MediationHandler? = null
    private var agent: EdgeAgent? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // DIDComm handler dedup sets.
    private val processedOfferThids = mutableSetOf<String>()
    private val processedIssueThids = mutableSetOf<String>()
    private val processedPresentationMessageIds = mutableSetOf<String>()
    private var messageStreamStarted = false

    fun init(context: Context) {
        appContext = context.applicationContext
        SecureStore.init(appContext)
    }

    // ── Startup & mediation (§8.1) ──────────────────────────────────────────────

    suspend fun startUpAndConnect() = withContext(Dispatchers.IO) {
        // Runs entirely off the main thread — the edge agent's DIDComm/mediation work must
        // not touch the UI dispatcher (an unreachable mediator would otherwise crash it).
        start()
        startMessageStream()
        createConnectionToCloudAgentIfNotExists()
        createIssuerDIDOnCloudAgentIfNotExists()
        // Both schemas run so the stored GUIDs stay in sync with the Cloud Agent.
        createPassportSchemaIfNotExists()
        createTicketSchemaIfNotExists()
        IdentusStatus.set(IdentusStatusState.Ready)
    }

    private suspend fun start() {
        IdentusStatus.set(IdentusStatusState.StartingAgent)
        try {
            val seed = loadOrCreateSeed()
            val mediatorDID = DID(IdentusConfig.mediatorDidString)
            val mediationHandler = BasicMediatorHandler(
                mediatorDID = mediatorDID,
                mercury = mercury,
                store = BasicMediatorHandler.PlutoMediatorRepositoryImpl(pluto)
            )
            handler = mediationHandler
            val edgeAgent = EdgeAgent(
                apollo = apollo,
                castor = castor,
                pluto = pluto,
                mercury = mercury,
                pollux = pollux,
                seed = seed,
                mediatorHandler = mediationHandler
            )
            agent = edgeAgent

            // Pluto (local encrypted DB) must be running before the agent starts.
            try {
                (pluto as PlutoImpl).start(appContext)
            } catch (e: Throwable) {
                if (e.javaClass.name !=
                    "org.hyperledger.identus.walletsdk.domain.models.PlutoError\$DatabaseServiceAlreadyRunning"
                ) throw e
            }

            edgeAgent.start()        // mediation handshake runs here
            try {
                edgeAgent.startFetchingMessages()
            } catch (_: Exception) {
            }
            started = true
        } catch (e: Throwable) {
            // Surface the real failure (mirrors iOS) instead of cascading into
            // confusing "no mediator" errors downstream.
            println("EdgeAgent.start() failed: $e")
            IdentusStatus.set(IdentusStatusState.Error(e.message ?: e.toString()))
            throw e
        }
    }

    suspend fun stop() {
        agent?.stop()
        started = false
    }

    // ── Seed ────────────────────────────────────────────────────────────────────

    private fun loadOrCreateSeed(): Seed {
        SecureStore.getBytes(IdentusConfig.seedKeychainKey)?.let {
            return Seed(value = it)
        }
        val seed = apollo.createRandomSeed().seed
        SecureStore.setBytes(IdentusConfig.seedKeychainKey, seed.value)
        return seed
    }

    // ── Teardown (dev, §8.7) ──────────────────────────────────────────────────────

    suspend fun tearDown() {
        agent?.stop()
        SecureStore.delete(IdentusConfig.seedKeychainKey)
        if (readIssuerDIDFromKeychain() != null) SecureStore.delete(IdentusConfig.cloudAgentIssuerDIDKeychainKey)
        if (readConnectionIdFromKeychain() != null) SecureStore.delete(IdentusConfig.cloudAgentConnectionIdKeychainKey)
        if (SecureStore.getString(IdentusConfig.passportIssueVCThidKeychainKey) != null)
            SecureStore.delete(IdentusConfig.passportIssueVCThidKeychainKey)
        if (readPassportSchemaIdFromKeychain() != null) SecureStore.delete(IdentusConfig.passportSchemaIdKeychainKey)
        if (SecureStore.getString(IdentusConfig.ticketIssueVCThidKeychainKey) != null)
            SecureStore.delete(IdentusConfig.ticketIssueVCThidKeychainKey)
        if (readTicketSchemaIdFromKeychain() != null) SecureStore.delete(IdentusConfig.ticketSchemaIdKeychainKey)
        started = false
    }

    // ── Connection to Cloud Agent (§8.2) ───────────────────────────────────────────

    fun readConnectionIdFromKeychain(): String? =
        SecureStore.getString(IdentusConfig.cloudAgentConnectionIdKeychainKey)

    private suspend fun createConnectionToCloudAgentIfNotExists() {
        IdentusStatus.set(IdentusStatusState.CreatingConnectionToCloudAgent)
        val stored = readConnectionIdFromKeychain()
        if (stored != null) {
            if (!connectionExists(stored, IdentusConfig.cloudAgentConnectionLabel)) {
                askCloudAgentForConnectionInvitationAndAcceptIt()
            }
        } else {
            askCloudAgentForConnectionInvitationAndAcceptIt()
        }
    }

    private suspend fun connectionExists(connectionId: String, label: String): Boolean {
        val connections = CloudAgentApi.getConnections() ?: return false
        return connections.contents.any { it.connectionId == connectionId && it.label == label }
    }

    private suspend fun askCloudAgentForConnectionInvitationAndAcceptIt() {
        val edgeAgent = agent ?: throw IllegalStateException("Agent not started")
        // Cloud Agent creates the OOB invitation; the connection record's id is the
        // connectionId used by every subsequent REST call.
        val response = CloudAgentApi.createInvitation()
        val invitation = edgeAgent.parseInvitation(response.invitation.invitationUrl)
        if (invitation is OutOfBandInvitation) {
            edgeAgent.acceptOutOfBandInvitation(invitation)
        } else {
            throw IllegalStateException("Cloud Agent invitation was not an OOB invitation")
        }
        if (!SecureStore.set(IdentusConfig.cloudAgentConnectionIdKeychainKey, response.connectionId)) {
            throw IllegalStateException("Failed to store connectionId")
        }
    }

    // ── Issuer DID (§8.3) ─────────────────────────────────────────────────────────

    private fun storeIssuerDIDInKeychain(did: String): Boolean =
        SecureStore.set(IdentusConfig.cloudAgentIssuerDIDKeychainKey, did.encodeBase64())

    fun readIssuerDIDFromKeychain(): String? =
        SecureStore.getString(IdentusConfig.cloudAgentIssuerDIDKeychainKey)?.decodeBase64()

    private suspend fun createIssuerDIDOnCloudAgentIfNotExists() {
        // Assume one DID per Cloud Agent — treat whatever exists as the issuer.
        val dids = CloudAgentApi.didsOnCloudAgent()
        if (dids.contents.isNotEmpty()) {
            val shortFormDid = dids.contents.first().did
            if (shortFormDid == null) {
                println("Could not get ShortForm DID for Cloud-Agent Issuer DID")
                return
            }
            if (!storeIssuerDIDInKeychain(shortFormDid)) {
                throw IllegalStateException("Could not store Cloud-Agent Issuer DID")
            }
            IdentusStatus.set(IdentusStatusState.IssuerDIDAlreadyExists)
            return
        }

        IdentusStatus.set(IdentusStatusState.CreatingIssuerDID)
        val request = CreateDIDRequest(
            documentTemplate = DocumentTemplate(
                publicKeys = listOf(
                    DIDPublicKey(id = "auth-1", purpose = "authentication"),
                    DIDPublicKey(id = "issue-1", purpose = "assertionMethod")
                ),
                services = emptyList()
            )
        )
        val created = CloudAgentApi.createIssuerDID(request)
        println("Created Issuer DID on Cloud-Agent: ${created.longFormDid}")
        if (!storeIssuerDIDInKeychain(created.longFormDid)) {
            throw IllegalStateException("Could not store created Issuer DID")
        }
        requestDIDPublication(longFormDID = created.longFormDid)
        pollIssuerCheckDIDStatusPublished(created.longFormDid)
    }

    private suspend fun requestDIDPublication(longFormDID: String) {
        val status = CloudAgentApi.didStatus(longFormDID)
            ?: throw IllegalStateException("DID status failed")
        requestDIDPublicationShort(status.did)
    }

    private suspend fun requestDIDPublicationShort(shortFormDID: String) {
        val scheduled = CloudAgentApi.requestDIDPublication(PublishDIDRequest(didRef = shortFormDID))
        if (scheduled.scheduledOperation.didRef != shortFormDID) {
            throw IllegalStateException("DID ref match failed")
        }
    }

    suspend fun verifyIssuerDIDIsPublished(shortOrLongFormDID: String): Boolean {
        val did = CloudAgentApi.didStatus(shortOrLongFormDID)
            ?: throw IllegalStateException("Issuer DID not published")
        return did.status == "PUBLISHED"
    }

    private suspend fun pollIssuerCheckDIDStatusPublished(shortOrLongFormDID: String) {
        // Block until PUBLISHED — nothing works until the issuer is on-ledger.
        IdentusStatus.set(IdentusStatusState.PublishingIssuerDID)
        while (true) {
            val isPublished = verifyIssuerDIDIsPublished(shortOrLongFormDID)
            println("Is Issuer DID Published yet?: ${if (isPublished) "Yes" else "No"}")
            if (isPublished) {
                IdentusStatus.set(IdentusStatusState.IssuerDIDPublished)
                return
            }
            delay(1000)
        }
    }

    /** Resolve the short-form DID string from a long-form DID via the Cloud Agent. */
    suspend fun didShortForm(fromLongFormDID: String): String? =
        CloudAgentApi.didStatus(fromLongFormDID)?.did

    // ── Schemas (§8.6) ──────────────────────────────────────────────────────────────

    fun readPassportSchemaIdFromKeychain(): String? =
        SecureStore.getString(IdentusConfig.passportSchemaIdKeychainKey)

    fun readTicketSchemaIdFromKeychain(): String? =
        SecureStore.getString(IdentusConfig.ticketSchemaIdKeychainKey)

    private suspend fun createPassportSchemaIfNotExists() {
        IdentusStatus.set(IdentusStatusState.CheckingPassportSchema)
        val savedGuid = readPassportSchemaIdFromKeychain()
        if (savedGuid != null && CloudAgentApi.getPassportSchemaByGuid(savedGuid) != null) return
        // Adopt an existing schema for our issuer if present (creation errors on duplicates).
        val issuerDID = readIssuerDIDFromKeychain()
        val shortIssuer = issuerDID?.let { runCatching { didShortForm(it) }.getOrNull() }
        if (shortIssuer != null) {
            val existing = findExistingSchemaGuid("passport", shortIssuer)
            if (existing != null) {
                SecureStore.set(IdentusConfig.passportSchemaIdKeychainKey, existing)
                return
            }
        }
        createPassportSchema()
    }

    private suspend fun findExistingSchemaGuid(name: String, author: String): String? {
        val page = CloudAgentApi.listSchemas() ?: return null
        return page.contents.firstOrNull { it.name == name && it.author == author }?.guid
    }

    private suspend fun createPassportSchema() {
        val issuerDID = readIssuerDIDFromKeychain()
            ?: throw IllegalStateException("Issuer DID not present")
        val shortForm = didShortForm(issuerDID) ?: return
        IdentusStatus.set(IdentusStatusState.CreatingPassportSchema)
        val schema = PassportSchema(
            guid = null,
            name = "passport",
            version = "1.0.0",
            description = "Passport Schema",
            type = "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json",
            author = shortForm,
            tags = listOf("passport", "schema"),
            schema = PassportSchemaData(
                id = IdentusConfig.passportSchemaId,
                schema = "https://json-schema.org/draft/2020-12/schema",
                description = "Passport",
                type = "object",
                properties = PassportProperties(
                    name = PropertyDetails(type = "string"),
                    dateOfIssuance = PropertyDetails(type = "string", format = "date-time"),
                    passportNumber = PropertyDetails(type = "string"),
                    dob = PropertyDetails(type = "string", format = "date-time")
                ),
                required = listOf("name", "dateOfIssuance", "passportNumber", "dob"),
                additionalProperties = true
            )
        )
        val created = CloudAgentApi.createPassportSchema(schema)
        created.guid?.let {
            println("Passport Schema Created with ID: $it")
            if (!SecureStore.set(IdentusConfig.passportSchemaIdKeychainKey, it)) {
                throw IllegalStateException("Schema id failed to save")
            }
        }
        IdentusStatus.set(IdentusStatusState.CreatedPassportSchema)
    }

    suspend fun createTicketSchemaIfNotExists() {
        val savedGuid = readTicketSchemaIdFromKeychain()
        if (savedGuid != null && CloudAgentApi.getTicketSchemaByGuid(savedGuid) != null) return
        val issuerDID = readIssuerDIDFromKeychain()
        val shortIssuer = issuerDID?.let { runCatching { didShortForm(it) }.getOrNull() }
        if (shortIssuer != null) {
            val existing = findExistingSchemaGuid("ticket", shortIssuer)
            if (existing != null) {
                SecureStore.set(IdentusConfig.ticketSchemaIdKeychainKey, existing)
                return
            }
        }
        createTicketSchema()
    }

    private suspend fun createTicketSchema() {
        val issuerDID = readIssuerDIDFromKeychain()
            ?: throw IllegalStateException("Issuer DID not present")
        val shortForm = didShortForm(issuerDID) ?: return
        val schema = TicketSchema(
            guid = null,
            name = "ticket",
            version = "1.0.0",
            description = "Ticket Schema",
            type = "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json",
            author = shortForm,
            tags = listOf("ticket", "schema"),
            schema = TicketSchemaData(
                id = IdentusConfig.ticketSchemaId,
                schema = "https://json-schema.org/draft/2020-12/schema",
                description = "Ticket",
                type = "object",
                properties = TicketProperties(
                    name = PropertyDetails(type = "string"),
                    dateOfIssuance = PropertyDetails(type = "string", format = "date-time"),
                    price = PropertyDetails(type = "number"),
                    departure = PropertyDetails(type = "string"),
                    arrival = PropertyDetails(type = "string"),
                    flightId = PropertyDetails(type = "string")
                ),
                required = listOf("name", "dateOfIssuance"),
                additionalProperties = true
            )
        )
        val created = CloudAgentApi.createTicketSchema(schema)
        created.guid?.let {
            println("Ticket Schema Created with ID: $it")
            if (!SecureStore.set(IdentusConfig.ticketSchemaIdKeychainKey, it)) {
                throw IllegalStateException("Schema id failed to save")
            }
        }
    }

    // ── Credential issuance (§8.4) ────────────────────────────────────────────────

    fun storePassportVCThidInKeychain(thid: String): Boolean =
        SecureStore.set(IdentusConfig.passportIssueVCThidKeychainKey, thid)

    fun storeTicketVCThidInKeychain(thid: String): Boolean =
        SecureStore.set(IdentusConfig.ticketIssueVCThidKeychainKey, thid)

    suspend fun createPassportCredentialOffer(request: CreateCredentialOfferRequest) =
        CloudAgentApi.createPassportCredentialOffer(request)

    suspend fun createTicketCredentialOffer(request: CreateTicketCredentialOfferRequest) =
        CloudAgentApi.createTicketCredentialOffer(request)

    suspend fun credentialRecordStatus(recordId: String) =
        CloudAgentApi.credentialRecordStatus(recordId)

    // ── DIDComm message handler (§8.8) ─────────────────────────────────────────────

    private fun startMessageStream() {
        if (messageStreamStarted) return
        messageStreamStarted = true
        IdentusStatus.set(IdentusStatusState.StartingDIDCommMessageListener)
        scope.launch {
            pluto.getAllMessages().collect { list ->
                val received = list.filter { it.direction == Message.Direction.RECEIVED }
                for (message in received) {
                    try {
                        dispatch(message)
                    } catch (e: Exception) {
                        println("Error handling message ${message.id}: $e")
                    }
                }
            }
        }
    }

    private suspend fun dispatch(message: Message) {
        when (message.piuri) {
            ProtocolType.DidcommOfferCredential.value -> handleOfferedCredential(message)
            ProtocolType.DidcommIssueCredential.value -> handleIssuedCredential(message)
            ProtocolType.DidcommRequestPresentation.value -> handleRequestPresentation(message)
            else -> { /* unhandled — logged upstream by the SDK */ }
        }
    }

    /**
     * Handle an offered credential. The iOS reference gated on the passport thid; per the
     * SPEC's "generalize this" note (and matching the KMP sample), we accept ANY offer and
     * dedup by thid, so both Passport and Ticket issuance flow through here.
     */
    private suspend fun handleOfferedCredential(message: Message) {
        val edgeAgent = agent ?: return
        val thid = message.thid ?: message.id
        if (!processedOfferThids.add(thid)) return
        val offer = OfferCredential.fromMessage(message)
        val subjectDID = edgeAgent.createNewPrismDID()
        val request = edgeAgent.prepareRequestCredentialWithIssuer(subjectDID, offer)
        edgeAgent.sendMessage(request.makeMessage())
    }

    private suspend fun handleIssuedCredential(message: Message) {
        val edgeAgent = agent ?: return
        val thid = message.thid ?: message.id
        if (!processedIssueThids.add(thid)) return
        edgeAgent.processIssuedCredentialMessage(IssueCredential.fromMessage(message))
    }

    private suspend fun handleRequestPresentation(message: Message) {
        val edgeAgent = agent ?: return
        if (!processedPresentationMessageIds.add(message.id)) return
        val requestPresentation = RequestPresentation.fromMessage(message)

        // Present the credential whose schema matches what the Verifier asked for.
        val requestedSchema = requestPresentation.body.proofTypes?.firstOrNull()?.schema
        val requestedGuid = requestedSchema?.let { extractSchemaGUID(it) }
        val credential: Credential? = if (requestedGuid != null) {
            println("Verifier requested proof for schema GUID: $requestedGuid")
            fetchCredential(requestedGuid)
        } else {
            println("No requested schema found; using first available credential")
            edgeAgent.getAllCredentials().first().firstOrNull()
        }

        if (credential == null) {
            println("Credential Not Found for requested schema!")
            throw IllegalStateException("Credential not found")
        }
        if (credential !is JWTCredential) {
            throw IllegalStateException("Credential is not presentable (not a JWT credential)")
        }

        val presentation = edgeAgent.preparePresentationForRequestProof(requestPresentation, credential)
        edgeAgent.sendMessage(presentation.makeMessage())
    }

    /** Fetch the first wallet credential whose schema GUID matches [schemaId] (§8.5.1). */
    suspend fun fetchCredential(schemaId: String): Credential? {
        val edgeAgent = agent ?: return null
        val credentials = edgeAgent.getAllCredentials().first()
        for (cred in credentials) {
            val schemaRef = cred.properties["schema"] as? String ?: continue
            val guid = extractSchemaGUID(schemaRef)
            if (guid == schemaId) return cred
        }
        return null
    }

    fun extractSchemaGUID(url: String): String? {
        val regex = Regex("schemas/([a-f0-9\\-]+)/schema", RegexOption.IGNORE_CASE)
        return regex.find(url)?.groupValues?.getOrNull(1)
    }

    // ── Present proof — verifier side (§8.5) ───────────────────────────────────────

    suspend fun createProofRequest(schemaId: String): PresentationResponseContent? {
        val connectionId = readConnectionIdFromKeychain() ?: return null
        // The Cloud Agent verifies against the exact schema URL and the issuer's SHORT-form
        // DID (the form carried in the credential's `issuer`).
        val issuerDID = readIssuerDIDFromKeychain() ?: run {
            println("Cannot create proof request: issuer DID unavailable")
            return null
        }
        val shortFormIssuerDID = didShortForm(issuerDID) ?: run {
            println("Cannot create proof request: issuer short form unavailable")
            return null
        }
        val schemaURL = "${IdentusConfig.cloudAgentBaseUrl}/schema-registry/schemas/$schemaId/schema"
        val request = CreateProofPresentationRequest(
            connectionId = connectionId,
            options = ProofPresentationOptions(challenge = UUID.randomUUID().toString(), domain = "identusbook.com"),
            proofs = listOf(ProofRequestAux(schemaId = schemaURL, trustIssuers = listOf(shortFormIssuerDID)))
        )
        return CloudAgentApi.createProofPresentation(request)
    }

    suspend fun getPresentations(): PresentationsResponse = CloudAgentApi.getPresentations()

    suspend fun getPresentation(presentationId: String): PresentationResponseContent? =
        CloudAgentApi.getProofPresentationRecord(presentationId)

    /** Statuses at which a verifier presentation is actionable or finalized. */
    private val terminalPresentationStatuses = setOf(
        "PresentationVerified",
        "PresentationVerificationFailed",
        "PresentationAccepted",
        "PresentationRejected",
        "RequestRejected",
        "ProblemReportReceived",
        "ProblemReportSent"
    )

    /** Poll a presentation record until terminal or timeout; return the latest known record. */
    suspend fun awaitPresentationOutcome(
        presentationId: String,
        timeoutSeconds: Double = 30.0,
        pollIntervalSeconds: Double = 1.5
    ): PresentationResponseContent? {
        val deadline = System.currentTimeMillis() + (timeoutSeconds * 1000).toLong()
        var latest: PresentationResponseContent? = null
        while (System.currentTimeMillis() < deadline) {
            getPresentation(presentationId)?.let { record ->
                latest = record
                if (terminalPresentationStatuses.contains(record.status)) return record
            }
            delay((pollIntervalSeconds * 1000).toLong())
        }
        return latest
    }

    suspend fun acceptPresentation(presentationId: String): PresentationResponseContent? =
        CloudAgentApi.updatePresentationProof(presentationId, VerifierPresentationActionRequest.accept)

    suspend fun denyPresentation(presentationId: String): PresentationResponseContent? =
        CloudAgentApi.updatePresentationProof(presentationId, VerifierPresentationActionRequest.reject)
}
