package com.btcemais.notepad

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MainActivity : AppCompatActivity() {

    private var updateApkFile: File? = null
    private var updateDialog: AlertDialog? = null
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "MainActivity created")

        val viewPager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.notes_tab)
                1 -> getString(R.string.about_tab)
                else -> null
            }
        }.attach()

        // Check for updates after a small delay for UI to load
        lifecycleScope.launch {
            kotlinx.coroutines.delay(2000) // 2 seconds delay
            checkForAppUpdate()
        }
    }

    private fun checkForAppUpdate() {
        Log.d(TAG, "Checking for app update...")
        lifecycleScope.launch {
            try {
                val updateInfo = VersionChecker.checkForUpdate(this@MainActivity)
                Log.d(TAG, "Update check completed: $updateInfo")

                if (updateInfo != null) {
                    Log.d(TAG, "Update available! Showing dialog...")
                    showUpdateDialog(updateInfo)
                } else {
                    Log.d(TAG, "No update available or check failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during update check: ${e.message}", e)
            }
        }
    }

    private fun showUpdateDialog(version: AppVersion) {
        runOnUiThread {
            try {
                Log.d(TAG, "Inflating update dialog...")
                val dialogView = layoutInflater.inflate(R.layout.dialog_update_available, null)
                val releaseNotesTextView = dialogView.findViewById<android.widget.TextView>(R.id.releaseNotesTextView)
                val updateButton = dialogView.findViewById<android.widget.Button>(R.id.updateButton)
                val laterButton = dialogView.findViewById<android.widget.Button>(R.id.laterButton)
                val progressBar = dialogView.findViewById<android.widget.ProgressBar>(R.id.updateProgressBar)

                val dialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create()

                updateDialog = dialog

                releaseNotesTextView.text = version.releaseNotes

                updateButton.setOnClickListener {
                    Log.d(TAG, "Update button clicked for URL: ${version.apkUrl}")
                    startUpdateDownload(version.apkUrl, dialogView)
                }

                laterButton.setOnClickListener {
                    Log.d(TAG, "Later button clicked")
                    dialog.dismiss()
                    updateDialog = null
                }

                dialog.show()
                Log.d(TAG, "Update dialog shown successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing update dialog: ${e.message}", e)
                Toast.makeText(this, R.string.update_error_showing, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startUpdateDownload(apkUrl: String, dialogView: android.view.View) {
        val updateButton = dialogView.findViewById<android.widget.Button>(R.id.updateButton)
        val laterButton = dialogView.findViewById<android.widget.Button>(R.id.laterButton)
        val progressBar = dialogView.findViewById<android.widget.ProgressBar>(R.id.updateProgressBar)

        updateButton.isEnabled = false
        laterButton.isEnabled = false
        updateButton.text = getString(R.string.downloading)
        progressBar.visibility = android.view.View.VISIBLE
        progressBar.progress = 0

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting download from: $apkUrl")
                val url = URL(apkUrl)
                val connection = url.openConnection()
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.connect()

                val contentLength = connection.contentLength
                Log.d(TAG, "Content length: $contentLength")
                val inputStream = connection.getInputStream()

                val apkFile = File(cacheDir, "notepad_update.apk")
                val outputStream = FileOutputStream(apkFile)

                var totalBytesRead = 0
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int

                inputStream.use { input ->
                    outputStream.use { output ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            if (contentLength > 0) {
                                val progress = (totalBytesRead * 100 / contentLength).toInt()
                                if (progress % 10 == 0) { // Log every 10% to avoid overload
                                    Log.d(TAG, "Download progress: $progress%")
                                }
                                withContext(Dispatchers.Main) {
                                    progressBar.progress = progress
                                }
                            }
                        }
                    }
                }

                updateApkFile = apkFile
                Log.d(TAG, "Download completed, file size: ${apkFile.length()} bytes")

                withContext(Dispatchers.Main) {
                    progressBar.visibility = android.view.View.GONE
                    updateButton.text = getString(R.string.install)
                    updateButton.isEnabled = true
                    laterButton.isEnabled = true

                    triggerUpdateInstallation(apkFile)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Download error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = android.view.View.GONE
                    updateButton.text = getString(R.string.try_again)
                    updateButton.isEnabled = true
                    laterButton.isEnabled = true

                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.download_error, e.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun triggerUpdateInstallation(apkFile: File) {
        try {
            Log.d(TAG, "Triggering installation for: ${apkFile.absolutePath}")

            // Check if file exists and has adequate size
            if (!apkFile.exists() || apkFile.length() == 0L) {
                Toast.makeText(this, R.string.corrupted_update_file, Toast.LENGTH_SHORT).show()
                return
            }

            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(this, "${packageName}.fileprovider", apkFile)
            } else {
                Uri.fromFile(apkFile)
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            startActivity(installIntent)
            updateDialog?.dismiss()
            updateDialog = null
            Log.d(TAG, "Installation intent started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Installation error: ${e.message}", e)

            // More specific message
            val errorMsg = when {
                e.message?.contains("conflict") == true ->
                    getString(R.string.package_conflict)
                e.message?.contains("no app") == true ->
                    getString(R.string.no_app_can_open_file)
                else -> getString(R.string.installation_error, e.message)
            }

            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        updateApkFile?.delete()
        updateDialog?.dismiss()
        super.onDestroy()
    }
}