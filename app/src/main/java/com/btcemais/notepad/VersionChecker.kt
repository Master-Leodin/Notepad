package com.btcemais.notepad

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

@Serializable
data class AppVersion(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val releaseNotes: String,
    val apkUrl: String
)

@Serializable
data class AllAppsVersions(
    val apps: Map<String, AppVersion>
)

object VersionChecker {
    private const val VERSION_URL = "https://pagebroke.netlify.app/json/all_apps_versions.json"
    private val json = Json { ignoreUnknownKeys = true; ignoreUnknownKeys = true }
    private const val TAG = "VersionChecker"
    private const val CONNECTION_TIMEOUT = 10000 // 10 seconds
    private const val READ_TIMEOUT = 10000 // 10 seconds

    private fun getCurrentVersionCode(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = packageInfo.longVersionCode.toInt()
            Log.d(TAG, "Current version code: $versionCode, package: ${context.packageName}")
            versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Error getting current version", e)
            -1
        }
    }

    private suspend fun fetchAllAppsVersions(): Result<AllAppsVersions> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching versions from: $VERSION_URL")

                val url = URL(VERSION_URL)
                val connection = url.openConnection() as HttpsURLConnection

                // Configure timeouts
                connection.connectTimeout = CONNECTION_TIMEOUT
                connection.readTimeout = READ_TIMEOUT
                connection.requestMethod = "GET"

                // Add headers to avoid cache
                connection.setRequestProperty("Cache-Control", "no-cache")
                connection.setRequestProperty("Pragma", "no-cache")

                val responseCode = connection.responseCode
                Log.d(TAG, "HTTP Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val jsonString = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        jsonString.append(line)
                    }

                    reader.close()
                    inputStream.close()

                    Log.d(TAG, "Received JSON successfully, length: ${jsonString.length}")
                    Log.d(TAG, "JSON content: ${jsonString.toString()}")

                    val allVersions = json.decodeFromString<AllAppsVersions>(jsonString.toString())
                    Log.d(TAG, "Parsed versions: ${allVersions.apps.keys}")

                    Result.success(allVersions)
                } else {
                    Log.e(TAG, "HTTP error: $responseCode - ${connection.responseMessage}")
                    Result.failure(RuntimeException("HTTP $responseCode"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching versions: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun checkForUpdate(context: Context): AppVersion? {
        Log.d(TAG, "Starting update check...")
        val currentVersionCode = getCurrentVersionCode(context)
        if (currentVersionCode == -1) return null

        val result = fetchAllAppsVersions()

        if (result.isFailure) {
            Log.e(TAG, "Failed to fetch versions: ${result.exceptionOrNull()?.message}")
            return null
        }

        val allVersions = result.getOrNull()
        if (allVersions == null) {
            Log.d(TAG, "No versions data received")
            return null
        }

        val latestVersion = allVersions.apps[context.packageName]

        return latestVersion?.let {
            Log.d(TAG, "Latest version code: ${it.latestVersionCode}, Current: $currentVersionCode")
            if (it.latestVersionCode > currentVersionCode) {
                Log.d(TAG, "Update available!")
                it
            } else {
                Log.d(TAG, "No update needed")
                null
            }
        } ?: run {
            Log.d(TAG, "No version info found for package: ${context.packageName}")
            Log.d(TAG, "Available packages: ${allVersions.apps.keys}")
            null
        }
    }
}