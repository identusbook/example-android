package com.identusbook.flighttix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.identusbook.flighttix.model.Passport
import com.identusbook.flighttix.ui.components.AsyncPrimaryButton
import com.identusbook.flighttix.ui.components.DateField
import com.identusbook.flighttix.ui.components.ScreenHeader
import com.identusbook.flighttix.ui.components.SecondaryButton
import kotlinx.coroutines.delay
import java.util.Date

/**
 * Passport registration modal — port of iOS `RegisterScreen.swift`.
 * Shows a form; on submit issues a Passport VC, waits 20s for the DIDComm round-trip,
 * self-presents to confirm, then shows a success state.
 */
@Composable
fun RegisterScreen(
    onDismiss: () -> Unit,
    model: RegisterViewModel = viewModel()
) {
    val isIssuerReady by model.isIssuerReady.collectAsState()
    var name by remember { mutableStateOf("") }
    var passportNumber by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf(Date()) }
    var registered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { model.confirmIssuerReady() }

    if (registered) {
        // Success view
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF34C759),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.size(16.dp))
            Text("Passport Issued", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(8.dp))
            Text(
                "Your passport credential is now in your wallet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.weight(1f))
            SecondaryButton(text = "Done", onClick = onDismiss)
        }
        return
    }

    // Registration form
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "Create Passport",
            subtitle = "We'll issue a passport credential to your wallet."
        )

        if (!isIssuerReady) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
                Text(
                    "Preparing issuer… the form will enable once it's ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text("Passport Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = passportNumber,
            onValueChange = { passportNumber = it },
            label = { Text("Passport Number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        DateField(label = "Birthdate", date = dob, onDateChange = { dob = it })

        Spacer(Modifier.size(4.dp))

        AsyncPrimaryButton(
            text = "Submit",
            enabled = isIssuerReady,
            action = {
                // Local validation: both name and passportNumber must be > 1 character.
                if (name.length > 1 && passportNumber.length > 1) {
                    model.register(
                        Passport(
                            name = name,
                            did = null,
                            passportNumber = passportNumber,
                            dob = dob,
                            dateOfIssuance = null
                        )
                    )
                    // Wait for the DIDComm round-trip to settle (iOS hard delay).
                    delay(20_000)
                    model.requestProof()
                    registered = true
                }
            }
        )
        SecondaryButton(text = "Close", onClick = onDismiss)
    }
}
