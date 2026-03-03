package com.dotagency.cactusc2.data.model

/**
 * Parsed status snapshot from the ESPloit dashboard HTML.
 * Populated by [com.dotagency.cactusc2.data.network.ESPloitClient.parseDashboard].
 */
data class DeviceStatus(
    val connected: Boolean = false,
    val spiffsUsed: String = "",      // Human-readable (e.g. "1.2 MB")
    val spiffsTotal: String = "",     // Human-readable (e.g. "3.0 MB")
    val spiffsPercent: Int = 0,       // 0-100 for progress bar
    val firmwareVersion: String = "",
    val chipId: String = ""
)

/** Entry in the on-device payload list (parsed from /listpayloads HTML). */
data class PayloadInfo(
    val name: String,
    val size: String
)

/** Entry in the exfiltrated-data list (parsed from /exfiltrate/list HTML). */
data class ExfiltratedFile(
    val name: String
)

/** Maps 1:1 to the ESPloit /submitsettings form fields. */
data class DeviceSettings(
    val wifiSSID: String = "",
    val wifiPassword: String = "",
    val apSSID: String = "",
    val apPassword: String = "",
    val adminUsername: String = "",
    val adminPassword: String = "",
    val esportalEnabled: Boolean = false,
    val esportalSSID: String = "",
    val esportalPassword: String = "",
    val payload1: String = "",
    val payload1RunOnBoot: Boolean = false,
    val payload2: String = "",
    val payload2RunOnBoot: Boolean = false
)
