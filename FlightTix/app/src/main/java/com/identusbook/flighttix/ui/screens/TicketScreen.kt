package com.identusbook.flighttix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.identusbook.flighttix.ui.components.LabeledRow
import com.identusbook.flighttix.ui.components.ScreenHeader

/** Ticket tab — port of iOS `TicketView.swift`. */
@Composable
fun TicketScreen(model: TicketViewModel = viewModel()) {
    val ticket by model.ticket.collectAsState()
    var ticketLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            model.getTicket()
        } catch (e: Exception) {
            println("Error loading ticket: $e")
        } finally {
            ticketLoaded = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ScreenHeader(title = "Your Ticket")

        if (!ticketLoaded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
                Text(
                    "Loading ticket details…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val t = ticket
            if (t != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    LabeledRow("Departure", t.departure)
                    HorizontalDivider()
                    LabeledRow("Arrival", t.arrival)
                    HorizontalDivider()
                    LabeledRow("Price", "%.2f".format(t.price))
                }
            } else {
                Text(
                    "No ticket found yet. Purchase a flight to receive one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
