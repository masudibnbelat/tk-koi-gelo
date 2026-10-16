// FILE: app/src/main/java/com/masud/moneymanager/update/AppUpdateManager.kt
package com.masud.moneymanager.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tagName: String,
    val versionLabel: String,
    val apkUrl: String,
    val apkName: String,
    val releaseNotes: String,
    val htmlUrl: String,
)

object AppUpdateManager {

    private const val OWNER = "masudibnbelat"
    private const val REPO = "tk-koi-gelo"
    private const val API_URL = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    /** Returns UpdateInfo if a newer release exists, null if already up to date. */
    suspend fun checkForUpdate(context: Context): Result<UpdateInfo?> =
        withContext(Dispatchers.IO) {
            try {
                val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "TkKoiGelo-App")
                }
                val code = conn.responseCode
                if (code == 404) return@withContext Result.failure(Exception("No releases published yet"))
                if (code != 200) return@withContext Result.failure(Exception("GitHub responded $code"))
                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val json = JSONObject(body)
                val tag = json.getString("tag_name")
                if (!isNewer(tag, currentVersionName(context))) {
                    return@withContext Result.success(null)
                }

                var apkUrl: String? = null
                var apkName = "tk-koi-gelo-update.apk"
                val assets = json.getJSONArray("assets")
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.getString("browser_download_url")
                        apkName = name
                        break
                    }
                }
                if (apkUrl == null) {
                    return@withContext Result.failure(Exception("No APK attached to release $tag"))
                }

                Result.success(
                    UpdateInfo(
                        tagName = tag,
                        versionLabel = tag.trimStart('v', 'V'),
                        apkUrl = apkUrl,
                        apkName = apkName,
                        releaseNotes = json.optString("body", ""),
                        htmlUrl = json.optString("html_url", ""),
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Downloads the APK to app cache, reporting 0..100 progress. */
    suspend fun downloadApk(
        context: Context,
        info: UpdateInfo,
        onProgress: (Int) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", "TkKoiGelo-App")
                instanceFollowRedirects = true
            }
            if (conn.responseCode != 200) {
                return@withContext Result.failure(Exception("Download failed (${conn.responseCode})"))
            }
            val total = conn.contentLengthLong
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val file = File(dir, info.apkName)

            conn.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var lastProgress = -1
                    while (true) {
                        val n = input.read(buffer)
                        if (n == -1) break
                        output.write(buffer, 0, n)
                        downloaded += n
                        if (total > 0) {
                            val p = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                            if (p != lastProgress) {
                                lastProgress = p
                                onProgress(p)
                            }
                        }
                    }
                }
            }
            conn.disconnect()
            onProgress(100)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Android 8+: user must allow "Install unknown apps" for this app once. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    fun openUnknownSourcesSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun currentVersionName(context: Context): String {
        return try {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                pm.getPackageInfo(context.packageName, 0)
            }
            info.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    /** Compares "v1.2.0" style tags against versionName numerically. */
    fun isNewer(tag: String, current: String): Boolean {
        fun parts(v: String): List<Int> =
            v.trim().trimStart('v', 'V').split(".", "-").mapNotNull { it.toIntOrNull() }

        val t = parts(tag)
        val c = parts(current)
        val n = maxOf(t.size, c.size)
        for (i in 0 until n) {
            val a = t.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}
