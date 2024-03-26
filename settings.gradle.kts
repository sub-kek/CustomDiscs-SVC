enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://maven.maxhenkel.de/repository/public")
        maven("https://jitpack.io")
        maven("https://maven.lavalink.dev/snapshots")
        maven("https://repo.maven.apache.org/maven2/")
        maven("https://repo.viaversion.com/")
        maven("https://maven.firstdarkdev.xyz/releases/")
    }
}

rootProject.name = "CustomDiscs"