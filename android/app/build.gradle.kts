import java.io.FileInputStream
import java.util.Properties

/**
 * Release signing, from a keystore that is never in this repository.
 *
 * Locally: put keystore.properties beside this file (it is gitignored).
 * In CI: the same four values arrive as environment variables.
 * With neither, the release build simply goes out unsigned rather than
 * failing — a debug build must keep working on a fresh clone.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) FileInputStream(file).use { load(it) }
}

fun signing(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

val storeFilePath = signing("storeFile", "SNIPS_STORE_FILE")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "app.snips"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.snips"
        minSdk = 26
        targetSdk = 35
        // CI passes these from the release tag; a local build gets the defaults.
        versionCode = (findProperty("snipsVersionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("snipsVersionName") as String?) ?: "0.1"
    }

    signingConfigs {
        if (storeFilePath != null) {
            create("release") {
                storeFile = file(storeFilePath)
                storePassword = signing("storePassword", "SNIPS_STORE_PASSWORD")
                keyAlias = signing("keyAlias", "SNIPS_KEY_ALIAS")
                keyPassword = signing("keyPassword", "SNIPS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

// Room writes the schema out so migrations have something to diff against
// once there's a version 2.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.browser)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.coil.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
