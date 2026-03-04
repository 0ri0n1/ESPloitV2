package com.dotagency.cactusc2.ui.screens.inputmode

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
import com.dotagency.cactusc2.ui.components.*
import com.dotagency.cactusc2.ui.theme.*
import com.dotagency.cactusc2.viewmodel.MainViewModel

/** Real-time keyboard input mode: type text, press quick-keys, or send modifier combos to the Cactus. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputModeScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    var textToType by remember { mutableStateOf("") }
    val device by vm.selectedDevice.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Input Mode", fontWeight = FontWeight.Bold) },
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
            Text("Target: ${device?.name ?: "None"}", color = CactusTextDim, fontSize = 13.sp)

            // Type text
            SectionHeader("TYPE TEXT")
            OutlinedTextField(
                value = textToType,
                onValueChange = { textToType = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Text to type on target...", color = CactusTextDim) },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = CactusText),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CactusAccent,
                    unfocusedBorderColor = CactusTextDim.copy(alpha = 0.3f),
                    cursorColor = CactusGreen,
                    focusedContainerColor = CactusSurface,
                    unfocusedContainerColor = CactusSurface
                )
            )
            CactusButton(
                text = "Type It",
                onClick = {
                    vm.sendKeys("Print:$textToType")
                    textToType = ""
                },
                icon = Icons.Filled.Keyboard,
                modifier = Modifier.fillMaxWidth(),
                enabled = textToType.isNotBlank()
            )

            // Quick keys — ESPloit native format (decimal keycodes)
            SectionHeader("QUICK KEYS")
            // ESPloit keycodes: ENTER=176, TAB=179, ESC=177, SPACE=32, BACKSPACE=178, DELETE=212
            val quickKeys = listOf(
                "ENTER" to "Press:176",
                "TAB" to "Press:179",
                "ESCAPE" to "Press:177",
                "SPACE" to "Press:32",
                "BACKSPACE" to "Press:178",
                "DELETE" to "Press:212",
            )
            quickKeys.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (label, esploitCmd) ->
                        CactusButton(
                            text = label,
                            onClick = { vm.sendKeys(esploitCmd) },
                            modifier = Modifier.weight(1f),
                            color = CactusText
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            // Modifier combos — ESPloit native format
            SectionHeader("MODIFIER COMBOS")
            // Arduino Keyboard.h keycodes: GUI=131, ALT=130, CTRL=128, SHIFT=129
            // Letters: ASCII decimal (r=114, c=99, v=118, l=108)
            // F4=197 (0xC5), DELETE=212, ESC=177
            val combos = listOf(
                "Press:131+114" to "Win+R (Run)",
                "Press:131" to "Windows Key",
                "Press:130+197" to "Close Window",
                "Press:128+99" to "Copy",
                "Press:128+118" to "Paste",
                "Press:128+130+212" to "Ctrl+Alt+Del",
                "Press:128+129+177" to "Task Manager",
                "Press:131+108" to "Lock Screen"
            )
            combos.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (cmd, label) ->
                        Card(
                            onClick = { vm.sendKeys(cmd) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = CactusSurface)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(cmd, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CactusAccent, fontFamily = FontFamily.Monospace)
                                Text(label, fontSize = 10.sp, color = CactusTextDim)
                            }
                        }
                    }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            // Arrow keys — ESPloit keycodes: UP=218, DOWN=217, LEFT=216, RIGHT=215
            SectionHeader("ARROW KEYS")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.weight(1f))
                CactusButton("UP", onClick = { vm.sendKeys("Press:218") })
                Spacer(Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CactusButton("LEFT", onClick = { vm.sendKeys("Press:216") })
                Spacer(Modifier.width(8.dp))
                CactusButton("DOWN", onClick = { vm.sendKeys("Press:217") })
                Spacer(Modifier.width(8.dp))
                CactusButton("RIGHT", onClick = { vm.sendKeys("Press:215") })
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
