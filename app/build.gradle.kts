// Copyright (c) RUNE Systems LLC 2026
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.rune.watch"
    compileSdk = 35

    val ciVersionCode =
        System.getenv("RUNE_WATCH_VERSION_CODE")?.toIntOrNull()
            ?: System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()

    defaultConfig {
        applicationId = "com.rune.watch"
        minSdk = 30          // WearOS 3.x minimum
        targetSdk = 35
        versionCode = ciVersionCode ?: 1
        versionName = "0.1.0"
    }

    val releaseStoreFile = providers.gradleProperty("RUNE_WATCH_RELEASE_STORE_FILE").orNull
    val releaseStorePassword = providers.gradleProperty("RUNE_WATCH_RELEASE_STORE_PASSWORD").orNull
    val releaseKeyAlias = providers.gradleProperty("RUNE_WATCH_RELEASE_KEY_ALIAS").orNull
    val releaseKeyPassword = providers.gradleProperty("RUNE_WATCH_RELEASE_KEY_PASSWORD").orNull

    signingConfigs {
        if (!releaseStoreFile.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Workaround for AGP/Kotlin analyzer incompatibility in CI release analysis.
        disable += "NullSafeMutableLiveData"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)

    // Compose for Wear OS
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)

    // Horologist
    implementation(libs.horologist.compose.layout)
    implementation(libs.horologist.compose.material)
    implementation(libs.horologist.tiles)
    implementation(libs.horologist.data.layer)

    // Tiles & Complications
    implementation(libs.wear.tiles)
    implementation(libs.wear.tiles.material)
    implementation(libs.wear.complications.provider)

    // Network / coroutines / persistence
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)

    testImplementation(kotlin("test"))
    testImplementation("org.json:json:20240303")
}
