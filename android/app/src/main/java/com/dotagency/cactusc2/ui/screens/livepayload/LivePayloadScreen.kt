package com.dotagency.cactusc2.ui.screens.livepayload

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

/** Live DuckyScript editor with quick-insert chips and execute button. Sends payloads to /runlivepayload. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivePayloadScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    var script by remember { mutableStateOf("") }
    val result by vm.livePayloadResult.collectAsState()
    val device by vm.selectedDevice.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Payload", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = CactusText)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        vm.saveLocalPayload(Payload(
                            name = "payload_${System.currentTimeMillis()}.txt",
                            content = script
                        ))
                    }, enabled = script.isNotBlank()) {
                        Icon(Icons.Filled.Save, "Save locally", tint = CactusAccent)
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
            Text(
                "Target: ${device?.name ?: "None"}",
                color = CactusTextDim,
                fontSize = 13.sp
            )

            // Script editor
            OutlinedTextField(
                value = script,
                onValueChange = { script = it },
                modifier = Modifier.fillMaxWidth().height(250.dp),
                placeholder = { Text("Enter DuckyScript payload...", color = CactusTextDim) },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = CactusText
                ),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CactusAccent,
                    unfocusedBorderColor = CactusTextDim.copy(alpha = 0.3f),
                    cursorColor = CactusGreen,
                    focusedContainerColor = CactusSurface,
                    unfocusedContainerColor = CactusSurface
                )
            )

            // Quick insert buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { script += "DELAY 500\n" },
                    label = { Text("DELAY", fontSize = 12.sp) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = CactusAccent)
                )
                AssistChip(
                    onClick = { script += "STRING " },
                    label = { Text("STRING", fontSize = 12.sp) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = CactusAccent)
                )
                AssistChip(
                    onClick = { script += "ENTER\n" },
                    label = { Text("ENTER", fontSize = 12.sp) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = CactusAccent)
                )
                AssistChip(
                    onClick = { script += "GUI r\n" },
                    label = { Text("GUI r", fontSize = 12.sp) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = CactusAccent)
                )
            }

            // Execute button
            CactusButton(
                text = "Execute Payload",
                onClick = { vm.runLivePayload(script) },
                icon = Icons.Filled.PlayArrow,
                color = CactusGreen,
                modifier = Modifier.fillMaxWidth(),
                enabled = script.isNotBlank() && device != null
            )

            // Result
            if (result.isNotBlank()) {
                SectionHeader("RESULT")
                CactusCard {
                    Text(
                        result,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CactusText
                    )
                }
            }

            // DuckyScript reference
            SectionHeader("QUICK REFERENCE")
            CactusCard {
                val commands = listOf(
                    "STRING <text>" to "Type text",
                    "DELAY <ms>" to "Wait in milliseconds",
                    "ENTER" to "Press Enter",
                    "GUI r" to "Win+R (Run dialog)",
                    "GUI" to "Windows key",
                    "ALT F4" to "Close window",
                    "CTRL ALT DELETE" to "Ctrl+Alt+Del",
                    "TAB" to "Tab key",
                    "UP/DOWN/LEFT/RIGHT" to "Arrow keys"
                )
                commands.forEach { (cmd, desc) ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CactusAccent,
                            modifier = Modifier.width(180.dp))
                        Text(desc, fontSize = 12.sp, color = CactusTextDim)
                    }
                }
            }
        }
    }
}
