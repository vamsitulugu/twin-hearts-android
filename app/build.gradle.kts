// App module build file. R8 shrinking is ON for release; nothing debug-only ships in release.
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// API base URL comes from local.properties (not committed). The fallback is the public
// Render address the web app already uses; it is not a secret.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val apiBaseUrl: String = localProps.getProperty("API_BASE_URL") ?: "https://us-app-av6d.onrender.com/"

// Continue with Google. This is the *Web* OAuth client id from Google Cloud (a public id, not a secret).
// It must be the same value as GOOGLE_CLIENT_ID on the server. Empty = the Google button is hidden.
val googleWebClientId: String = localProps.getProperty("GOOGLE_WEB_CLIENT_ID") ?: ""

// Ads (AdMob). Google's public TEST ids: debug builds always use these, so tapping ads while testing is safe.
// Real ids go in local.properties (ADMOB_APP_ID, ADMOB_BANNER_ID, ADMOB_INTERSTITIAL_ID). They are not secrets,
// but keeping them out of git lets you switch accounts without editing code. Release builds show ads ONLY when all three exist.
val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val admobTestBannerId = "ca-app-pub-3940256099942544/9214589741"
val admobTestInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val admobTestRewardedId = "ca-app-pub-3940256099942544/5224354917"
val admobAppId: String? = localProps.getProperty("ADMOB_APP_ID")
val admobBannerId: String? = localProps.getProperty("ADMOB_BANNER_ID")
val admobInterstitialId: String? = localProps.getProperty("ADMOB_INTERSTITIAL_ID")
// Rewarded ads ("watch an ad for coins / a free spin") need their own ad unit: ADMOB_REWARDED_ID in local.properties.
// Without it a release build simply hides the watch-ad buttons; the other ads are not affected.
val admobRewardedId: String? = localProps.getProperty("ADMOB_REWARDED_ID")
val adsReleaseReady = admobAppId != null && admobBannerId != null && admobInterstitialId != null

android {
    namespace = "com.twinhearts.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.twinhearts.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "0.6.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobTestBannerId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobTestInterstitialId\"")
            buildConfigField("boolean", "REWARDED_ENABLED", "true")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$admobTestRewardedId\"")
            manifestPlaceholders["admobAppId"] = admobTestAppId
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            buildConfigField("boolean", "ADS_ENABLED", adsReleaseReady.toString())
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${admobBannerId ?: admobTestBannerId}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${admobInterstitialId ?: admobTestInterstitialId}\"")
            buildConfigField("boolean", "REWARDED_ENABLED", (adsReleaseReady && admobRewardedId != null).toString())
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"${admobRewardedId ?: admobTestRewardedId}\"")
            manifestPlaceholders["admobAppId"] = admobAppId ?: admobTestAppId
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    // The bundled cinema (assets/cinema) has big media. Stored uncompressed they can be read by any byte range (seeking in the ad video and sounds)
    // and they are already compressed formats, so the APK is not bigger for it.
    androidResources {
        noCompress += listOf("glb", "mp3", "mp4", "wav", "ogg", "webp")
    }
}

// Room writes its schema files here so database migrations can be checked later.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.serialization)
    implementation(libs.okhttp)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.maplibre.android)
    implementation(libs.stream.webrtc)
    implementation(libs.razorpay.checkout)
    implementation(libs.play.services.ads)

    // Continue with Google
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Push notifications: the receiver service is always compiled; it only wakes up once google-services.json is added.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Push notifications: Firebase is switched on only when you add app/google-services.json (download it from
// the Firebase console). Without the file the app still builds and runs; pushes just stay off.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
