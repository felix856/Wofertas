// C:\Users\Felix\AndroidStudioProjects\Wofertas\build.gradle.kts

// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Define plugins que estarão disponíveis nos módulos, mas não aplicados aqui
plugins {
    id("com.google.devtools.ksp") version "2.1.10-1.0.30" apply false

    id("com.android.application") version "8.13.2" apply false // 8.10.1 é Alpha/Beta, use 8.7.3 se quiser estabilidade
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.10" apply false
}


buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {


    }
}


// Geralmente, o task clean vai depois de `allprojects` e `buildscript`.
// Vou reativar e corrigir a sintaxe para Kotlin DSL
tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory) // Sintaxe mais moderna para diretório de build
}