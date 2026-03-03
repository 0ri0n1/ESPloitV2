package com.dotagency.cactusc2.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dotagency.cactusc2.data.model.Device
import com.dotagency.cactusc2.ui.screens.dashboard.DashboardScreen
import com.dotagency.cactusc2.ui.screens.devices.DeviceEditScreen
import com.dotagency.cactusc2.ui.screens.devices.DeviceListScreen
import com.dotagency.cactusc2.ui.screens.exfiltration.ExfiltrationScreen
import com.dotagency.cactusc2.ui.screens.firmware.FirmwareScreen
import com.dotagency.cactusc2.ui.screens.inputmode.InputModeScreen
import com.dotagency.cactusc2.ui.screens.livepayload.LivePayloadScreen
import com.dotagency.cactusc2.ui.screens.payload.PayloadManagerScreen
import com.dotagency.cactusc2.ui.screens.settings.SettingsScreen
import com.dotagency.cactusc2.viewmodel.MainViewModel

/** Navigation route constants for all screens. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val DEVICES = "devices"
    const val DEVICE_ADD = "device_add"
    const val DEVICE_EDIT = "device_edit"
    const val LIVE_PAYLOAD = "livepayload"
    const val PAYLOADS = "payloads"
    const val INPUT_MODE = "inputmode"
    const val EXFILTRATION = "exfiltration"
    const val SETTINGS = "settings"
    const val FIRMWARE = "firmware"
}

/**
 * Navigation graph wiring all 10 screens to their routes.
 * Dashboard is the start destination; sub-screens pop back to it.
 */
@Composable
fun CactusNavGraph(
    navController: NavHostController,
    vm: MainViewModel
) {
    var editDevice by remember { mutableStateOf<Device?>(null) }

    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                vm = vm,
                onNavigate = { navController.navigate(it) }
            )
        }
        composable(Routes.DEVICES) {
            DeviceListScreen(
                vm = vm,
                onDeviceSelected = { navController.navigate(Routes.DASHBOARD) },
                onAddDevice = { navController.navigate(Routes.DEVICE_ADD) },
                onEditDevice = {
                    editDevice = it
                    navController.navigate(Routes.DEVICE_EDIT)
                }
            )
        }
        composable(Routes.DEVICE_ADD) {
            DeviceEditScreen(
                vm = vm,
                editDevice = null,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.DEVICE_EDIT) {
            DeviceEditScreen(
                vm = vm,
                editDevice = editDevice,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.LIVE_PAYLOAD) {
            LivePayloadScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.PAYLOADS) {
            PayloadManagerScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.INPUT_MODE) {
            InputModeScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.EXFILTRATION) {
            ExfiltrationScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.FIRMWARE) {
            FirmwareScreen(vm = vm, onBack = { navController.popBackStack() })
        }
    }
}
