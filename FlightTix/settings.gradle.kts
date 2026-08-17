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
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")

        // The Identus Edge Agent SDK (org.hyperledger.identus:edge-agent-sdk-android)
        // pulls a native dependency — org.hyperledger:anoncreds_uniffi-android — that is
        // only published to GitHub Packages, NOT Maven Central. Resolving it requires a
        // GitHub username + a Personal Access Token with the `read:packages` scope.
        //
        // Provide them via ~/.gradle/gradle.properties:
        //     gpr.user=<your-github-username>
        //     gpr.token=<your-PAT-with-read:packages>
        // or via environment variables GITHUB_ACTOR / GITHUB_TOKEN.
        maven {
            name = "GitHubPackagesAriesUniffi"
            url = uri("https://maven.pkg.github.com/LF-Decentralized-Trust-labs/aries-uniffi-wrappers")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.token").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

rootProject.name = "FlightTix"
include(":app")
