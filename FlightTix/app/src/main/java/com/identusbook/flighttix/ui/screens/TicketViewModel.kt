package com.identusbook.flighttix.ui.screens

import androidx.lifecycle.ViewModel
import com.identusbook.flighttix.agent.Identus
import com.identusbook.flighttix.model.Ticket
import com.identusbook.flighttix.util.claimString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Ticket details — port of iOS `TicketViewModel.swift`. */
class TicketViewModel : ViewModel() {

    private val _ticket = MutableStateFlow<Ticket?>(null)
    val ticket: StateFlow<Ticket?> = _ticket.asStateFlow()

    suspend fun getTicket() {
        _ticket.value = getTicketDetails()
    }

    private suspend fun getTicketDetails(): Ticket? {
        val ticketSchemaId = Identus.readTicketSchemaIdFromKeychain()
            ?: throw IllegalStateException("Cannot read ticket schema id")
        val credential = Identus.fetchCredential(ticketSchemaId) ?: return null
        val price = credential.claimString("price")?.toDoubleOrNull() ?: 0.0
        val departure = credential.claimString("departure") ?: ""
        val arrival = credential.claimString("arrival") ?: ""
        return Ticket(price = price, departure = departure, arrival = arrival)
    }
}
