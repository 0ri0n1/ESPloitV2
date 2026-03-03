package com.dotagency.cactusc2.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for a locally-stored DuckyScript payload.
 *
 * Payloads are either seeded from bundled_payloads.json on first launch
 * (see [com.dotagency.cactusc2.data.PayloadSeeder]) or user-created
 * in the Payload Manager / Live Payload editor.
 *
 * [content] holds raw DuckyScript v1.0 text (STRING, DELAY, GUI, etc.)
 * that gets POSTed to the ESPloit /runlivepayload endpoint.
 */
@Entity(tableName = "payloads")
data class Payload(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,           // Filename-style label (e.g. "win-rickroll.txt")
    val content: String,        // Raw DuckyScript v1.0 payload body
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
