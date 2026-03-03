package com.dotagency.cactusc2.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a Cactus WHID device in the local registry.
 *
 * Stores WiFi AP credentials (ssid/wifiPassword) for automatic connection,
 * the HTTP endpoint (ipAddress:httpPort) for ESPloit API calls, and optional
 * Basic Auth credentials (adminUser/adminPassword) for protected endpoints.
 *
 * One device can be marked [isDefault] — auto-selected on app launch.
 * Default IP 192.168.1.1:80 matches the Cactus WHID factory AP.
 */
@Entity(tableName = "devices")
data class Device(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,              // User-friendly label (e.g. "Lab Cactus #1")
    val ssid: String,              // WiFi AP SSID broadcast by the Cactus
    val wifiPassword: String,      // WPA2 passphrase for the AP
    val ipAddress: String = "192.168.1.1",  // Default Cactus AP gateway
    val httpPort: Int = 80,                 // ESPloit web UI port
    val adminUser: String = "admin",        // Basic Auth username
    val adminPassword: String = "",         // Basic Auth password (empty = no auth)
    val isDefault: Boolean = false          // Auto-select flag
)
