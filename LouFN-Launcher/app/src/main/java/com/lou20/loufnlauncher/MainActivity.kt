package com.lou20.loufnlauncher

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class MainActivity : Activity() {
    private val gamePackage = "com.epicgames.fortnite"
    private val apkType = "application/vnd.android.package-archive"
    private lateinit var status: TextView
    private lateinit var launchButton: Button
    private lateinit var buildsView: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 12, 20)
        window.navigationBarColor = Color.rgb(10, 12, 20)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 22, 20, 18)
            setBackgroundColor(Color.rgb(10, 12, 20))
        }
        root.addView(TextView(this).apply {
            text = "LOUFN"
            textSize = 36f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }, lp())
        root.addView(TextView(this).apply {
            text = "BIBLIOTHÈQUE DE BUILDS"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(90, 170, 255))
        }, lp())
        status = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 16, 0, 12)
        }
        root.addView(status, lp())
        root.addView(Button(this).apply {
            text = "＋ IMPORTER UNE BUILD APK"
            isAllCaps = false
            setOnClickListener { pickApk() }
        }, lp())
        launchButton = Button(this).apply {
            text = "LANCER LA BUILD INSTALLÉE"
            isAllCaps = false
            setOnClickListener { launchGame() }
        }
        root.addView(launchButton, lp())
        root.addView(TextView(this).apply {
            text = "MES BUILDS"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 18, 0, 8)
        }, lp())
        buildsView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(buildsView) },
            LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(this).apply {
            text = "LouFN conserve les références aux APK importés et peut vérifier leur intégrité. Android doit installer un APK pour l'exécuter : cette bibliothèque ne lance pas directement un jeu non installé."
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            setPadding(0, 10, 0, 0)
        }, lp())
        setContentView(root)
        refresh()
        renderBuilds()
    }

    private fun lp() = LinearLayout.LayoutParams(-1, -2)

    private fun pickApk() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = apkType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, 4102)
    }

    @Deprecated("Legacy picker callback for Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 4102 || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        validateAndSave(uri, data.flags)
    }

    private fun validateAndSave(uri: Uri, flags: Int) {
        var temp: File? = null
        try {
            val tempFile = File.createTempFile("loufn-", ".apk", cacheDir)
            temp = tempFile
            contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Fichier illisible" }
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            @Suppress("DEPRECATION")
            val info = packageManager.getPackageArchiveInfo(tempFile.absolutePath, 0)
            if (info?.packageName != gamePackage) {
                Toast.makeText(this, "Ce fichier n'est pas un APK Fortnite Android valide.", Toast.LENGTH_LONG).show()
                return
            }
            try {
                contentResolver.takePersistableUriPermission(uri, flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
                Toast.makeText(this, "Android ne permet pas de mémoriser l'accès à ce fichier. Il faudra peut-être le réimporter.", Toast.LENGTH_LONG).show()
            }
            val version = info.versionName ?: "Version inconnue"
            @Suppress("DEPRECATION")
            val versionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
            val sha256 = sha256Of(tempFile)
            val label = info.applicationInfo?.loadLabel(packageManager)?.toString() ?: "Fortnite"
            val old = readBuilds()
            val saved = JSONArray()
            for (i in 0 until old.length()) {
                val item = old.optJSONObject(i) ?: continue
                if (item.optString("uri") != uri.toString()) saved.put(item)
            }
            saved.put(JSONObject()
                .put("label", label)
                .put("version", version)
                .put("versionCode", versionCode)
                .put("sha256", sha256)
                .put("uri", uri.toString()))
            prefs().edit().putString("builds", saved.toString()).apply()
            Toast.makeText(this, "Build $version ajoutée à la bibliothèque.", Toast.LENGTH_LONG).show()
            renderBuilds()
        } catch (_: Exception) {
            Toast.makeText(this, "Impossible de lire ou vérifier cet APK.", Toast.LENGTH_LONG).show()
        } finally {
            temp?.delete()
        }
    }

    private fun prefs() = getSharedPreferences("loufn_library", MODE_PRIVATE)
    private fun readBuilds() = try { JSONArray(prefs().getString("builds", "[]")) } catch (_: Exception) { JSONArray() }

    private fun renderBuilds() {
        buildsView.removeAllViews()
        val list = readBuilds()
        if (list.length() == 0) {
            buildsView.addView(TextView(this).apply {
                text = "Aucune build enregistrée. Importe un APK pour commencer."
                textSize = 13f
                setTextColor(Color.LTGRAY)
                setPadding(4, 12, 4, 12)
            })
            return
        }
        for (index in 0 until list.length()) {
            val item = list.optJSONObject(index) ?: continue
            val uri = item.optString("uri")
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 10, 12, 10)
                setBackgroundColor(Color.rgb(25, 30, 44))
            }
            card.addView(TextView(this).apply {
                text = item.optString("label", "Fortnite") + "  •  " + item.optString("version", "Version inconnue")
                textSize = 14f
                setTextColor(Color.WHITE)
            }, lp())
            card.addView(TextView(this).apply {
                val code = item.optLong("versionCode", -1L)
                val hash = item.optString("sha256", "")
                text = "VersionCode : " + (if (code >= 0L) code.toString() else "non vérifié") +
                    (if (hash.isNotBlank()) "\nSHA-256 : " + hash.take(16) + "…" else "\nAncienne entrée : réimporte l'APK pour vérifier son intégrité.")
                textSize = 10f
                setTextColor(Color.LTGRAY)
                setPadding(0, 4, 0, 8)
            }, lp())
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val buildVersionCode = item.optLong("versionCode", -1L)
            row.addView(Button(this@MainActivity).apply {
                text = if (buildVersionCode > 0L && installedVersionCode() == buildVersionCode) "JOUER" else "INSTALLER"
                isAllCaps = false
                setOnClickListener {
                    if (buildVersionCode > 0L && installedVersionCode() == buildVersionCode) {
                        launchGame()
                    } else {
                        installBuild(uri, item.optString("sha256", ""))
                    }
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(Button(this@MainActivity).apply {
                text = "VÉRIFIER"
                isAllCaps = false
                setOnClickListener { verifyBuild(uri, item.optString("sha256", "")) }
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(Button(this@MainActivity).apply {
                text = "Retirer"
                isAllCaps = false
                setOnClickListener { removeBuild(index) }
            }, LinearLayout.LayoutParams(-2, -2))
            card.addView(row, lp())
            buildsView.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        }
    }

    private fun verifyBuildFile(uriString: String, expectedHash: String): Boolean {
        if (expectedHash.isBlank()) return false
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            contentResolver.openInputStream(Uri.parse(uriString)).use { input ->
                requireNotNull(input) { "Fichier inaccessible" }
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            actualHash.equals(expectedHash, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    private fun verifyBuild(uriString: String, expectedHash: String) {
        val matches = verifyBuildFile(uriString, expectedHash)
        Toast.makeText(
            this,
            if (matches) "Vérification réussie : APK inchangé."
            else "Vérification impossible ou empreinte différente. Réimporte le fichier.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun removeBuild(index: Int) {
        val old = readBuilds()
        val next = JSONArray()
        for (i in 0 until old.length()) if (i != index) old.optJSONObject(i)?.let { next.put(it) }
        prefs().edit().putString("builds", next.toString()).apply()
        renderBuilds()
    }

    private fun installBuild(uriString: String, expectedHash: String) {
        try {
            if (!verifyBuildFile(uriString, expectedHash)) {
                Toast.makeText(this, "APK modifié ou inaccessible. Réimporte la build avant installation.", Toast.LENGTH_LONG).show()
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
                Toast.makeText(this, "Autorise LouFN Launcher à installer des APK, puis réessaie.", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                })
                return
            }
            val uri = Uri.parse(uriString)
            val installer = packageManager.packageInstaller
            val fileSize = contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            if (fileSize <= 0L) {
                Toast.makeText(this, "Taille de l'APK impossible à lire.", Toast.LENGTH_LONG).show()
                return
            }
            val sessionParams = android.content.pm.PackageInstaller.SessionParams(
                android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
            ).apply {
                setAppPackageName(gamePackage)
                setSize(fileSize)
            }
            val sessionId = installer.createSession(sessionParams)
            try {
                installer.openSession(sessionId).use { session ->
                    contentResolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { "APK inaccessible" }
                        session.openWrite("fortnite.apk", 0, fileSize).use { output ->
                            input.copyTo(output)
                            session.fsync(output)
                        }
                    }
                    val callback = Intent(this, InstallStatusReceiver::class.java)
                        .setAction(InstallStatusReceiver.ACTION_INSTALL_STATUS)
                    val pending = PendingIntent.getBroadcast(
                        this,
                        sessionId,
                        callback,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )
                    session.commit(pending.intentSender)
                }
                Toast.makeText(this, "Installation Android lancée. Confirme la demande affichée.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                try { installer.abandonSession(sessionId) } catch (_: Exception) {}
                throw e
            }
        } catch (_: Exception) {
            Toast.makeText(this, "APK inaccessible. Réimporte ce fichier.", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun installedVersion(): String? {
        return try { packageManager.getPackageInfo(gamePackage, 0).versionName } catch (_: Exception) { null }
    }

    @Suppress("DEPRECATION")
    private fun installedVersionCode(): Long? {
        return try {
            val info = packageManager.getPackageInfo(gamePackage, 0)
            if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        } catch (_: Exception) { null }
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun refresh() {
        val version = installedVersion()
        if (version == null) {
            status.text = "Aucune build Fortnite installée."
            launchButton.isEnabled = false
        } else {
            status.text = "BUILD INSTALLÉE : $version"
            launchButton.isEnabled = true
        }
        if (::buildsView.isInitialized) renderBuilds()
    }

    private fun launchGame() {
        val intent = packageManager.getLaunchIntentForPackage(gamePackage)
        if (intent == null) {
            Toast.makeText(this, "Fortnite ne peut pas être lancé.", Toast.LENGTH_LONG).show()
            refresh()
        } else try { startActivity(intent) } catch (_: Exception) {
            Toast.makeText(this, "Échec du lancement.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refresh()
    }
}
