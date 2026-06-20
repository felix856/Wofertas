plugins {
    id("com.google.devtools.ksp")
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

android {
    namespace  = "com.example.wofertas"
    compileSdk = 35

    defaultConfig {
        applicationId   = "com.example.wofertas"
        minSdk          = 23
        targetSdk       = 35
        versionCode     = 1
        versionName     = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { viewBinding = true }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}


dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // AndroidX
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.activity.ktx)
    implementation(libs.fragment.ktx)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)

    // Navigation
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation("io.coil-kt:coil:2.7.0")
    // Retrofit / OkHttp
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Glide
    implementation(libs.glide)

    // OSMdroid — mapa open source, sem necessidade de API key
    implementation(libs.osmdroid.android)

    // GPS
    implementation(libs.play.services.location)

    // Outros
    implementation(libs.threetenabp)


    // ── Room (cache local) ────────────────────────────────────────────────────
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // ── Security Crypto (EncryptedSharedPreferences) ──────────────────────────
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ── Lifecycle runtime (repeatOnLifecycle para StateFlow) ──────────────────
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // ML Kit OCR
    implementation(libs.mlkit.text.recognition)

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
}