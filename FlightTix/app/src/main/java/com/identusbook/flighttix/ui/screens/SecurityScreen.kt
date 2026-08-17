package com.identusbook.flighttix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.identusbook.flighttix.ui.components.AsyncPrimaryButton
import com.identusbook.flighttix.ui.components.ScreenHeader

/** Airport Security tab — port of iOS `SecurityView.swift`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(model: SecurityViewModel = viewModel()) {
    val requestState by model.requestState.collectAsState()
    val proofUnderReview by model.proofUnderReview.collectAsState()
    val presentations by model.presentations.collectAsState()

    LaunchedEffect(Unit) {
        try {
            model.loadPresentations()
        } catch (_: Exception) {
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "Airport Security",
            subtitle = "Request and verify a traveller's credentials."
        )

        AsyncPrimaryButton(
            text = "Request Proof of Ticket",
            enabled = !model.isBusy,
            action = { model.requestProofOfTicketAndPassport() }
        )

        val busyLabel = when (requestState) {
            is SecurityViewModel.RequestState.Requesting -> "Creating proof request…"
            is SecurityViewModel.RequestState.Awaiting -> "Waiting for wallet to present proof…"
            else -> ""
        }
        if (busyLabel.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
                Text(busyLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        (requestState as? SecurityViewModel.RequestState.Error)?.let { err ->
            Text(
                err.message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF3B30),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
        }

        if (presentations.isEmpty()) {
            Text(
                "No presentations yet.\nRequest proof of a ticket to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            )
        } else {
            Text(
                "Previous Presentations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(presentations) { row ->
                    PresentationRowView(row)
                }
            }
        }
    }

    val review = proofUnderReview
    if (review != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { /* stays open until a decision is recorded */ },
            sheetState = sheetState
        ) {
            ProofReviewSheet(
                review = review,
                onAccept = { model.accept() },
                onDeny = { model.deny() }
            )
        }
    }
}

@Composable
private fun PresentationRowView(row: SecurityViewModel.PresentationRow) {
    val statusColor = when (row.status) {
        "PresentationVerified", "PresentationAccepted" -> Color(0xFF34C759)
        "PresentationVerificationFailed", "PresentationRejected", "RequestRejected" -> Color(0xFFFF3B30)
        else -> Color(0xFFFF9500)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            Modifier
                .size(10.dp)
                .background(statusColor, CircleShape)
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.status, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                row.presentationId,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
