package com.identusbook.flighttix.store

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Android equivalent of the iOS Keychain (KeychainSwift) used throughout Identus.swift.
 * Backed by EncryptedSharedPreferences so the seed and DIDs are stored encrypted at rest.
 *
 * String values are stored as-is; byte values (the seed) are Base64-wrapped.
 */
object SecureStore {

    private const val PREFS_NAME = "flighttix_secure_store"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getString(key: String): String? = prefs.getString(key, null)

    fun set(key: String, value: String): Boolean =
        prefs.edit().putString(key, value).commit()

    fun getBytes(key: String): ByteArray? =
        prefs.getString(key, null)?.let { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }

    fun setBytes(key: String, value: ByteArray): Boolean =
        prefs.edit().putString(
            key,
            android.util.Base64.encodeToString(value, android.util.Base64.NO_WRAP)
        ).commit()

    fun delete(key: String): Boolean = prefs.edit().remove(key).commit()

    fun contains(key: String): Boolean = prefs.contains(key)
}
