package com.identusbook.flighttix.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.identusbook.flighttix.R
import com.identusbook.flighttix.ui.components.SecondaryButton

/**
 * Splash / bootstrap screen — port of iOS `LoadingScreen.swift`.
 * Shows the current Identus [statusText] and a dev "Tear Down and Stop" affordance.
 */
@Composable
fun LoadingScreen(
    statusText: String,
    onTearDownAndStop: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))
        Text("FlightTix", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("powered by", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(12.dp))
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.identus_logo),
                contentDescription = "Identus",
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.size(12.dp))
            Text(
                "Identus",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color(0xFF0A2540)
            )
        }
        Spacer(Modifier.size(24.dp))
        CircularProgressIndicator()
        Spacer(Modifier.size(12.dp))
        Text(
            statusText,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.size(24.dp))
        SecondaryButton(
            text = "Tear Down and Stop",
            modifier = Modifier.padding(horizontal = 48.dp),
            onClick = onTearDownAndStop
        )
        Spacer(Modifier.weight(1f))
        Text(
            "Identus Kotlin SDK: v4.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
