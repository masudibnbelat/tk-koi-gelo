// FILE: app/build.gradle.kts
import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ---- Automatic versioning from git tag (Kotlin DSL) ----
// versionName = latest git tag  (v2 -> "2")
// versionCode = total commit count (always increases, never edit by hand)
// No git / no tag -> safe fallback "1.0" / 1, build never breaks.
fun gitTagVersionName(): String = try {
    val out = ByteArrayOutputStream()
    exec {
        commandLine("git", "describe", "--tags", "--abbrev=0")
        standardOutput = out
    }
    out.toString().trim().removePrefix("v")
} catch (e: Exception) {
    "1.0"
}

fun gitCommitCount(): Int = try {
    val out = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        standardOutput = out
    }
    out.toString().trim().toInt()
} catch (e: Exception) {
    1
}

android {
    namespace = "com.masud.moneymanager"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.masud.moneymanager"
        minSdk = 26
        targetSdk = 35
        versionCode = gitCommitCount()
        versionName = gitTagVersionName()
    }

    buildTypes {
        release {
            // Sign release with the existing debug keystore (~/.android/debug.keystore).
            // No new key, no password, no env vars needed.
            // Same key as installDebug builds -> updates install over the current app, no data loss.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))

    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // Home-screen task widget (Glance)
    implementation("androidx.glance:glance-appwidget:1.1.0")


    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")





    debugImplementation("androidx.compose.ui:ui-tooling")
}
