// FILE: app/build.gradle
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ---- Automatic versioning from git tag ----
// versionName = latest git tag  (v1.2 -> "1.2")
// versionCode = total commit count (always increases, never edit by hand)
// No git / no tag -> safe fallback "1.0" / 1, build never breaks.
def getVersionName = { ->
    try {
        def out = new ByteArrayOutputStream()
        exec {
            commandLine "git", "describe", "--tags", "--abbrev=0"
            standardOutput = out
        }
        return out.toString().trim().replaceFirst(/^v/, "")
    } catch (Exception ignored) {
        return "1.0"
    }
}

def getVersionCode = { ->
    try {
        def out = new ByteArrayOutputStream()
        exec {
            commandLine "git", "rev-list", "--count", "HEAD"
            standardOutput = out
        }
        return out.toString().trim().toInteger()
    } catch (Exception ignored) {
        return 1
    }
}

android {
    namespace = "com.masud.moneymanager"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.masud.moneymanager"
        minSdk = 26
        targetSdk = 35
        versionCode = getVersionCode()
        versionName = getVersionName()
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
