package com.dotagency.cactusc2.data

import android.content.Context
import com.dotagency.cactusc2.data.db.PayloadDao
import com.dotagency.cactusc2.data.model.Payload
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Seeds the local Room database with bundled DuckyScript payloads on first launch.
 *
 * Reads payloads from assets/bundled_payloads.json (shipped with the APK) and inserts
 * them into the payloads table. Uses a SharedPreferences flag ("payloads_seeded") to
 * ensure seeding only happens once. The flag is versioned so future payload updates
 * can re-trigger seeding by bumping SEED_VERSION.
 */
object PayloadSeeder {

    /** Bump this to re-seed payloads after an app update adds new ones. */
    private const val SEED_VERSION = 1
    private const val PREFS_NAME = "cactus_c2_prefs"
    private const val KEY_SEED_VERSION = "payloads_seed_version"

    /**
     * Checks whether seeding is needed and, if so, parses the bundled JSON and
     * batch-inserts all payloads into Room.
     *
     * @param context  Application context (for asset access and SharedPreferences)
     * @param dao      PayloadDao to insert records into
     */
    suspend fun seedIfNeeded(context: Context, dao: PayloadDao) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentVersion = prefs.getInt(KEY_SEED_VERSION, 0)
        if (currentVersion >= SEED_VERSION) return // already seeded at this version

        // Read bundled JSON from APK assets
        val json = context.assets.open("bundled_payloads.json")
            .bufferedReader()
            .use { it.readText() }

        // Parse JSON array into BundledPayload DTOs
        val type = object : TypeToken<List<BundledPayload>>() {}.type
        val bundled: List<BundledPayload> = Gson().fromJson(json, type)

        // Map DTOs to Room entities and insert
        val now = System.currentTimeMillis()
        bundled.forEach { bp ->
            dao.insert(
                Payload(
                    name = bp.name,
                    content = bp.content,
                    description = "[${bp.category}] [${bp.os}] ${bp.description}",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        // Mark seeding complete for this version
        prefs.edit().putInt(KEY_SEED_VERSION, SEED_VERSION).apply()
    }

    /**
     * DTO matching the JSON schema in bundled_payloads.json.
     * Fields map 1:1 to the JSON keys.
     */
    private data class BundledPayload(
        val name: String,
        val category: String,
        val os: String,
        val description: String,
        val content: String
    )
}
