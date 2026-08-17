package com.identusbook.flighttix.util

import android.util.Base64

/**
 * Base64 helpers mirroring iOS `String+base64.swift`.
 * The iOS app uses standard (padded) Base64 to obscure the issuer DID it stores.
 */

fun String.encodeBase64(): String =
    Base64.encodeToString(this.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

fun String.decodeBase64(): String? = try {
    String(Base64.decode(this, Base64.NO_WRAP), Charsets.UTF_8)
} catch (e: IllegalArgumentException) {
    null
}
