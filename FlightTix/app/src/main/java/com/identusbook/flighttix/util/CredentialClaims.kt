package com.identusbook.flighttix.util

import org.hyperledger.identus.walletsdk.domain.models.Claim
import org.hyperledger.identus.walletsdk.domain.models.ClaimType
import org.hyperledger.identus.walletsdk.domain.models.Credential

/**
 * Read a single claim value by key from a wallet credential.
 * JWTCredential exposes every credentialSubject entry as a [ClaimType.StringValue].
 */
fun Credential.claimString(key: String): String? {
    val claim: Claim = claims.firstOrNull { it.key == key } ?: return null
    return when (val v = claim.value) {
        is ClaimType.StringValue -> v.value
        is ClaimType.NumberValue -> v.value.toString()
        is ClaimType.BoolValue -> v.value.toString()
        else -> null
    }
}
