package com.dotagency.cactusc2.ui.screens.exfiltration

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotagency.cactusc2.ui.components.*
import com.dotagency.cactusc2.ui.theme.*
import com.dotagency.cactusc2.viewmodel.MainViewModel

/** Exfiltrated data viewer — lists files on SPIFFS written by payloads, tap to view contents. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExfiltrationScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val files by vm.exfilFiles.collectAsState()
    val content by vm.exfilContent.collectAsState()
    val device by vm.selectedDevice.collectAsState()
    val status by vm.deviceStatus.collectAsState()
    var selectedFile by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(device, status.connected) {
        if (status.connected) vm.refreshExfilData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedFile != null) selectedFile!! else "Exfiltrated Data",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedFile != null) selectedFile = null else onBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = CactusText)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refreshExfilData() }) {
                        Icon(Icons.Filled.Refresh, "Refresh", tint = CactusAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CactusBg, titleContentColor = CactusText
                )
            )
        },
        containerColor = CactusBg
    ) { padding ->
        if (selectedFile != null) {
            // File content view
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                CactusCard {
                    Text(
                        content.ifEmpty { "Loading..." },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = CactusText
                    )
                }
            }
        } else if (!status.connected) {
            EmptyState(
                Icons.Filled.WifiOff, "Not Connected",
                "Connect to a device to view exfiltrated data",
                modifier = Modifier.padding(padding)
            )
        } else if (files.isEmpty()) {
            EmptyState(
                Icons.Filled.FolderOff, "No Data",
                "No exfiltrated data on this device",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files, key = { it.name }) { file ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedFile = file.name
                                vm.loadExfilFile(file.name)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CactusSurface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.InsertDriveFile, null, tint = CactusYellow, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(file.name, color = CactusText, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Filled.ChevronRight, null, tint = CactusTextDim)
                        }
                    }
                }
            }
        }
    }
}
