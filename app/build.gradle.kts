plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.core.voidapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.core.voidapp"
        minSdk = 23
        targetSdk = 35
        versionCode = 5
        versionName = "0.3.1"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Needed for java.time (LocalDate etc.) to work below API 26.
        // Your Lenovo runs Android 9 / API 28 so it doesn't strictly need this,
        // but minSdk 23 means the app could otherwise crash on older test devices.
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
}
