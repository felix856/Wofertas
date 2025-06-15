import org.gradle.kotlin.dsl.implementation

plugins {

    id("com.android.application")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.android")

}

android {
    namespace = "com.example.wofertas"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.wofertas"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // ——————————————————————————————————————————————————————————
    // Firebase BoM (gerencia versões de Auth, Firestore, Storage etc.)
    implementation(platform(libs.firebase.bom.v33140))

    // Firebase Authentication
    implementation(libs.firebase.auth.ktx)

    // Firebase Firestore
    implementation(libs.com.google.firebase.firebase.firestore)

    // Firebase Storage
    implementation(libs.com.google.firebase.firebase.storage)

    // (Se ainda usar Realtime Database em alguma parte, senão pode remover)
    implementation(libs.firebase.database.ktx)

    // ——————————————————————————————————————————————————————————
    // Google Play Services:
    //   • Autenticação Google Sign-In
    implementation(libs.play.services.auth)

    //   • Localização (FusedLocationProviderClient)
    implementation(libs.play.services.location)

    //   • Google Maps
    implementation(libs.play.services.maps)

    // ——————————————————————————————————————————————————————————
    // Glide (para carregar thumbnails)
    implementation(libs.glide)

    // ——————————————————————————————————————————————————————————
    // PDF Renderer nativo (Jetpack)
    // Não é obrigatório adicionar nenhuma dependência extra para PdfRenderer,
    // pois ele está disponível no AndroidX padrão em API 21+.
    // Se você quiser usar alguma biblioteca de visualização de PDF externa,
    // inclua aqui. Caso esteja usando somente PdfRenderer, remova estas linhas:
    // implementation(libs.pdf.viewer.fragment)
    // releaseImplementation(libs.android.pdf.viewer)

    // ——————————————————————————————————————————————————————————
    // AndroidX + Material UI
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.activity.ktx.v1101)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    // ——————————————————————————————————————————————————————————
    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ktx)
    androidTestImplementation(libs.espresso.core)

}
