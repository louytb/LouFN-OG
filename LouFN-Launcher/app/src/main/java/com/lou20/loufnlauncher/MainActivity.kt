package com.lou20.loufnlauncher

import android.app.Activity
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
            text = "La bibliothèque garde les références aux APK d'origine. Android n'autorise qu'une seule installation Fortnite à la fois : installer une autre build remplace la version actuelle. Les anciennes versions peuvent ne plus fonctionner ou ne plus se connecter."
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
            val label = info.applicationInfo?.loadLabel(packageManager)?.toString() ?: "Fortnite"
            val old = readBuilds()
            val saved = JSONArray()
            for (i in 0 until old.length()) {
                val item = old.optJSONObject(i) ?: continue
                if (item.optString("uri") != uri.toString()) saved.put(item)
            }
            saved.put(JSONObject().put("label", label).put("version", version).put("uri", uri.toString()))
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
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val buildVersion = item.optString("version", "Version inconnue")
            val installed = installedVersion()
            val isThisBuildInstalled = installed != null && installed == buildVersion
            row.addView(Button(this@MainActivity).apply {
                text = if (isThisBuildInstalled) "JOUER" else "INSTALLER"
                isAllCaps = false
                setOnClickListener {
                    if (isThisBuildInstalled) launchGame() else installBuild(uri)
                }
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

    private fun removeBuild(index: Int) {
        val old = readBuilds()
        val next = JSONArray()
        for (i in 0 until old.length()) if (i != index) old.optJSONObject(i)?.let { next.put(it) }
        prefs().edit().putString("builds", next.toString()).apply()
        renderBuilds()
    }

    private fun installBuild(uriString: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
                Toast.makeText(this, "Autorise LouFN Launcher à installer des APK, puis réessaie.", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                })
                return
            }
            val uri = Uri.parse(uriString)
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, apkType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (_: Exception) {
            Toast.makeText(this, "APK inaccessible. Réimporte ce fichier.", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun installedVersion(): String? {
        return try { packageManager.getPackageInfo(gamePackage, 0).versionName } catch (_: Exception) { null }
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
