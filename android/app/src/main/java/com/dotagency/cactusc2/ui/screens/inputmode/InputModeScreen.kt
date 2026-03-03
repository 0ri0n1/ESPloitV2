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
                    vm.sendKeys("STRING $textToType")
                    textToType = ""
                },
                icon = Icons.Filled.Keyboard,
                modifier = Modifier.fillMaxWidth(),
                enabled = textToType.isNotBlank()
            )

            // Quick keys
            SectionHeader("QUICK KEYS")
            val quickKeys = listOf(
                "ENTER" to Icons.Filled.KeyboardReturn,
                "TAB" to Icons.Filled.KeyboardTab,
                "ESCAPE" to Icons.Filled.Cancel,
                "SPACE" to Icons.Filled.SpaceBar,
                "BACKSPACE" to Icons.Filled.Backspace,
                "DELETE" to Icons.Filled.Delete,
            )
            quickKeys.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (key, icon) ->
                        CactusButton(
                            text = key,
                            onClick = { vm.sendKeys(key) },
                            modifier = Modifier.weight(1f),
                            color = CactusText
                        )
                    }
                    // Pad remaining space if row is not full
                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            // Modifier combos
            SectionHeader("MODIFIER COMBOS")
            val combos = listOf(
                "GUI r" to "Win+R (Run)",
                "GUI" to "Windows Key",
                "ALT F4" to "Close Window",
                "CTRL c" to "Copy",
                "CTRL v" to "Paste",
                "CTRL ALT DELETE" to "Ctrl+Alt+Del",
                "CTRL SHIFT ESCAPE" to "Task Manager",
                "GUI l" to "Lock Screen"
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

            // Arrow keys
            SectionHeader("ARROW KEYS")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.weight(1f))
                CactusButton("UP", onClick = { vm.sendKeys("UP") })
                Spacer(Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CactusButton("LEFT", onClick = { vm.sendKeys("LEFT") })
                Spacer(Modifier.width(8.dp))
                CactusButton("DOWN", onClick = { vm.sendKeys("DOWN") })
                Spacer(Modifier.width(8.dp))
                CactusButton("RIGHT", onClick = { vm.sendKeys("RIGHT") })
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
