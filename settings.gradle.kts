pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io")
        maven(url = "https://maven.fabric.io/public")
        maven {
            url = uri("https://git.lepudev.com/api/v4/projects/268/packages/maven")
            name  = "blepro"

            credentials(HttpHeaderCredentials::class) {
                name = "Deploy-Token"
                value = "jMCHXv-upTT47AXegB1E"
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://maven.fabric.io/public")
        maven {
            url = uri("https://git.lepudev.com/api/v4/projects/268/packages/maven")
            name  = "blepro"

            credentials(HttpHeaderCredentials::class) {
                name = "Deploy-Token"
                value = "jMCHXv-upTT47AXegB1E"
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}

rootProject.name = "SleepDemo"
include(":app")
 