package com.identusbook.flighttix.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.identusbook.flighttix.agent.Identus
import com.identusbook.flighttix.agent.IdentusStatus
import com.identusbook.flighttix.agent.IdentusStatusState
import com.identusbook.flighttix.auth.Auth
import com.identusbook.flighttix.ui.screens.DevUtilsScreen
import com.identusbook.flighttix.ui.screens.LoadingScreen
import com.identusbook.flighttix.ui.screens.ProfileScreen
import com.identusbook.flighttix.ui.screens.PurchaseScreen
import com.identusbook.flighttix.ui.screens.RegisterScreen
import com.identusbook.flighttix.ui.screens.SecurityScreen
import com.identusbook.flighttix.ui.screens.TicketScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ViewState { LOADING, TABS }

private enum class NavItem(val label: String) {
    PURCHASE("Purchase"),
    TICKET("Ticket"),
    SECURITY("Airport Security"),
    DEV_TOOLS("Dev Utils")
}

/**
 * Root navigation + state machine — port of iOS `ContentView.swift`.
 * Three top-level states collapse to two (loading, tabs); "login" is a modal over tabs.
 */
@Composable
fun ContentView() {
    val status by IdentusStatus.status.collectAsState()
    var viewState by remember { mutableStateOf(ViewState.LOADING) }
    var selectedTab by remember { mutableStateOf(NavItem.PURCHASE) }
    val modalManager = remember { ModalManager() }
    val scope = rememberCoroutineScope()

    // Bootstrap the edge agent once (Identus.startUpAndConnect()). Runs on IO internally;
    // any failure (e.g. unreachable mediator) surfaces as an Error status on the Loading
    // screen rather than crashing.
    LaunchedEffect(Unit) {
        try {
            Identus.startUpAndConnect()
        } catch (e: Throwable) {
            println("startUpAndConnect failed: $e")
            IdentusStatus.set(IdentusStatusState.Error(e.message ?: e.toString()))
        }
    }

    // When status reaches Ready, wait ~2s (cosmetic) then show the tabs.
    LaunchedEffect(status) {
        if (status is IdentusStatusState.Ready && viewState == ViewState.LOADING) {
            delay(2000)
            viewState = ViewState.TABS
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (viewState) {
            ViewState.LOADING -> LoadingScreen(
                statusText = status.description,
                onTearDownAndStop = {
                    scope.launch {
                        try {
                            Identus.tearDown()
                            Identus.stop()
                        } catch (e: Exception) {
                            println("Tear down failed: $e")
                        }
                    }
                }
            )

            ViewState.TABS -> {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavItem.entries.forEach { item ->
                                NavigationBarItem(
                                    selected = selectedTab == item,
                                    onClick = { selectedTab = item },
                                    icon = {
                                        Icon(
                                            imageVector = when (item) {
                                                NavItem.PURCHASE -> Icons.Filled.Flight
                                                NavItem.TICKET -> Icons.Filled.ConfirmationNumber
                                                NavItem.SECURITY -> Icons.Filled.PanTool
                                                NavItem.DEV_TOOLS -> Icons.Filled.Build
                                            },
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(Modifier.padding(innerPadding)) {
                        when (selectedTab) {
                            NavItem.PURCHASE -> PurchaseScreen(
                                onOpenProfile = { modalManager.show(ActiveModal.PROFILE) }
                            )
                            NavItem.TICKET -> TicketScreen()
                            NavItem.SECURITY -> SecurityScreen()
                            NavItem.DEV_TOOLS -> DevUtilsScreen()
                        }
                    }
                }

                // Login gate: on entering tabs AND on every tab switch, show Register if
                // the wallet holds no Passport VC.
                LaunchedEffect(selectedTab) {
                    if (!Auth.isLoggedIn()) {
                        modalManager.show(ActiveModal.REGISTER)
                    }
                }

                // Single active modal, rendered full-screen over the tabs.
                when (modalManager.activeModal) {
                    ActiveModal.REGISTER -> FullScreenModal {
                        RegisterScreen(onDismiss = { modalManager.dismiss() })
                    }
                    ActiveModal.PROFILE -> FullScreenModal {
                        ProfileScreen(onClose = { modalManager.dismiss() })
                    }
                    null -> {}
                }
            }
        }
    }
}

@Composable
private fun FullScreenModal(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        content()
    }
}
