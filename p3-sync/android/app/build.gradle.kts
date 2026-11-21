plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "id.unsri.musiwatch"
    compileSdk = 36

    defaultConfig {
        applicationId = "id.unsri.musiwatch"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

// TAMBAHAN: mesin ini hanya punya JDK 25 (tidak ada JDK 17), jadi jvmToolchain(17) dicabut.
// AGP menurunkan bytecode ke target Java 17 lewat compileOptions di atas, dan Kotlin mengikuti
// jvmTarget yang sama. JDK 17+ tetap memenuhi syarat AGP 9.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
