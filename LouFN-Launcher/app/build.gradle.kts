plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lou20.loufnlauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lou20.loufnlauncher"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
}
