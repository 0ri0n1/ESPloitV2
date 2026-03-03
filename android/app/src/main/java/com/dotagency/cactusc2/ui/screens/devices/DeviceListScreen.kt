package com.dotagency.cactusc2.ui.screens.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotagency.cactusc2.data.model.Device
import com.dotagency.cactusc2.ui.components.*
import com.dotagency.cactusc2.ui.theme.*
import com.dotagency.cactusc2.viewmodel.MainViewModel

/** Device registry list with connect/edit/delete actions and FAB to add new devices. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    vm: MainViewModel,
    onDeviceSelected: (Device) -> Unit,
    onAddDevice: () -> Unit,
    onEditDevice: (Device) -> Unit
) {
    val devices by vm.devices.collectAsState()
    val selectedDevice by vm.selectedDevice.collectAsState()
    val isConnecting by vm.isConnecting.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Devices", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CactusBg,
                    titleContentColor = CactusText
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddDevice,
                containerColor = CactusAccent,
                contentColor = CactusBg
            ) {
                Icon(Icons.Filled.Add, "Add device")
            }
        },
        containerColor = CactusBg
    ) { padding ->
        if (devices.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Router,
                title = "No Devices",
                subtitle = "Tap + to add your first Cactus WHID",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        isSelected = device.id == selectedDevice?.id,
                        isConnecting = isConnecting && device.id == selectedDevice?.id,
                        onSelect = {
                            vm.selectDevice(device)
                            onDeviceSelected(device)
                        },
                        onConnect = { vm.connectToDevice(device) },
                        onEdit = { onEditDevice(device) },
                        onSetDefault = { vm.setDefaultDevice(device) },
                        onDelete = { vm.deleteDevice(device) }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: Device,
    isSelected: Boolean,
    isConnecting: Boolean,
    onSelect: () -> Unit,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CactusSurface.copy(alpha = 0.9f) else CactusSurface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
            width = 1.dp
        ) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        fontWeight = FontWeight.Bold,
                        color = CactusText,
                        fontSize = 16.sp
                    )
                    if (device.isDefault) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CactusAccent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "DEFAULT",
                                fontSize = 10.sp,
                                color = CactusAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${device.ssid} · ${device.ipAddress}",
                    color = CactusTextDim,
                    fontSize = 13.sp
                )
            }

            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = CactusAccent
                )
            } else {
                IconButton(onClick = onConnect) {
                    Icon(Icons.Filled.Wifi, "Connect", tint = CactusAccent)
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, "More", tint = CactusTextDim)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { showMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Filled.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Set as Default") },
                        onClick = { showMenu = false; onSetDefault() },
                        leadingIcon = { Icon(Icons.Filled.Star, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = CactusRed) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = CactusRed) }
                    )
                }
            }
        }
    }
}
