pluginManagement {
    repositories {
        maven("https://maven.myket.ir/")
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.myket.ir/")
        mavenCentral()
        google()
    }
}

rootProject.name = "arisam-tunes-backend"
