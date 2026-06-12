pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral() // Menjamin Firebase, ExoPlayer, dan Play Services terunduh secara resmi terpusat
    }
}

rootProject.name = "JobsKuy"
include(":app")