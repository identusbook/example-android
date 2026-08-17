package com.identusbook.flighttix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.identusbook.flighttix.ui.components.AsyncPrimaryButton
import com.identusbook.flighttix.ui.components.AsyncSecondaryButton

private val Green = Color(0xFF34C759)
private val Red = Color(0xFFFF3B30)

/** Bottom-sheet content for the officer to accept/deny — port of iOS `ProofReviewSheet.swift`. */
@Composable
fun ProofReviewSheet(
    review: SecurityViewModel.ProofReview,
    onAccept: suspend () -> Unit,
    onDeny: suspend () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "Proof of Ticket",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ValidityRow(title = "Ticket", valid = review.ticketValid, status = review.ticketStatus)
            HorizontalDivider()
            ValidityRow(title = "Passport", valid = review.passportValid, status = review.passportStatus)
        }

        if (!review.allValid) {
            Text(
                "One or more credentials could not be verified.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AsyncSecondaryButton(
                text = "Deny",
                tint = Red,
                modifier = Modifier.weight(1f),
                action = onDeny
            )
            AsyncPrimaryButton(
                text = "Accept",
                enabled = review.allValid,
                modifier = Modifier.weight(1f),
                action = onAccept
            )
        }
    }
}

@Composable
private fun ValidityRow(title: String, valid: Boolean, status: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            imageVector = if (valid) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            tint = if (valid) Green else Red,
            modifier = Modifier.size(28.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                if (valid) "Valid" else "Invalid · $status",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
    }
}
