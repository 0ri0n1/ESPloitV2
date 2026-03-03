package com.dotagency.cactusc2.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotagency.cactusc2.ui.components.*
import com.dotagency.cactusc2.ui.theme.*
import com.dotagency.cactusc2.viewmodel.MainViewModel

/** Dashboard showing device connection status, SPIFFS usage, firmware version, and quick-action grid. */
@Composable
fun DashboardScreen(
    vm: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val device by vm.selectedDevice.collectAsState()
    val status by vm.deviceStatus.collectAsState()
    val isConnecting by vm.isConnecting.collectAsState()

    LaunchedEffect(device) { vm.refreshStatus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Device header
        CactusCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(status.connected)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            device?.name ?: "No Device",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = CactusText
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (status.connected) "${device?.ipAddress ?: ""} · Connected"
                        else "Not connected",
                        color = CactusTextDim,
                        fontSize = 13.sp
                    )
                }
                if (device != null) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp,
                            color = CactusAccent
                        )
                    } else {
                        IconButton(onClick = {
                            device?.let { vm.connectToDevice(it) }
                        }) {
                            Icon(
                                Icons.Filled.Wifi,
                                "Connect",
                                tint = if (status.connected) CactusGreen else CactusTextDim
                            )
                        }
                    }
                }
            }
        }

        if (status.connected) {
            // Storage card
            CactusCard {
                Text("SPIFFS Storage", fontWeight = FontWeight.Bold, color = CactusText)
                Spacer(Modifier.height(8.dp))
                @Suppress("DEPRECATION")
                LinearProgressIndicator(
                    progress = status.spiffsPercent / 100f,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (status.spiffsPercent > 80) CactusRed else CactusAccent,
                    trackColor = CactusBg,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${status.spiffsUsed} / ${status.spiffsTotal} (${status.spiffsPercent}%)",
                    color = CactusTextDim,
                    fontSize = 13.sp
                )
            }

            // Firmware
            CactusCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Memory, null, tint = CactusAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Firmware", fontWeight = FontWeight.Bold, color = CactusText)
                    Spacer(Modifier.weight(1f))
                    Text(status.firmwareVersion, color = CactusTextDim, fontSize = 13.sp)
                }
            }
        }

        // Quick actions
        SectionHeader("QUICK ACTIONS")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionCard("Live\nPayload", Icons.Filled.Terminal, Modifier.weight(1f)) {
                onNavigate("livepayload")
            }
            QuickActionCard("Payloads", Icons.Filled.Folder, Modifier.weight(1f)) {
                onNavigate("payloads")
            }
            QuickActionCard("Input\nMode", Icons.Filled.Keyboard, Modifier.weight(1f)) {
                onNavigate("inputmode")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionCard("Exfil\nData", Icons.Filled.Download, Modifier.weight(1f)) {
                onNavigate("exfiltration")
            }
            QuickActionCard("Settings", Icons.Filled.Settings, Modifier.weight(1f)) {
                onNavigate("settings")
            }
            QuickActionCard("Firmware", Icons.Filled.SystemUpdate, Modifier.weight(1f)) {
                onNavigate("firmware")
            }
        }

        // Device actions
        if (status.connected) {
            SectionHeader("DEVICE ACTIONS")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CactusOutlinedButton("Reboot", onClick = { vm.rebootDevice() }, icon = Icons.Filled.RestartAlt)
                CactusOutlinedButton("Refresh", onClick = { vm.refreshStatus() }, icon = Icons.Filled.Refresh)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CactusSurface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = CactusAccent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 12.sp, color = CactusText, lineHeight = 14.sp)
        }
    }
}
