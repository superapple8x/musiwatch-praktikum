// AGP 9.x mendukung Kotlin secara bawaan; plugin org.jetbrains.kotlin.android ditolak
// ("no longer required for Kotlin support since AGP 9.0").
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
