plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun git(vararg args: String): String? = try {
    val process = ProcessBuilder(listOf("git") + args)
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().use { it.readText().trim() }
    if (process.waitFor() == 0 && output.isNotEmpty()) output else null
} catch (e: Exception) {
    null
}

val gitCommitHash: String = git("rev-parse", "--short=8", "HEAD") ?: "unknown"

// Every commit produces a higher versionCode, so a freshly built APK installs
// over the previous one as an update instead of being refused as a downgrade.
val gitCommitCount: Int = git("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 1

// Checked-in signing key: without it every machine (and every CI runner) signs
// with its own generated debug key, and the resulting APKs refuse to replace
// each other because the signatures differ.
val sharedKeystore = rootProject.file("keystore/debug.keystore").takeIf { it.exists() }

@Suppress("UnstableApiUsage")
android {
    namespace = "com.thevakhovske.cunnyplayground"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.thevakhovske.cunnyplayground"
        minSdk = 36
        targetSdk = 37
        versionCode = gitCommitCount
        versionName = "1.0-$gitCommitHash"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (sharedKeystore != null) {
            create("shared") {
                storeFile = sharedKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            if (sharedKeystore != null) {
                signingConfig = signingConfigs.getByName("shared")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (sharedKeystore != null) {
                signingConfigs.getByName("shared")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // MIUIX
    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("io.github.d4viddf:hyperisland_kit:0.4.0")
}