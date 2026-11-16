package com.lou20.loufnlauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val fortnitePackage = "com.epicgames.fortnite"
    private lateinit var status: TextView
    private lateinit var playButton: Button

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
            text = "LAUNCHER"
            textSize = 15f
            letterSpacing = 0.35f
            setTextColor(Color.rgb(90, 170, 255))
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 28)
        }
        playButton = Button(this).apply {
            text = "JOUER À FORTNITE"
            isAllCaps = false
            setOnClickListener { launchFortnite() }
        }
        val refresh = Button(this).apply {
            text = "Vérifier à nouveau"
            isAllCaps = false
            setOnClickListener { refreshStatus() }
        }
        val note = TextView(this).apply {
            text = "LouFN Launcher lance une installation officielle compatible. Il ne fournit pas les fichiers du jeu."
            textSize = 12f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 0)
        }

        root.addView(title, match())
        root.addView(subtitle, match())
        root.addView(status, match())
        root.addView(playButton, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        root.addView(refresh, LinearLayout.LayoutParams(-1, -2))
        root.addView(note, match())
        setContentView(root)
        refreshStatus()
    }

    private fun match() = LinearLayout.LayoutParams(-1, -2)

    private fun refreshStatus() {
        val info = try {
            packageManager.getPackageInfo(fortnitePackage, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Exception) {
            null
        }

        if (info == null) {
            status.text = "Fortnite n'est pas détecté sur cet appareil."
            playButton.isEnabled = false
        } else {
            @Suppress("DEPRECATION")
            val version = info.versionName ?: "Version inconnue"
            status.text = "Fortnite détecté\nVersion : $version"
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
}
