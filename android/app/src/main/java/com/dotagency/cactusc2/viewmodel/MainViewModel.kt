package com.dotagency.cactusc2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dotagency.cactusc2.data.PayloadSeeder
import com.dotagency.cactusc2.data.db.AppDatabase
import com.dotagency.cactusc2.data.model.*
import com.dotagency.cactusc2.data.network.ESPloitClient
import com.dotagency.cactusc2.data.network.WifiConnector
import com.dotagency.cactusc2.data.repository.DeviceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Central ViewModel shared by all screens in Cactus C2.
 *
 * Owns the Room database, HTTP client, WiFi connector, and all UI state.
 * Exposes reactive [StateFlow]s that Compose screens collect, and action
 * methods that launch coroutines in [viewModelScope].
 *
 * On first launch, seeds the local payload database from bundled_payloads.json
 * via [PayloadSeeder].
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    // ── Core dependencies ──
    private val db = AppDatabase.getInstance(app)
    val repository = DeviceRepository(db.deviceDao(), db.payloadDao())
    val espClient = ESPloitClient()      // HTTP client for ESPloit API
    val wifiConnector = WifiConnector(app) // Programmatic AP switching

    // ── Reactive state exposed to UI ──
    val devices = repository.allDevices.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val localPayloads = repository.allPayloads.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice

    private val _deviceStatus = MutableStateFlow<DeviceStatus>(DeviceStatus())
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting

    private val _remotePayloads = MutableStateFlow<List<PayloadInfo>>(emptyList())
    val remotePayloads: StateFlow<List<PayloadInfo>> = _remotePayloads

    private val _exfilFiles = MutableStateFlow<List<ExfiltratedFile>>(emptyList())
    val exfilFiles: StateFlow<List<ExfiltratedFile>> = _exfilFiles

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar

    private val _livePayloadResult = MutableStateFlow("")
    val livePayloadResult: StateFlow<String> = _livePayloadResult

    init {
        // Seed bundled payloads into Room on first launch
        viewModelScope.launch {
            PayloadSeeder.seedIfNeeded(app, db.payloadDao())
        }
        // Auto-select the default device once the device list loads
        viewModelScope.launch {
            devices.collect { list ->
                if (_selectedDevice.value == null) {
                    _selectedDevice.value = list.firstOrNull { it.isDefault } ?: list.firstOrNull()
                }
            }
        }
    }

    fun selectDevice(device: Device) {
        _selectedDevice.value = device
        refreshStatus()
    }

    fun addDevice(device: Device) = viewModelScope.launch {
        repository.addDevice(device)
        msg("Device added")
    }

    fun updateDevice(device: Device) = viewModelScope.launch {
        repository.updateDevice(device)
        if (_selectedDevice.value?.id == device.id) _selectedDevice.value = device
        msg("Device updated")
    }

    fun deleteDevice(device: Device) = viewModelScope.launch {
        repository.deleteDevice(device)
        if (_selectedDevice.value?.id == device.id) {
            _selectedDevice.value = null
        }
        msg("Device removed")
    }

    fun setDefaultDevice(device: Device) = viewModelScope.launch {
        repository.setDefaultDevice(device.id)
        msg("${device.name} set as default")
    }

    // ── WiFi ──

    fun connectToDevice(device: Device) = viewModelScope.launch {
        _isConnecting.value = true
        val success = wifiConnector.connectToDevice(device.ssid, device.wifiPassword)
        _isConnecting.value = false
        if (success) {
            msg("Connected to ${device.ssid}")
            _selectedDevice.value = device
            refreshStatus()
        } else {
            msg("Failed to connect to ${device.ssid}")
            _deviceStatus.value = DeviceStatus(connected = false)
        }
    }

    // ── Status ──

    fun refreshStatus() = viewModelScope.launch {
        val device = _selectedDevice.value ?: return@launch
        val connected = espClient.ping(device)
        if (connected) {
            espClient.getStatus(device).onSuccess {
                _deviceStatus.value = it
            }.onFailure {
                _deviceStatus.value = DeviceStatus(connected = true)
            }
        } else {
            _deviceStatus.value = DeviceStatus(connected = false)
        }
    }

    // ── Live Payload ──

    fun runLivePayload(script: String) = viewModelScope.launch {
        val device = _selectedDevice.value ?: run { msg("No device selected"); return@launch }
        espClient.runLivePayload(device, script).onSuccess {
            _livePayloadResult.value = it
            msg("Payload sent to device")
            scheduleExfilPoll()
        }.onFailure {
            _livePayloadResult.value = "Error: ${it.message}"
            msg("Payload failed: ${it.message}")
        }
    }

    // ── Payload Manager ──

    fun refreshRemotePayloads() = viewModelScope.launch {
        val device = _selectedDevice.value ?: return@launch
        espClient.listPayloads(device).onSuccess {
            _remotePayloads.value = it
        }.onFailure { msg("Error listing payloads: ${it.message}") }
    }

    fun uploadPayload(name: String, content: String) = viewModelScope.launch {
        val device = _selectedDevice.value ?: run { msg("No device selected"); return@launch }
        espClient.uploadPayload(device, name, content).onSuccess {
            msg("Uploaded $name")
            refreshRemotePayloads()
        }.onFailure { msg("Upload failed: ${it.message}") }
    }

    fun runRemotePayload(name: String) = viewModelScope.launch {
        val device = _selectedDevice.value ?: return@launch
        espClient.runPayload(device, name).onSuccess {
            msg("Running $name")
            scheduleExfilPoll()
        }.onFailure { msg("Error: ${it.message}") }
    }

    /** Download a payload from device SPIFFS and save it to the local Room database. */
    fun downloadPayloadToLocal(payloadPath: String) = viewModelScope.launch {
        val device = _selectedDevice.value ?: run { msg("No device selected"); return@launch }
        espClient.showPayload(device, payloadPath).onSuccess { content ->
            // Extract a clean name from the SPIFFS path (e.g. "/payloads/hello.txt" -> "hello.txt")
            val cleanName = payloadPath.substringAfterLast("/")
            val payload = Payload(
                name = cleanName,
                content = content,
                description = "Downloaded from device",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.addPayload(payload)
            msg("Downloaded $cleanName to local payloads")
        }.onFailure { msg("Download failed: ${it.message}") }
    }

    fun deleteRemotePayload(name: String) = viewModelScope.launch {
        val device = _selectedDevice.value ?: return@launch
        espClient.deletePayload(device, name).onSuccess {
            msg("Deleted $name")
            refreshRemotePayloads()
        }.onFailure { msg("Error: ${it.message}") }
    }

    fun saveLocalPayload(payload: Payload) = viewModelScope.launch {
        if (payload.id == 0L) {
            repository.addPayload(payload)
            msg("Payload saved locally")
        } else {
            repository.updatePayload(payload.copy(updatedAt = System.currentTimeMillis()))
            msg("Payload updated")
        }
    }

    fun deleteLocalPayload(payload: Payload) = viewModelScope.launch {
        repository.deletePayload(payload)
        msg("Local payload deleted")
    }

    // ── Exfiltration ──

    private val _exfilLoading = MutableStateFlow(false)
    val exfilLoading: StateFlow<Boolean> = _exfilLoading

    fun refreshExfilData() = viewModelScope.launch {
        val device = _selectedDevice.value ?: return@launch
        espClient.listExfiltratedData(device).onSuccess {
            _exfilFiles.value = it
        }.onFailure { msg("Error: ${it.message}") }
    }

    private val _exfilContent = MutableStateFlow("")
    val exfilContent: StateFlow<String> = _exfilContent

    fun loadExfilFile(name: String) = viewModelScope.launch {
        val device = _selectedDevice.value ?: return@launch
        _exfilContent.value = ""
        _exfilLoading.value = true
        espClient.getExfiltratedFile(device, name).onSuccess {
            _exfilContent.value = it
        }.onFailure { msg("Error loading file: ${it.message}") }
        _exfilLoading.value = false
    }

    fun clearExfilContent() {
        _exfilContent.value = ""
    }

    /** Poll exfil data a few times after a payload runs, catching data that gets written to SPIFFS. */
    private fun scheduleExfilPoll() = viewModelScope.launch {
        // Payloads take time to execute and exfil data; poll at 3s, 8s, 15s
        for (delayMs in longArrayOf(3000, 5000, 7000)) {
            delay(delayMs)
            val device = _selectedDevice.value ?: return@launch
            espClient.listExfiltratedData(device).onSuccess {
                _exfilFiles.value = it
            }
        }
    }

    // ── Settings ──

    fun submitDeviceSettings(params: Map<String, String>) = viewModelScope.launch {
        val device = _selectedDevice.value ?: run { msg("No device selected"); return@launch }
        espClient.submitSettings(device, params).onSuccess {
            msg("Settings saved to device")
        }.onFailure { msg("Error: ${it.message}") }
    }

    // ── Firmware ──

    fun getFirmwareInfo() = viewModelScope.launch {
        val device = _selectedDevice.value ?: return@launch
        espClient.getFirmwareInfo(device).onSuccess {
            msg("Firmware info loaded")
        }.onFailure { msg("Error: ${it.message}") }
    }

    fun uploadFirmware(file: java.io.File) = viewModelScope.launch {
        val device = _selectedDevice.value ?: run { msg("No device selected"); return@launch }
        msg("Uploading firmware...")
        espClient.uploadFirmware(device, file).onSuccess {
            msg("Firmware upload complete — device rebooting")
        }.onFailure { msg("Firmware upload failed: ${it.message}") }
    }

    fun autoUpdateFirmware(url: String) = viewModelScope.launch {
        val device = _selectedDevice.value ?: run { msg("No device selected"); return@launch }
        espClient.autoUpdateFirmware(device, url).onSuccess {
            msg("Auto-update started")
        }.onFailure { msg("Error: ${it.message}") }
    }

    // ── Device actions ──

    fun rebootDevice() = viewModelScope.launch {
        val device = _selectedDevice.value ?: return@launch
        espClient.reboot(device).onSuccess { msg("Rebooting device") }
            .onFailure { msg("Error: ${it.message}") }
    }

    fun restoreDefaults() = viewModelScope.launch {
        val device = _selectedDevice.value ?: return@launch
        espClient.restoreDefaults(device).onSuccess { msg("Defaults restored") }
            .onFailure { msg("Error: ${it.message}") }
    }

    fun formatSpiffs() = viewModelScope.launch {
        val device = _selectedDevice.value ?: return@launch
        espClient.formatSpiffs(device).onSuccess { msg("SPIFFS formatted") }
            .onFailure { msg("Error: ${it.message}") }
    }

    // ── Input mode ──

    fun sendKeys(script: String) = viewModelScope.launch {
        val device = _selectedDevice.value ?: run { msg("No device selected"); return@launch }
        espClient.sendKeys(device, script).onSuccess {
            msg("Keys sent")
        }.onFailure { msg("Error: ${it.message}") }
    }

    /** Emit a snackbar message for the UI to display. */
    private fun msg(text: String) = viewModelScope.launch { _snackbar.emit(text) }

    /** Public snackbar message for screens that need to show errors directly. */
    fun snackbarMsg(text: String) = msg(text)
}
