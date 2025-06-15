pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()


    }
    plugins {
        id("com.android.application") version "8.10.0" apply false
        id("org.jetbrains.kotlin.android") version "1.8.21" apply false
        id("com.google.gms.google-services") version "4.4.2" apply false
        id("com.google.gms.google-services")
        id("com.android.application")

    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Outros repositórios que precisar, ex. jitpack:
         maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "Wofertas"
include(":app")
