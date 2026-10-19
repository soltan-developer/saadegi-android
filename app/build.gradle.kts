plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ir.saadegi.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "ir.saadegi.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-internal"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Internal preview for current 64-bit Android phones. Do NOT ship a universal multi-ABI APK.
        ndk { abiFilters += "arm64-v8a" }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // Internal compact APK: obfuscation keeps generated libXray JNI interface.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // The debug build exercises native connection in a guarded lab mode.
            // Separate internal native-VPN test package: keep the existing preview untouched.
            applicationIdSuffix = ".lab"
            versionNameSuffix = "-debug"
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        // Compress the Go native .so in APK, then extract at install. Download smaller,
        // but installed footprint remains larger. Not a claim of lower runtime RAM.
        jniLibs.useLegacyPackaging = true
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.core:core-ktx:1.17.0")
    // Version/checksum-pinned upstream MIT wrapper; downloaded by scripts/prepare-libxray.sh.
    implementation(files("libs/libXray.aar"))
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
