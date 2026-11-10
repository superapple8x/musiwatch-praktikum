# TAMBAHAN: aturan proguard untuk build release. P1 tidak memakai minify (isMinifyEnabled=false),
# berkas ini disiapkan agar konfigurasi build.gradle.kts valid saat release dibangun.
-keep class id.unsri.musiwatch.net.** { *; }
