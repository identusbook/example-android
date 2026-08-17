package com.identusbook.flighttix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.identusbook.flighttix.model.Flight
import com.identusbook.flighttix.ui.components.AsyncPrimaryButton

/** Purchase tab — port of iOS `PurchaseView.swift`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    onOpenProfile: () -> Unit,
    model: PurchaseViewModel = viewModel()
) {
    val purchaseComplete by model.purchaseComplete.collectAsState()
    val flights = model.availableFlights
    var selectedFlight by remember { mutableStateOf<Flight?>(null) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (selectedFlight == null) selectedFlight = flights.firstOrNull()
    }

    fun label(f: Flight): String =
        "${f.departure} → ${f.arrival} – $%.2f".format(f.price)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Purchase", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Buy a flight to receive a ticket credential.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onOpenProfile) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Choose Flight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedFlight?.let { label(it) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Flights") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    flights.forEach { flight ->
                        DropdownMenuItem(
                            text = { Text(label(flight)) },
                            onClick = {
                                selectedFlight = flight
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        AsyncPrimaryButton(
            text = "Purchase Ticket",
            enabled = selectedFlight != null,
            action = {
                selectedFlight?.let { model.purchaseTicket(it) }
            }
        )

        Spacer(Modifier.weight(1f))
    }

    if (purchaseComplete) {
        AlertDialog(
            onDismissRequest = { model.clearPurchaseComplete() },
            title = { Text("Ticket Issued ✈️") },
            text = { Text("Your ticket credential has been issued to your wallet. You can view it on the Ticket tab.") },
            confirmButton = {
                TextButton(onClick = { model.clearPurchaseComplete() }) { Text("OK") }
            }
        )
    }
}
