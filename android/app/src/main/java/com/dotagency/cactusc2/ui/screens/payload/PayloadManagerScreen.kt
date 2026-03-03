package com.dotagency.cactusc2.ui.screens.payload

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
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

    // Editor state: non-null = editing/creating a payload
    var editingPayload by remember { mutableStateOf<Payload?>(null) }
    var editorName by remember { mutableStateOf("") }
    var editorDescription by remember { mutableStateOf("") }
    var editorContent by remember { mutableStateOf("") }

    // Open editor for a payload (existing or new blank)
    fun openEditor(payload: Payload) {
        editingPayload = payload
        editorName = payload.name
        editorDescription = payload.description
        editorContent = payload.content
    }

    fun closeEditor() {
        editingPayload = null
    }

    LaunchedEffect(device, status.connected) {
        if (status.connected) vm.refreshRemotePayloads()
    }

    val isEditing = editingPayload != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) editorName.ifEmpty { "New Payload" } else "Payloads",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditing) closeEditor() else onBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = CactusText)
                    }
                },
                actions = {
                    if (isEditing) {
                        // Save button in toolbar
                        IconButton(onClick = {
                            val p = editingPayload ?: return@IconButton
                            val updated = p.copy(
                                name = editorName.trim().ifEmpty { "untitled.txt" },
                                description = editorDescription.trim(),
                                content = editorContent,
                                updatedAt = System.currentTimeMillis()
                            )
                            vm.saveLocalPayload(updated)
                            closeEditor()
                        }) {
                            Icon(Icons.Filled.Save, "Save", tint = CactusGreen)
                        }
                    } else {
                        // New payload button
                        IconButton(onClick = {
                            openEditor(Payload(name = "", content = ""))
                        }) {
                            Icon(Icons.Filled.Add, "New payload", tint = CactusAccent)
                        }
                        if (status.connected) {
                            IconButton(onClick = { vm.refreshRemotePayloads() }) {
                                Icon(Icons.Filled.Refresh, "Refresh", tint = CactusAccent)
                            }
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
        if (isEditing) {
            PayloadEditor(
                name = editorName,
                onNameChange = { editorName = it },
                description = editorDescription,
                onDescriptionChange = { editorDescription = it },
                content = editorContent,
                onContentChange = { editorContent = it },
                onSave = {
                    val p = editingPayload ?: return@PayloadEditor
                    val updated = p.copy(
                        name = editorName.trim().ifEmpty { "untitled.txt" },
                        description = editorDescription.trim(),
                        content = editorContent,
                        updatedAt = System.currentTimeMillis()
                    )
                    vm.saveLocalPayload(updated)
                    closeEditor()
                },
                onRun = { vm.runLivePayload(editorContent) },
                onUpload = if (status.connected) {
                    { vm.uploadPayload(editorName.trim().ifEmpty { "untitled.txt" }, editorContent) }
                } else null,
                canRun = editorContent.isNotBlank() && device != null,
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(modifier = Modifier.padding(padding)) {
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
                    0 -> LocalPayloadList(localPayloads, vm, onEdit = { openEditor(it) })
                    1 -> RemotePayloadList(remotePayloads, vm, status.connected)
                }
            }
        }
    }
}

// ── Editor ──

@Composable
fun PayloadEditor(
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onRun: () -> Unit,
    onUpload: (() -> Unit)?,
    canRun: Boolean,
    modifier: Modifier = Modifier
) {
    val editorColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CactusAccent,
        unfocusedBorderColor = CactusTextDim.copy(alpha = 0.3f),
        focusedTextColor = CactusText,
        unfocusedTextColor = CactusText,
        cursorColor = CactusGreen,
        focusedLabelColor = CactusAccent,
        unfocusedLabelColor = CactusTextDim,
        focusedContainerColor = CactusSurface,
        unfocusedContainerColor = CactusSurface
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Payload Name") },
            placeholder = { Text("e.g. win-rickroll.txt", color = CactusTextDim) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = editorColors
        )

        // Description field
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = editorColors
        )

        // Quick insert chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("DELAY 500" to "DELAY 500\n", "STRING " to "STRING ", "ENTER" to "ENTER\n", "GUI r" to "GUI r\n").forEach { (label, insert) ->
                AssistChip(
                    onClick = { onContentChange(content + insert) },
                    label = { Text(label, fontSize = 11.sp, color = CactusAccent) },
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, CactusAccent.copy(alpha = 0.3f)),
                    colors = AssistChipDefaults.assistChipColors(containerColor = CactusSurface)
                )
            }
        }

        // Script editor — takes remaining vertical space
        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            label = { Text("DuckyScript") },
            placeholder = { Text("REM Your payload here\nDELAY 1000\nGUI r\n...", color = CactusTextDim) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = CactusText
            ),
            shape = RoundedCornerShape(8.dp),
            colors = editorColors
        )

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CactusButton(
                text = "Save",
                icon = Icons.Filled.Save,
                onClick = onSave,
                color = CactusGreen,
                modifier = Modifier.weight(1f)
            )
            CactusOutlinedButton(
                text = "Run",
                icon = Icons.Filled.PlayArrow,
                onClick = { if (canRun) onRun() },
                color = if (canRun) CactusGreen else CactusTextDim,
                modifier = Modifier.weight(1f)
            )
            if (onUpload != null) {
                CactusOutlinedButton(
                    text = "Upload",
                    icon = Icons.Filled.Upload,
                    onClick = onUpload,
                    color = CactusAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── List views ──

@Composable
fun LocalPayloadList(payloads: List<Payload>, vm: MainViewModel, onEdit: (Payload) -> Unit) {
    if (payloads.isEmpty()) {
        EmptyState(Icons.Filled.Folder, "No Local Payloads", "Tap + to create a payload")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(payloads, key = { it.id }) { payload ->
                PayloadCard(
                    name = payload.name,
                    subtitle = "[${payload.description.take(40)}] ${payload.content.lines().size} lines",
                    onClick = { onEdit(payload) },
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
                    onClick = null,
                    onRun = { vm.runRemotePayload(payload.name) },
                    onUpload = null,
                    onDelete = { vm.deleteRemotePayload(payload.name) }
                )
            }
        }
    }
}

// ── Card ──

@Composable
fun PayloadCard(
    name: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    onRun: () -> Unit,
    onUpload: (() -> Unit)?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
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
                Text(subtitle, color = CactusTextDim, fontSize = 12.sp, maxLines = 1)
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
