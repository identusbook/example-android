# FlightTix — Identus Example Android App

FlightTix for Android is a **native Kotlin / Jetpack Compose** port of the SwiftUI
[FlightTix iOS app](../example-ios). It runs an on-device **edge agent** (the
[Hyperledger Identus Edge Agent SDK for Kotlin/KMP](https://github.com/hyperledger-identus/sdk-kmp))
that:

- requests **mediation** from an Identus Mediator (to receive DIDComm messages while offline),
- pairs with an Identus **Cloud Agent**, and
- issues / holds verifiable credentials (a "passport" and a flight "ticket"),
- and, at airport security, requests and verifies a **presentation** of the ticket.

It is a line-for-line behavioral translation of the iOS reference — same screens, copy,
flows, Cloud Agent endpoints, DIDComm messages, and startup sequence. See
[`../example-ios/SPEC.md`](../example-ios/SPEC.md) for the language-neutral specification;
this app follows it.

The Android project lives in [`FlightTix/`](FlightTix).

---

## Prerequisites

- **Android Studio** (Koala/Ladybug or newer) with an emulator, **or** the Android SDK +
  a physical device. `compileSdk 34`, `minSdk 26`.
- **JDK 17–21** to run Gradle. (Android Studio bundles a suitable JBR; from the CLI this
  repo was verified with Homebrew `openjdk@21`. The Gradle wrapper pins **Gradle 8.9**,
  AGP **8.5.2**, Kotlin **1.9.25**.)
- A running **identus-docker** backend stack (Cloud Agent + Mediator + PRISM node), exactly
  as for the iOS app — see [`../example-ios/README.md`](../example-ios/README.md) Part 1.
- **A GitHub Personal Access Token with the `read:packages` scope** (see below) — required
  to download the SDK's native dependency.

---

## 1. GitHub Packages token (required)

The Identus SDK (`org.hyperledger.identus:edge-agent-sdk-android:4.0.0`, on Maven Central)
pulls a native runtime dependency — `org.hyperledger:anoncreds_uniffi-android` (the AnonCreds
uniffi wrapper) — that is **only published to GitHub Packages**, not Maven Central. GitHub
Packages Maven **always requires authentication**, even for public packages.

Create a token (classic) with **`read:packages`** at
<https://github.com/settings/tokens>, then add it to your **user-level** Gradle properties
(kept out of the repo):

```bash
# ~/.gradle/gradle.properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.token=ghp_your_token_with_read_packages
```

Alternatively export `GITHUB_ACTOR` / `GITHUB_TOKEN` in your environment. The repo referenced
is `LF-Decentralized-Trust-labs/aries-uniffi-wrappers`; the Maven repo + credential wiring is
already set up in [`FlightTix/settings.gradle.kts`](FlightTix/settings.gradle.kts).

> If you have the `gh` CLI, you can add the scope to your existing login with:
> `gh auth refresh -s read:packages` — then use `gh auth token` as `gpr.token`.

---

## 2. Point the app at your backend

All backend config lives in one file:
[`FlightTix/app/src/main/java/com/identusbook/flighttix/agent/IdentusConfig.kt`](FlightTix/app/src/main/java/com/identusbook/flighttix/agent/IdentusConfig.kt).

The iOS Simulator shares the Mac's `/etc/hosts`, so it can reach the backend at the Docker
hostnames `cloud-agent` / `identus-mediator`. **An Android emulator/device cannot.** Do the
device-style setup from the iOS README:

1. Reconfigure the docker stack to advertise a host the device can reach. For the **Android
   emulator**, that host is `10.0.2.2` (the emulator's alias for your Mac's loopback); for a
   **physical device** use your Mac's LAN IP. In `identus-docker/docker-compose.yaml`:
   - Mediator:    `SERVICE_ENDPOINTS=http://10.0.2.2:8080;ws://10.0.2.2:8080/ws`
   - Cloud Agent: `DIDCOMM_SERVICE_URL=http://10.0.2.2:8090`, `REST_SERVICE_URL=http://10.0.2.2:8085`
   - then `docker compose up -d --force-recreate`.
2. In `IdentusConfig.kt`, set:
   - `cloudAgentBaseUrl = "http://10.0.2.2:8085"`
   - `mediatorDidString` ← the mediator's DID from `http://10.0.2.2:8080/` (the page's
     `<meta name="did">`).

Cleartext HTTP to the local backend is already permitted (see
`res/xml/network_security_config.xml`), mirroring the iOS App Transport Security exception.

---

## 3. Build & run

Open [`FlightTix/`](FlightTix) in Android Studio (**File → Open**), let it sync, pick an
emulator/device, and **Run**. Or from the CLI:

```bash
cd example-android/FlightTix
./gradlew :app:installDebug     # build + install on a running emulator/device
```

On launch the app bootstraps automatically (`Identus.startUpAndConnect()`): starts the edge
agent and achieves mediation → creates the Cloud Agent connection → creates & publishes the
issuer DID → registers the passport & ticket schemas → shows the tabs. Publishing the issuer
DID on a cold start can take a while (it polls the ledger once per second); the Loading
screen shows live progress.

---

## How it maps to the iOS app

| iOS (Swift / SwiftUI) | Android (Kotlin / Compose) |
| --- | --- |
| `Agent/Identus.swift` | `agent/Identus.kt` |
| `Agent/IdentusConfig.swift` + `FlightTixURLSessionConfig.swift` | `agent/IdentusConfig.kt` |
| `Agent/IdentusStatus.swift` | `agent/IdentusStatus.kt` |
| `Networking/API+CloudAgent.swift` + `APIClient.swift` | `net/CloudAgentApi.kt` |
| `Networking/APIModels/*` | `net/ApiModels.kt` |
| `Models/*` + `Schemas/VCSchemas.swift` | `model/Models.kt`, `model/Schemas.kt` |
| Keychain (`KeychainSwift`) | `store/SecureStore.kt` (EncryptedSharedPreferences) |
| `Auth/Auth.swift` | `auth/Auth.kt` |
| SwiftUI screens + view models | `ui/screens/*` + `ui/ContentView.kt` |
| `DesignSystem.swift`, `ModalManager.swift`, `FlightSearch.swift` | `ui/components/*`, `ui/ModalManager.kt`, `ui/FlightSearch.kt` |
| EdgeAgentSDK (`DIDCommAgent`) | Identus KMP `EdgeAgent` (Apollo/Castor/Pluto/Mercury/Pollux) |

### Intentional differences (faithful, not literal)

- **Credential-offer handling is generalized.** The iOS reference gated incoming offers on
  the stored *passport* thid (and reused the same keychain key for the ticket thid). Per the
  SPEC's "a port should generalize this" note — and matching the KMP SDK sample — this port
  accepts **any** offer and de-duplicates by `thid`, so both Passport and Ticket issuance
  work through the same handler.
- **Message stream.** iOS subscribes to `handleReceivedMessagesEvents()`; the KMP SDK
  surfaces messages via `pluto.getAllMessages()` — this port collects that flow, filters to
  received messages, and dispatches by PIURI exactly as iOS does.
- **Reading VC claims.** Rather than manually decoding the JWS payload, this port reads
  `credential.claims` and `credential.properties["schema"]` from the SDK's `JWTCredential`.
- The cosmetic sleeps (2 s ready delay, 20 s register wait, 30 s Dev Utils wait, issuer-DID
  publish poll) are preserved for behavioral parity.
