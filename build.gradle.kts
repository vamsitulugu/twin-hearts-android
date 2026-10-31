// Root build file: declares plugins once; the app module applies them.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // Applied by the app only when app/google-services.json exists (see app/build.gradle.kts).
    alias(libs.plugins.google.services) apply false
}
