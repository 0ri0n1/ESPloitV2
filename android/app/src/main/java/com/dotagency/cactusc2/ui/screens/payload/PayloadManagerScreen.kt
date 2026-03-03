package com.dotagency.cactusc2.ui.screens.payload

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotagency.cactusc2.data.model.Payload
import com.dotagency.cactusc2.ui.components.*
import com.dotagency.cactusc2.ui.theme.*
import com.dotagency.cactusc2.viewmodel.MainViewModel

/** Tabbed payload manager: Local (Room DB) and Device (SPIFFS) payloads with run/upload/delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayloadManagerScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val localPayloads by vm.localPayloads.collectAsState()
    val remotePayloads by vm.remotePayloads.collectAsState()
    val device by vm.selectedDevice.collectAsState()
    val status by vm.deviceStatus.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(device, status.connected) {
        if (status.connected) vm.refreshRemotePayloads()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payloads", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = CactusText)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "New payload", tint = CactusAccent)
                    }
                    if (status.connected) {
                        IconButton(onClick = { vm.refreshRemotePayloads() }) {
                            Icon(Icons.Filled.Refresh, "Refresh", tint = CactusAccent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CactusBg, titleContentColor = CactusText
                )
            )
        },
        containerColor = CactusBg
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CactusSurface,
                contentColor = CactusAccent
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Local", modifier = Modifier.padding(12.dp), color = if (selectedTab == 0) CactusAccent else CactusTextDim)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Device", modifier = Modifier.padding(12.dp), color = if (selectedTab == 1) CactusAccent else CactusTextDim)
                }
            }

            when (selectedTab) {
                0 -> LocalPayloadList(localPayloads, vm)
                1 -> RemotePayloadList(remotePayloads, vm, status.connected)
            }
        }
    }

    if (showAddDialog) {
        NewPayloadDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, content ->
                vm.saveLocalPayload(Payload(name = name, content = content))
                showAddDialog = false
            }
        )
    }
}

@Composable
fun LocalPayloadList(payloads: List<Payload>, vm: MainViewModel) {
    if (payloads.isEmpty()) {
        EmptyState(Icons.Filled.Folder, "No Local Payloads", "Create payloads to store them locally")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(payloads, key = { it.id }) { payload ->
                PayloadCard(
                    name = payload.name,
                    subtitle = "${payload.content.lines().size} lines",
                    onRun = { vm.runLivePayload(payload.content) },
                    onUpload = { vm.uploadPayload(payload.name, payload.content) },
                    onDelete = { vm.deleteLocalPayload(payload) }
                )
            }
        }
    }
}

@Composable
fun RemotePayloadList(payloads: List<com.dotagency.cactusc2.data.model.PayloadInfo>, vm: MainViewModel, connected: Boolean) {
    if (!connected) {
        EmptyState(Icons.Filled.WifiOff, "Not Connected", "Connect to a device to see its payloads")
    } else if (payloads.isEmpty()) {
        EmptyState(Icons.Filled.Folder, "No Device Payloads", "Upload payloads from the Local tab")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(payloads, key = { it.name }) { payload ->
                PayloadCard(
                    name = payload.name,
                    subtitle = payload.size,
                    onRun = { vm.runRemotePayload(payload.name) },
                    onUpload = null,
                    onDelete = { vm.deleteRemotePayload(payload.name) }
                )
            }
        }
    }
}

@Composable
fun PayloadCard(
    name: String,
    subtitle: String,
    onRun: () -> Unit,
    onUpload: (() -> Unit)?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CactusSurface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Description, null, tint = CactusAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Medium, color = CactusText, fontSize = 14.sp)
                Text(subtitle, color = CactusTextDim, fontSize = 12.sp)
            }
            IconButton(onClick = onRun) {
                Icon(Icons.Filled.PlayArrow, "Run", tint = CactusGreen)
            }
            if (onUpload != null) {
                IconButton(onClick = onUpload) {
                    Icon(Icons.Filled.Upload, "Upload", tint = CactusAccent)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete", tint = CactusRed.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun NewPayloadDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Payload", color = CactusText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CactusAccent, unfocusedBorderColor = CactusTextDim.copy(alpha = 0.3f),
                        focusedTextColor = CactusText, unfocusedTextColor = CactusText,
                        cursorColor = CactusAccent, focusedLabelColor = CactusAccent, unfocusedLabelColor = CactusTextDim
                    )
                )
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    label = { Text("Script") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = CactusText),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CactusAccent, unfocusedBorderColor = CactusTextDim.copy(alpha = 0.3f),
                        cursorColor = CactusGreen, focusedLabelColor = CactusAccent, unfocusedLabelColor = CactusTextDim,
                        focusedContainerColor = CactusSurface, unfocusedContainerColor = CactusSurface
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim().ifEmpty { "untitled.txt" }, content) }) {
                Text("Save", color = CactusGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = CactusTextDim) }
        },
        containerColor = CactusSurface
    )
}
