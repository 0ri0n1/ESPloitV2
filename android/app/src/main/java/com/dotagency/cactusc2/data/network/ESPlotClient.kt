package com.dotagency.cactusc2.data.network

import com.dotagency.cactusc2.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP client covering the full ESPloit REST API on the Cactus WHID.
 *
 * All calls run on [Dispatchers.IO] and return [Result] for caller-friendly error handling.
 *
 * **Public endpoints** (no auth): dashboard, live payload, payload list/upload,
 * exfiltrated data list/view.
 *
 * **Protected endpoints** (Basic Auth via [authClient]): settings, delete/run payload,
 * reboot, firmware, restore defaults, format SPIFFS.
 *
 * The dashboard HTML is parsed with regex to extract SPIFFS usage and firmware version
 * (the device has no JSON API — only server-rendered HTML pages).
 */
class ESPloitClient {

    /** Shared OkHttp instance with short timeouts for fast device pings. */
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /** Builds cleartext HTTP base URL from device config (default: http://192.168.1.1:80). */
    private fun baseUrl(device: Device) = "http://${device.ipAddress}:${device.httpPort}"

    /** Returns an OkHttp client that auto-attaches Basic Auth on 401 challenge. */
    private fun authClient(device: Device): OkHttpClient {
        return client.newBuilder()
            .authenticator { _, response ->
                if (response.request.header("Authorization") != null) return@authenticator null
                val credential = Credentials.basic(device.adminUser, device.adminPassword)
                response.request.newBuilder()
                    .header("Authorization", credential)
                    .build()
            }
            .build()
    }

    // ── Public endpoints (no auth required) ──

    /** Quick connectivity check — true if device responds to GET /. */
    suspend fun ping(device: Device): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(baseUrl(device) + "/").build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: IOException) {
            false
        }
    }

    suspend fun getDashboard(device: Device): Result<String> = get(device, "/", auth = false)

    suspend fun getStatus(device: Device): Result<DeviceStatus> = withContext(Dispatchers.IO) {
        try {
            val html = get(device, "/", auth = false).getOrThrow()
            Result.success(parseDashboard(html))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun runLivePayload(device: Device, script: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val body = FormBody.Builder()
                    .add("livepayload", script)
                    .add("livepayloadpresent", "1") // Required for firmware to process
                    .build()
                val request = Request.Builder()
                    .url(baseUrl(device) + "/runlivepayload")
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    val html = response.body?.string() ?: ""
                    Result.success(parseLivePayloadResponse(html))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun listPayloads(device: Device): Result<List<PayloadInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val html = get(device, "/listpayloads", auth = false).getOrThrow()
                Result.success(parsePayloadList(html))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun uploadPayload(device: Device, name: String, content: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "upload", name,
                        content.toRequestBody("application/octet-stream".toMediaType())
                    )
                    .build()
                val request = Request.Builder()
                    .url(baseUrl(device) + "/upload")
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IOException("Upload failed: HTTP ${response.code}"))
                    }
                    Result.success(response.body?.string() ?: "")
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Fetch the content of a stored payload from the device SPIFFS. */
    suspend fun showPayload(device: Device, payloadPath: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(payloadPath, "UTF-8")
                val request = Request.Builder()
                    .url(baseUrl(device) + "/showpayload?payload=$encoded")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IOException("HTTP ${response.code}"))
                    }
                    val html = response.body?.string() ?: ""
                    Result.success(parsePayloadContent(html))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun listExfiltratedData(device: Device): Result<List<ExfiltratedFile>> =
        withContext(Dispatchers.IO) {
            try {
                val html = get(device, "/exfiltrate/list", auth = false).getOrThrow()
                Result.success(parseExfilList(html))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getExfiltratedFile(device: Device, fileName: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // Firmware serves file contents via /showpayload?payload=FILENAME
                // Response is HTML with content inside <pre>FILENAME\n-----\nCONTENT</pre>
                val encoded = java.net.URLEncoder.encode(fileName, "UTF-8")
                val request = Request.Builder()
                    .url(baseUrl(device) + "/showpayload?payload=$encoded")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IOException("HTTP ${response.code}"))
                    }
                    val html = response.body?.string() ?: ""
                    Result.success(parseExfilContent(html))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ── Auth-protected endpoints ──

    suspend fun getSettings(device: Device): Result<String> = get(device, "/settings", auth = true)

    suspend fun submitSettings(device: Device, params: Map<String, String>): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val formBuilder = FormBody.Builder()
                params.forEach { (k, v) -> formBuilder.add(k, v) }
                val request = Request.Builder()
                    .url(baseUrl(device) + "/submitsettings")
                    .post(formBuilder.build())
                    .build()
                authClient(device).newCall(request).execute().use { response ->
                    Result.success(response.body?.string() ?: "")
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deletePayload(device: Device, name: String): Result<String> =
        get(device, "/deletepayload?payload=${java.net.URLEncoder.encode(name, "UTF-8")}", auth = true)

    suspend fun runPayload(device: Device, name: String): Result<String> =
        get(device, "/dopayload?payload=${java.net.URLEncoder.encode(name, "UTF-8")}", auth = true)

    suspend fun reboot(device: Device): Result<String> =
        get(device, "/reboot", auth = true)

    suspend fun getFirmwareInfo(device: Device): Result<String> =
        get(device, "/firmware", auth = true)

    suspend fun restoreDefaults(device: Device): Result<String> =
        get(device, "/restoredefaults", auth = true)

    suspend fun formatSpiffs(device: Device): Result<String> =
        get(device, "/format", auth = true)

    /** OTA firmware upload via port 1337 (dedicated firmware update server on the ESP). */
    suspend fun uploadFirmware(device: Device, firmwareFile: File): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "update", firmwareFile.name,
                        firmwareFile.asRequestBody("application/octet-stream".toMediaType())
                    )
                    .build()
                val request = Request.Builder()
                    .url("http://${device.ipAddress}:1337/update")  // Firmware port, NOT main web UI
                    .post(body)
                    .build()
                // Extended timeouts for large firmware binaries
                val longClient = authClient(device).newBuilder()
                    .writeTimeout(120, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .build()
                longClient.newCall(request).execute().use { response ->
                    Result.success(response.body?.string() ?: "")
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun autoUpdateFirmware(device: Device, url: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val body = FormBody.Builder().add("url", url).build()
                val request = Request.Builder()
                    .url(baseUrl(device) + "/autoupdatefirmware")
                    .post(body)
                    .build()
                authClient(device).newCall(request).execute().use { response ->
                    Result.success(response.body?.string() ?: "")
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ── Input mode ──

    suspend fun sendKeys(device: Device, keys: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val body = FormBody.Builder()
                    .add("livepayload", keys)
                    .add("livepayloadpresent", "1")
                    .build()
                val request = Request.Builder()
                    .url(baseUrl(device) + "/runlivepayload")
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    Result.success(response.body?.string() ?: "")
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ── Helpers ──

    private suspend fun get(device: Device, path: String, auth: Boolean): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(baseUrl(device) + path).build()
                val c = if (auth) authClient(device) else client
                c.newCall(request).execute().use { response ->
                    Result.success(response.body?.string() ?: "")
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Extracts SPIFFS usage and firmware version from the ESPloit dashboard HTML.
     * Uses regex because the device serves plain HTML, not a JSON API.
     */
    private fun parseDashboard(html: String): DeviceStatus {
        // Firmware outputs both formats: "<b>2510</b> B used" and "2510 bytes used"
        val usedMatch = Regex("""(\d+)\s*(?:bytes?|B)\s*used""", RegexOption.IGNORE_CASE).find(html)
        val totalMatch = Regex("""(\d+)\s*(?:bytes?|B)\s*total""", RegexOption.IGNORE_CASE).find(html)
        val used = usedMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val total = totalMatch?.groupValues?.get(1)?.toLongOrNull() ?: 1L
        val pct = if (total > 0) ((used * 100) / total).toInt() else 0
        val fwMatch = Regex("""v?(\d+\.\d+\.\d+)""", RegexOption.IGNORE_CASE).find(html)
        return DeviceStatus(
            connected = true,
            spiffsUsed = formatBytes(used),
            spiffsTotal = formatBytes(total),
            spiffsPercent = pct,
            firmwareVersion = fwMatch?.groupValues?.get(1) ?: "unknown"
        )
    }

    private fun parsePayloadList(html: String): List<PayloadInfo> {
        val results = mutableListOf<PayloadInfo>()
        val seen = mutableSetOf<String>()

        // Primary: match table rows — <a href="/showpayload?payload=NAME">NAME</a></td><td>SIZE</td>
        // Firmware renders: <td><a href="/showpayload?payload=/payloads/file.txt">/payloads/file.txt</a></td><td>83</td>
        val tablePattern = Regex(
            """href="/showpayload\?payload=([^"]+)"[^>]*>[^<]*</a>\s*</td>\s*<td>(\d+)</td>""",
            RegexOption.IGNORE_CASE
        )
        tablePattern.findAll(html).forEach { match ->
            val name = match.groupValues[1].trim()
            val sizeBytes = match.groupValues[2].trim()
            if (name.isNotEmpty() && seen.add(name)) {
                results.add(PayloadInfo(name, "$sizeBytes B"))
            }
        }

        // Fallback: older firmware format — href="...payload=NAME"...>...</a> - SIZE
        if (results.isEmpty()) {
            val legacyPattern = Regex(
                """href="[^"]*payload=([^"&]+)"[^>]*>.*?</a>\s*[\-–]\s*(\d+\s*\w+)""",
                RegexOption.DOT_MATCHES_ALL
            )
            legacyPattern.findAll(html).forEach { match ->
                val name = match.groupValues[1].trim()
                if (name.isNotEmpty() && seen.add(name)) {
                    results.add(PayloadInfo(name, match.groupValues[2].trim()))
                }
            }
        }

        // Last resort: match any .txt filenames in the HTML
        if (results.isEmpty()) {
            val simplePattern = Regex(""">\s*([^<]+\.txt)\s*<""")
            simplePattern.findAll(html).forEach { match ->
                val name = match.groupValues[1].trim()
                if (name.isNotEmpty() && seen.add(name)) {
                    results.add(PayloadInfo(name, ""))
                }
            }
        }
        return results
    }

    /** Parse the /runlivepayload HTML response into readable feedback. */
    private fun parseLivePayloadResponse(html: String): String {
        // Firmware responds: <pre>Running live payload: <br>SCRIPT</pre>
        val preMatch = Regex("""<pre>(.*?)</pre>""", setOf(RegexOption.DOT_MATCHES_ALL)).find(html)
        if (preMatch != null) {
            return preMatch.groupValues[1]
                .replace("<br>", "\n")
                .replace(Regex("<[^>]*>"), "")
                .trim()
        }
        // If we got an HTML page but no <pre>, the request likely failed
        return if (html.contains("Running live payload", ignoreCase = true)) {
            "Payload accepted by device"
        } else if (html.isBlank()) {
            "No response from device"
        } else {
            "Device responded (payload may not have executed)"
        }
    }

    /** Extract payload script content from /showpayload HTML. Same format as exfil but reusable. */
    private fun parsePayloadContent(html: String): String {
        val preMatch = Regex("""<pre>(.*?)</pre>""", setOf(RegexOption.DOT_MATCHES_ALL)).find(html)
        val preContent = preMatch?.groupValues?.get(1) ?: return html.replace(Regex("<[^>]*>"), "").trim()
        // Firmware wraps as: <pre>/payloads/name.txt\n-----\nCONTENT</pre>
        val dividerIndex = preContent.indexOf("-----\n")
        return if (dividerIndex >= 0) {
            preContent.substring(dividerIndex + 6).trim()
        } else {
            preContent.replace(Regex("<[^>]*>"), "").trim()
        }
    }

    /** Extract raw file content from the /showpayload HTML response. */
    private fun parseExfilContent(html: String): String {
        // Firmware wraps content in: <pre>/filename\n-----\nACTUAL_CONTENT</pre>
        val preMatch = Regex("""<pre>(.*?)</pre>""", setOf(RegexOption.DOT_MATCHES_ALL)).find(html)
        val preContent = preMatch?.groupValues?.get(1) ?: return html
        val dividerIndex = preContent.indexOf("-----\n")
        return if (dividerIndex >= 0) {
            preContent.substring(dividerIndex + 6)
        } else {
            preContent
        }
    }

    private fun parseExfilList(html: String): List<ExfiltratedFile> {
        val seen = mutableSetOf<String>()
        val results = mutableListOf<ExfiltratedFile>()

        // Primary: extract filenames from showpayload hrefs (firmware-generated links)
        val hrefPattern = Regex("""href="/showpayload\?payload=([^"]+)"""")
        hrefPattern.findAll(html).forEach { match ->
            val name = java.net.URLDecoder.decode(match.groupValues[1], "UTF-8").trim()
            if (name.isNotEmpty() && seen.add(name)) {
                results.add(ExfiltratedFile(name))
            }
        }

        // Fallback: match SPIFFS filenames (must start with /) if href parsing found nothing
        if (results.isEmpty()) {
            val pattern = Regex(""">\s*(/[^<]+\.\w+)\s*<""")
            pattern.findAll(html).forEach { match ->
                val name = match.groupValues[1].trim()
                if (name.isNotEmpty() && !Regex("""^\d+\.\d+\.\d+\.\d+""").matches(name) && seen.add(name)) {
                    results.add(ExfiltratedFile(name))
                }
            }
        }
        return results
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
