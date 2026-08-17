package com.identusbook.flighttix.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Credential schema registration bodies — ports of iOS `Models/Schemas/VCSchemas.swift`.
 * These are POSTed to /schema-registry/schemas. The embedded JSON Schema uses `$id`/`$schema`.
 *
 * NOTE: `PropertyDetails.format` is nullable and omitted when null (the JSON client is
 * configured with explicitNulls=false), matching iOS where `format: nil` drops the key.
 */

@Serializable
data class PropertyDetails(
    val type: String,
    val format: String? = null
)

// ── Passport ────────────────────────────────────────────────────────────────

@Serializable
data class PassportProperties(
    val name: PropertyDetails,
    val dateOfIssuance: PropertyDetails,
    val passportNumber: PropertyDetails,
    val dob: PropertyDetails
)

@Serializable
data class PassportSchemaData(
    @SerialName("\$id") val id: String,
    @SerialName("\$schema") val schema: String,
    val description: String,
    val type: String,
    val properties: PassportProperties,
    val required: List<String>,
    val additionalProperties: Boolean
)

@Serializable
data class PassportSchema(
    val guid: String? = null,
    val name: String,
    val version: String,
    val description: String,
    val type: String,
    val author: String,
    val tags: List<String>,
    val schema: PassportSchemaData
)

// ── Ticket ──────────────────────────────────────────────────────────────────

@Serializable
data class TicketProperties(
    val name: PropertyDetails,
    val dateOfIssuance: PropertyDetails,
    val price: PropertyDetails,
    val departure: PropertyDetails,
    val arrival: PropertyDetails,
    val flightId: PropertyDetails
)

@Serializable
data class TicketSchemaData(
    @SerialName("\$id") val id: String,
    @SerialName("\$schema") val schema: String,
    val description: String,
    val type: String,
    val properties: TicketProperties,
    val required: List<String>,
    val additionalProperties: Boolean
)

@Serializable
data class TicketSchema(
    val guid: String? = null,
    val name: String,
    val version: String,
    val description: String,
    val type: String,
    val author: String,
    val tags: List<String>,
    val schema: TicketSchemaData
)
