package com.dotagency.cactusc2.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dotagency.cactusc2.ui.components.*
import com.dotagency.cactusc2.ui.screens.devices.CactusField
import com.dotagency.cactusc2.ui.theme.*
import com.dotagency.cactusc2.viewmodel.MainViewModel

/** Device settings form: WiFi client, AP config, auth, ESPortal toggle, payload slots, danger zone. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val device by vm.selectedDevice.collectAsState()
    val status by vm.deviceStatus.collectAsState()

    // WiFi settings
    var wifiSSID by remember { mutableStateOf("") }
    var wifiPass by remember { mutableStateOf("") }

    // AP settings
    var apSSID by remember { mutableStateOf(device?.ssid ?: "") }
    var apPass by remember { mutableStateOf(device?.wifiPassword ?: "") }

    // Auth
    var adminUser by remember { mutableStateOf(device?.adminUser ?: "admin") }
    var adminPass by remember { mutableStateOf(device?.adminPassword ?: "") }

    // ESPortal
    var esportalEnabled by remember { mutableStateOf(false) }
    var esportalSSID by remember { mutableStateOf("") }
    var esportalPass by remember { mutableStateOf("") }

    // Payloads
    var payload1 by remember { mutableStateOf("") }
    var payload1Boot by remember { mutableStateOf(false) }
    var payload2 by remember { mutableStateOf("") }
    var payload2Boot by remember { mutableStateOf(false) }

    var showConfirm by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = CactusText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CactusBg, titleContentColor = CactusText
                )
            )
        },
        containerColor = CactusBg
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!status.connected) {
                CactusCard {
                    Text("Connect to a device to modify settings", color = CactusYellow)
                }
            }

            SectionHeader("WIFI CLIENT (connect device to internet)")
            CactusField("WiFi SSID", wifiSSID) { wifiSSID = it }
            CactusField("WiFi Password", wifiPass) { wifiPass = it }

            SectionHeader("ACCESS POINT (device's own AP)")
            CactusField("AP SSID", apSSID) { apSSID = it }
            CactusField("AP Password", apPass) { apPass = it }

            SectionHeader("AUTHENTICATION")
            CactusField("Admin Username", adminUser) { adminUser = it }
            CactusField("Admin Password", adminPass) { adminPass = it }

            SectionHeader("ESPORTAL (captive portal)")
            Row {
                Text("Enable ESPortal", color = CactusText, modifier = Modifier.weight(1f))
                Switch(
                    checked = esportalEnabled,
                    onCheckedChange = { esportalEnabled = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = CactusAccent)
                )
            }
            if (esportalEnabled) {
                CactusField("Portal SSID", esportalSSID) { esportalSSID = it }
                CactusField("Portal Password", esportalPass) { esportalPass = it }
            }

            SectionHeader("PAYLOAD SLOTS")
            CactusField("Payload Slot 1", payload1) { payload1 = it }
            Row {
                Text("Run on boot", color = CactusTextDim, modifier = Modifier.weight(1f))
                Switch(
                    checked = payload1Boot,
                    onCheckedChange = { payload1Boot = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = CactusGreen)
                )
            }
            CactusField("Payload Slot 2", payload2) { payload2 = it }
            Row {
                Text("Run on boot", color = CactusTextDim, modifier = Modifier.weight(1f))
                Switch(
                    checked = payload2Boot,
                    onCheckedChange = { payload2Boot = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = CactusGreen)
                )
            }

            // Save button
            CactusButton(
                text = "Save Settings to Device",
                onClick = {
                    vm.submitDeviceSettings(buildMap {
                        put("wifissid", wifiSSID)
                        put("wifipass", wifiPass)
                        put("apssid", apSSID)
                        put("appass", apPass)
                        put("adminuser", adminUser)
                        put("adminpass", adminPass)
                        put("esportal", if (esportalEnabled) "1" else "0")
                        put("esportalssid", esportalSSID)
                        put("esportalpass", esportalPass)
                        put("payload1", payload1)
                        put("payload1boot", if (payload1Boot) "1" else "0")
                        put("payload2", payload2)
                        put("payload2boot", if (payload2Boot) "1" else "0")
                    })
                },
                icon = Icons.Filled.Save,
                color = CactusGreen,
                modifier = Modifier.fillMaxWidth(),
                enabled = status.connected
            )

            // Danger zone
            SectionHeader("DANGER ZONE")
            CactusCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CactusButton(
                        text = "Restore Defaults",
                        onClick = { showConfirm = "restore" },
                        icon = Icons.Filled.RestoreFromTrash,
                        color = CactusYellow,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = status.connected
                    )
                    CactusButton(
                        text = "Format SPIFFS",
                        onClick = { showConfirm = "format" },
                        icon = Icons.Filled.DeleteForever,
                        color = CactusRed,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = status.connected
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    showConfirm?.let { action ->
        AlertDialog(
            onDismissRequest = { showConfirm = null },
            title = { Text("Confirm", color = CactusText) },
            text = {
                Text(
                    when (action) {
                        "restore" -> "This will restore all settings to factory defaults. Continue?"
                        "format" -> "This will erase ALL data on the device SPIFFS. This cannot be undone. Continue?"
                        else -> ""
                    },
                    color = CactusTextDim
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (action) {
                        "restore" -> vm.restoreDefaults()
                        "format" -> vm.formatSpiffs()
                    }
                    showConfirm = null
                }) {
                    Text("Yes, do it", color = CactusRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = null }) {
                    Text("Cancel", color = CactusTextDim)
                }
            },
            containerColor = CactusSurface
        )
    }
}
