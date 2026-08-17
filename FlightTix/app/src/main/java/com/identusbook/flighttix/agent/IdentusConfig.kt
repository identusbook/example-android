package com.identusbook.flighttix.agent

/**
 * All backend/wallet configuration for FlightTix — the Android equivalent of the iOS
 * `Agent/IdentusConfig.swift` (config) + `Networking/FlightTixURLSessionConfig.swift` (baseURL).
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * ⚠️  NETWORK REACHABILITY (read before running)
 * ─────────────────────────────────────────────────────────────────────────────
 * The iOS reference reaches the backend at the Docker hostnames `cloud-agent` and
 * `identus-mediator` because the Simulator shares the Mac's /etc/hosts. An Android
 * emulator/device does NOT, so those names will not resolve as-is.
 *
 * To run against a local identus-docker stack from the Android emulator/device, do the
 * device-style setup from the iOS README:
 *   1. Reconfigure the docker stack to advertise a host the device can reach — for the
 *      emulator that is `10.0.2.2` (the host loopback alias); for a physical device use
 *      your Mac's LAN IP (e.g. 192.168.x.x):
 *        Mediator:    SERVICE_ENDPOINTS=http://10.0.2.2:8080;ws://10.0.2.2:8080/ws
 *        Cloud Agent: DIDCOMM_SERVICE_URL=http://10.0.2.2:8090
 *                     REST_SERVICE_URL=http://10.0.2.2:8085
 *      then `docker compose up -d --force-recreate`.
 *   2. Set [cloudAgentBaseUrl] below to `http://10.0.2.2:8085`.
 *   3. Set [mediatorDidString] to the mediator's new DID (copy from http://10.0.2.2:8080/).
 */
object IdentusConfig {

    // ── Mediator ──────────────────────────────────────────────────────────────
    // OOB invitation URL (kept for parity with iOS; the mediation flow uses the DID).
    const val mediatorOOBString: String =
        "https://mediator.rootsid.cloud?_oob=eyJ0eXBlIjoiaHR0cHM6Ly9kaWRjb21tLm9yZy9vdXQtb2YtYmFuZC8yLjAvaW52aXRhdGlvbiJ9"

    // Peer DID of the mediator. Default is the deterministic identus-docker 1.1.0 mediator
    // DID on :8080 (from the iOS README). Replace it when pointing at another mediator or
    // a relocated endpoint (see the network note above).
    const val mediatorDidString: String =
        "did:peer:2.Ez6LSghwSE437wnDE1pt3X6hVDUQzSjsHzinpX3XFvMjRAm7y" +
            ".Vz6Mkhh1e5CEYYq6JBUcTZ6Cp2ranCWRrv7Yax3Le4N59R6dd" +
            ".SeyJ0IjoiZG0iLCJzIjp7InVyaSI6Imh0dHA6Ly9pZGVudHVzLW1lZGlhdG9yOjgwODAiLCJhIjpbImRpZGNvbW0vdjIiXX19" +
            ".SeyJ0IjoiZG0iLCJzIjp7InVyaSI6IndzOi8vaWRlbnR1cy1tZWRpYXRvcjo4MDgwL3dzIiwiYSI6WyJkaWRjb21tL3YyIl19fQ"

    // ── Cloud Agent REST ────────────────────────────────────────────────────────
    // Where REST calls go, AND (embedded into schema URLs) the host the Cloud Agent uses
    // to dereference schemas internally. See the network note above.
    const val cloudAgentBaseUrl: String = "http://cloud-agent:8085"

    const val cloudAgentConnectionLabel: String = "FlightTixAndroid-CloudAgent"

    // ── Stable schema $id URLs (travel inside credentials) ──────────────────────
    const val passportSchemaId: String = "https://identusbook.com/flighttix-passport-1.0.0"
    const val ticketSchemaId: String = "https://identusbook.com/flighttix-ticket-1.0.0"

    // ── Secure-store (Keychain-equivalent) keys ─────────────────────────────────
    const val seedKeychainKey: String = "FlightTixSeed"
    const val cloudAgentConnectionIdKeychainKey: String = "CloudAgentConnectionId"
    const val cloudAgentIssuerDIDKeychainKey: String = "CloudAgentIssuerDID"
    const val passportIssueVCThidKeychainKey: String = "IssuePassportVC"
    const val ticketIssueVCThidKeychainKey: String = "IssueTicketVC"
    const val passportSchemaIdKeychainKey: String = "PassportSchemaId"
    const val ticketSchemaIdKeychainKey: String = "TicketSchemaId"
}
