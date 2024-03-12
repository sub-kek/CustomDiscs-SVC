enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://maven.maxhenkel.de/repository/public")
        maven("https://jitpack.io")
        maven("https://m2.dv8tion.net/releases")
        maven("https://repo.maven.apache.org/maven2/")
    }
}

rootProject.name = "CustomDiscs"

include(":bukkit")
include("folia-provider")
