package com.identusbook.flighttix.model

import java.util.Date
import java.util.UUID

/**
 * In-memory domain models used while screens render — ports of the iOS `Models` structs.
 * The wallet (edge agent SDK) is the real store; these structs are transient.
 */

data class Passport(
    val id: UUID = UUID.randomUUID(),   // local-only; not on the VC
    val name: String,
    val did: String? = null,            // subject DID, from the VC's `sub`
    val passportNumber: String,
    val dob: Date,
    val dateOfIssuance: Date? = null    // from the VC
)

data class Ticket(
    val price: Double,
    val departure: String,
    val arrival: String
)

data class Flight(
    val id: UUID = UUID.randomUUID(),
    val departure: String,
    val arrival: String,
    val price: Double
)

data class Traveller(
    val passport: Passport,
    val tickets: List<Ticket> = emptyList()   // empty in current UI; reserved
)

data class SecurityOfficer(
    val id: UUID = UUID.randomUUID()          // unused in current UI; reserved
)
