# LouFN Launcher

Native Android launcher app built with Kotlin.

## Current features
- Detects the Android Fortnite package `com.epicgames.fortnite`.
- Displays the installed app version.
- Launches the installed Fortnite app.
- Lets the user pick a single APK through Android's document picker.
- Checks that the selected APK declares the Fortnite Android package name before handing it to Android's package installer.
- Android displays the installation confirmation; LouFN does not silently install apps.

## Important limitations
- A single APK can be imported. Split APK bundles (such as APKS/XAPK) are not supported by this importer.
- Fortnite versions using the same Android package ID replace one another; Android does not install them side-by-side as separate apps.
- An APK alone may not contain all game data or guarantee that a Fortnite build can connect to its services.
- LouFN does not provide Fortnite files, download builds, bypass anti-cheat, or bypass Epic services. Use only files you are authorized to install and official supported sources.

## Build
Open GitHub Actions → **Build LouFN Launcher APK** → download the **LouFN-Launcher-debug-apk** artifact after a successful run.
