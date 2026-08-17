package com.identusbook.flighttix.ui.screens

import androidx.lifecycle.ViewModel
import com.identusbook.flighttix.agent.Identus
import com.identusbook.flighttix.model.Passport
import com.identusbook.flighttix.model.Traveller
import com.identusbook.flighttix.util.DateUtils
import com.identusbook.flighttix.util.claimString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

/** Passport details — port of iOS `ProfileViewModel.swift`. */
class ProfileViewModel : ViewModel() {

    private val _traveller = MutableStateFlow<Traveller?>(null)
    val traveller: StateFlow<Traveller?> = _traveller.asStateFlow()

    suspend fun getTraveller() {
        _traveller.value = getPassportDetails()
    }

    private suspend fun getPassportDetails(): Traveller? {
        val passportSchemaId = Identus.readPassportSchemaIdFromKeychain()
            ?: throw IllegalStateException("Cannot read passport schema id")
        val credential = Identus.fetchCredential(passportSchemaId) ?: return null
        val name = credential.claimString("name") ?: ""
        val passportNumber = credential.claimString("passportNumber") ?: ""
        val dobString = credential.claimString("dob")
        val dob: Date = dobString?.let { DateUtils.stringToDate(it) } ?: Date()
        return Traveller(
            passport = Passport(
                name = name,
                passportNumber = passportNumber,
                dob = dob
            )
        )
    }
}
