package com.lou20.loufnlauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {
    private val fortnitePackage = "com.epicgames.fortnite"
    private val apkMimeType = "application/vnd.android.package-archive"
    private lateinit var status: TextView
    private lateinit var playButton: Button
    private lateinit var importButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 12, 20)
        window.navigationBarColor = Color.rgb(10, 12, 20)
        window.decorView.systemUiVisibility = 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 36, 28, 36)
            setBackgroundColor(Color.rgb(10, 12, 20))
        }

        val title = TextView(this).apply {
            text = "LOUFN"
            textSize = 38f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "GESTIONNAIRE DE BUILDS"
            textSize = 15f
            letterSpacing = 0.25f
            setTextColor(Color.rgb(90, 170, 255))
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 24)
        }
        importButton = Button(this).apply {
            text = "IMPORTER UNE BUILD APK"
            isAllCaps = false
            setOnClickListener { chooseBuildApk() }
        }
        playButton = Button(this).apply {
            text = "LANCER FORTNITE"
            isAllCaps = false
            setOnClickListener { launchFortnite() }
        }
        val refresh = Button(this).apply {
            text = "Vérifier la version installée"
            isAllCaps = false
            setOnClickListener { refreshStatus() }
        }
        val note = TextView(this).apply {
            text = "Importe un APK Fortnite officiel que tu possèdes. Android te demandera de confirmer l'installation. Une nouvelle version remplace l'ancienne ; les builds ne peuvent pas cohabiter avec le même identifiant."
            textSize = 12f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }

        root.addView(title, match())
        root.addView(subtitle, match())
        root.addView(status, match())
        root.addView(importButton, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        root.addView(playButton, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        root.addView(refresh, match())
        root.addView(note, match())
        setContentView(root)
        refreshStatus()
    }

    private fun match() = LinearLayout.LayoutParams(-1, -2)

    private fun chooseBuildApk() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = apkMimeType
        }
        try {
            startActivityForResult(intent, REQUEST_PICK_APK)
        } catch (_: Exception) {
            Toast.makeText(this, "Impossible d'ouvrir le sélecteur de fichiers.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Using the activity result API keeps compatibility with the minimum Android version")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK_APK || resultCode != RESULT_OK) return
        val apkUri: Uri = data?.data ?: return
        validateAndInstallApk(apkUri)
    }

    private fun validateAndInstallApk(apkUri: Uri) {
        var tempApk: File? = null
        try {
            val tempFile = File.createTempFile("loufn-check-", ".apk", cacheDir)
            tempApk = tempFile
            contentResolver.openInputStream(apkUri).use { input ->
                if (input == null) throw IllegalStateException("Fichier illisible")
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }

            @Suppress("DEPRECATION")
            val archiveInfo = packageManager.getPackageArchiveInfo(
                tempFile.absolutePath,
                PackageManager.GET_ACTIVITIES
            )
            if (archiveInfo == null) {
                Toast.makeText(this, "Ce fichier APK est invalide ou illisible.", Toast.LENGTH_LONG).show()
                return
            }
            if (archiveInfo.packageName != fortnitePackage) {
                Toast.makeText(
                    this,
                    "Ce n'est pas un APK Fortnite Android reconnu. Aucun changement effectué.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val version = archiveInfo.versionName ?: "version inconnue"
            Toast.makeText(
                this,
                "Build Fortnite détectée : $version. Confirme l'installation dans Android.",
                Toast.LENGTH_LONG
            ).show()

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, apkMimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(installIntent)
        } catch (_: SecurityException) {
            Toast.makeText(this, "LouFN n'a pas pu lire ce fichier. Choisis un APK accessible.", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Impossible de vérifier cet APK.", Toast.LENGTH_LONG).show()
        } finally {
            tempApk?.delete()
        }
    }

    private fun refreshStatus() {
        val info = try {
            packageManager.getPackageInfo(fortnitePackage, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Exception) {
            null
        }

        if (info == null) {
            status.text = "Aucune build Fortnite détectée comme installée."
            playButton.isEnabled = false
        } else {
            @Suppress("DEPRECATION")
            val version = info.versionName ?: "Version inconnue"
            status.text = "Build installée détectée\nVersion : $version"
            playButton.isEnabled = true
        }
    }

    private fun launchFortnite() {
        val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(fortnitePackage)
        if (launchIntent == null) {
            Toast.makeText(this, "Impossible de lancer Fortnite. Vérifie son installation.", Toast.LENGTH_LONG).show()
            refreshStatus()
            return
        }
        try {
            startActivity(launchIntent)
        } catch (_: Exception) {
            Toast.makeText(this, "Le lancement de Fortnite a échoué.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshStatus()
    }

    companion object {
        private const val REQUEST_PICK_APK = 4102
    }
}
