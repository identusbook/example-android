package com.identusbook.flighttix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.identusbook.flighttix.model.Flight
import com.identusbook.flighttix.model.Passport
import com.identusbook.flighttix.ui.components.AsyncPrimaryButton
import com.identusbook.flighttix.ui.components.AsyncSecondaryButton
import com.identusbook.flighttix.ui.components.ScreenHeader
import kotlinx.coroutines.delay
import java.util.Date

/** Dev Utils tab — port of iOS `DevUtils.swift`. */
@Composable
fun DevUtilsScreen(model: DevUtilsViewModel = viewModel()) {
    var confirmation by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "Dev Utils",
            subtitle = "Issue test credentials and manage the agent."
        )

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncPrimaryButton(
                text = "Issue Passport",
                action = {
                    model.issuePassport(
                        Passport(
                            name = "Jon Bauer",
                            did = "did:example:123",
                            passportNumber = "12345",
                            dob = Date(),
                            dateOfIssuance = null
                        )
                    )
                    delay(30_000)
                    model.requestProofOfPassport()
                    confirmation = "Passport credential issued."
                }
            )
            AsyncPrimaryButton(
                text = "Issue Ticket",
                action = {
                    val flight = Flight(departure = "SFO", arrival = "TYO", price = 700.0)
                    model.issueTicket(flight)
                    delay(30_000)
                    model.requestProofOfTicket()
                    confirmation = "Ticket credential issued."
                }
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncSecondaryButton(
                    text = "Start Up",
                    modifier = Modifier.weight(1f),
                    action = { model.startUp() }
                )
                AsyncSecondaryButton(
                    text = "Stop",
                    modifier = Modifier.weight(1f),
                    action = { model.stop() }
                )
            }
            AsyncSecondaryButton(
                text = "Reset Wallet",
                tint = Color(0xFFFF3B30),
                action = { model.tearDown() }
            )
        }
    }

    confirmation?.let { message ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text("Done") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { confirmation = null }) { Text("OK") }
            }
        )
    }
}
