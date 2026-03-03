package com.dotagency.cactusc2.ui.screens.firmware

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotagency.cactusc2.ui.components.*
import com.dotagency.cactusc2.ui.screens.devices.CactusField
import com.dotagency.cactusc2.ui.theme.*
import com.dotagency.cactusc2.viewmodel.MainViewModel
import java.io.File

/** OTA firmware update screen: file picker for .bin upload or auto-update via URL. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirmwareScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val device by vm.selectedDevice.collectAsState()
    val status by vm.deviceStatus.collectAsState()
    var otaUrl by remember { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf<Uri?>(null) }
    var showConfirm by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedFile = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Firmware Update", fontWeight = FontWeight.Bold) },
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
            // Current firmware info
            CactusCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Memory, null, tint = CactusAccent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Current Firmware", fontWeight = FontWeight.Bold, color = CactusText)
                        Text(
                            if (status.connected) "Version: ${status.firmwareVersion}" else "Not connected",
                            color = CactusTextDim, fontSize = 13.sp
                        )
                    }
                }
            }

            // Upload .bin file
            SectionHeader("UPLOAD FIRMWARE FILE")
            CactusCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Select a .bin firmware file to flash via HTTP to port 1337",
                        color = CactusTextDim, fontSize = 13.sp
                    )
                    CactusButton(
                        text = if (selectedFile != null) "File selected" else "Choose .bin file",
                        onClick = { filePicker.launch("application/octet-stream") },
                        icon = Icons.Filled.FileOpen,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (selectedFile != null) {
                        CactusButton(
                            text = "Upload & Flash",
                            onClick = { showConfirm = true },
                            icon = Icons.Filled.Upload,
                            color = CactusGreen,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = status.connected
                        )
                    }
                }
            }

            // OTA URL
            SectionHeader("OTA UPDATE FROM URL")
            CactusCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "The device will download and flash the firmware from this URL",
                        color = CactusTextDim, fontSize = 13.sp
                    )
                    CactusField("Firmware URL", otaUrl) { otaUrl = it }
                    CactusButton(
                        text = "Start OTA Update",
                        onClick = { vm.autoUpdateFirmware(otaUrl) },
                        icon = Icons.Filled.CloudDownload,
                        color = CactusGreen,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = otaUrl.isNotBlank() && status.connected
                    )
                }
            }

            // Warning
            CactusCard {
                Row {
                    Icon(Icons.Filled.Warning, null, tint = CactusYellow, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Firmware update will reboot the device. Do not disconnect power during the update.",
                        color = CactusYellow, fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Flash Firmware?", color = CactusText) },
            text = { Text("This will upload and flash the firmware. The device will reboot.", color = CactusTextDim) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    selectedFile?.let { uri ->
                        val inputStream = context.contentResolver.openInputStream(uri)
                        if (inputStream == null) {
                            vm.snackbarMsg("Failed to read firmware file")
                            return@let
                        }
                        val tempFile = File(context.cacheDir, "firmware.bin")
                        inputStream.use { input ->
                            tempFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        vm.uploadFirmware(tempFile)
                    }
                }) {
                    Text("Flash", color = CactusGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel", color = CactusTextDim)
                }
            },
            containerColor = CactusSurface
        )
    }
}
