package com.lou20.loufnlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast

class InstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return

        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_SUCCESS -> {
                Toast.makeText(context, "Installation terminée. Lancement de Fortnite…", Toast.LENGTH_LONG).show()
                val launch = context.packageManager.getLaunchIntentForPackage("com.epicgames.fortnite")
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launch)
                } else {
                    Toast.makeText(context, "Fortnite est installé, mais Android ne trouve pas son écran de lancement.", Toast.LENGTH_LONG).show()
                }
            }
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmation = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirmation != null) {
                    confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmation)
                }
            }
            else -> {
                val reason = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Installation refusée ou impossible."
                Toast.makeText(context, "Échec de l'installation : $reason", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.lou20.loufnlauncher.INSTALL_STATUS"
    }
}
