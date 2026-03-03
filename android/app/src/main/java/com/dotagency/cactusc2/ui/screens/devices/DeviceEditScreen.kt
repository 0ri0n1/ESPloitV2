package com.dotagency.cactusc2.ui.screens.devices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dotagency.cactusc2.data.model.Device
import com.dotagency.cactusc2.ui.components.SectionHeader
import com.dotagency.cactusc2.ui.theme.*
import com.dotagency.cactusc2.viewmodel.MainViewModel

/** Add/edit form for a Cactus WHID device (name, SSID, password, IP, port, auth). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceEditScreen(
    vm: MainViewModel,
    editDevice: Device?,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(editDevice?.name ?: "") }
    var ssid by remember { mutableStateOf(editDevice?.ssid ?: "") }
    var wifiPw by remember { mutableStateOf(editDevice?.wifiPassword ?: "") }
    var ip by remember { mutableStateOf(editDevice?.ipAddress ?: "192.168.1.1") }
    var port by remember { mutableStateOf(editDevice?.httpPort?.toString() ?: "80") }
    var adminUser by remember { mutableStateOf(editDevice?.adminUser ?: "admin") }
    var adminPw by remember { mutableStateOf(editDevice?.adminPassword ?: "") }

    val isEdit = editDevice != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Device" else "Add Device", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = CactusText)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val device = Device(
                                id = editDevice?.id ?: 0,
                                name = name.trim(),
                                ssid = ssid.trim(),
                                wifiPassword = wifiPw,
                                ipAddress = ip.trim(),
                                httpPort = port.toIntOrNull() ?: 80,
                                adminUser = adminUser.trim(),
                                adminPassword = adminPw,
                                isDefault = editDevice?.isDefault ?: false
                            )
                            if (isEdit) vm.updateDevice(device) else vm.addDevice(device)
                            onBack()
                        },
                        enabled = name.isNotBlank() && ssid.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Check, "Save", tint = CactusGreen)
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
            SectionHeader("DEVICE INFO")
            CactusField("Device Name", name) { name = it }
            CactusField("WiFi SSID", ssid) { ssid = it }
            CactusField("WiFi Password", wifiPw) { wifiPw = it }

            SectionHeader("NETWORK")
            CactusField("IP Address", ip) { ip = it }
            CactusField("HTTP Port", port) { port = it }

            SectionHeader("AUTHENTICATION")
            CactusField("Admin Username", adminUser) { adminUser = it }
            CactusField("Admin Password", adminPw) { adminPw = it }
        }
    }
}

@Composable
fun CactusField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CactusAccent,
            unfocusedBorderColor = CactusTextDim.copy(alpha = 0.3f),
            focusedLabelColor = CactusAccent,
            unfocusedLabelColor = CactusTextDim,
            cursorColor = CactusAccent,
            focusedTextColor = CactusText,
            unfocusedTextColor = CactusText
        ),
        singleLine = true
    )
}
