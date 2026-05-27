pluginManagement {

    repositories {

        google()

        mavenCentral() // Manter aqui para plugins

        gradlePluginPortal()

    }

}



dependencyResolutionManagement {

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral() // <--- ESTA LINHA É A CHAVE PARA A BIBLIOTECA DO CALENDÁRIO KIZITONWOSE

        maven { url = uri("https://jitpack.io") } // Mantenha isso se você ainda tem outras libs do Jitpack

    }

}



rootProject.name = "Wofertas"

include(":app")