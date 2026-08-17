package com.identusbook.flighttix.auth

import com.identusbook.flighttix.agent.Identus

/**
 * The entire "login" model — port of iOS `Auth/Auth.swift`.
 * You are "logged in" iff the wallet holds a Passport VC.
 */
object Auth {

    private var authValid: Boolean = false

    suspend fun logout() {
        authValid = false
    }

    suspend fun isLoggedIn(): Boolean {
        if (!authValid) {
            if (loginVCExists()) authValid = true
        }
        return authValid
    }

    private suspend fun loginVCExists(): Boolean {
        val schemaId = Identus.readPassportSchemaIdFromKeychain() ?: return false
        return Identus.fetchCredential(schemaId) != null
    }
}
