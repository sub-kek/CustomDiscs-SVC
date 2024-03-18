plugins {
    id("java-library")
    id("io.github.kota65535.dependency-report") version "2.0.1"
    id("com.github.johnrengelman.shadow") version ("8.1.1")
}

allprojects {
    group = "io.github.subkek.customdiscs"
    version = "1.3.7"
}

java.sourceCompatibility = JavaVersion.VERSION_16
java.targetCompatibility = JavaVersion.VERSION_16
java.disableAutoTargetJvm()

val apiAndDocs: Configuration by configurations.creating {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
}
configurations.api {
    extendsFrom(apiAndDocs)
}

dependencies {
    //compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")

    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.googlecode.json-simple:json-simple:1.1.1")
    compileOnly("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    compileOnly("de.maxhenkel.voicechat:voicechat-api:2.3.3")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0")
    compileOnly("org.jflac:jflac-codec:1.5.2")
    compileOnly("com.sedmelluq:lavaplayer:1.3.78")
    compileOnly("commons-io:commons-io:2.14.0")
    compileOnly("org.projectlombok:lombok:1.18.30")

    compileOnly(platform("net.kyori:adventure-bom:4.13.1"))
    compileOnly("net.kyori:adventure-api")
    compileOnly("net.kyori:adventure-text-minimessage")
    compileOnly("net.kyori:adventure-text-serializer-gson")
    compileOnly("net.kyori:adventure-text-serializer-legacy")
    compileOnly("net.kyori:adventure-text-serializer-plain")
    compileOnly("net.kyori:adventure-text-logger-slf4j")
    compileOnly("net.kyori:adventure-text-serializer-legacy")
    compileOnly("net.kyori:adventure-platform-bukkit:4.3.2")

    compileOnly("org.yaml:snakeyaml:2.2")
    compileOnly ("me.carleslc.Simple-YAML:Simple-Yaml:1.8.4") {
        exclude(group="org.yaml", module="snakeyaml")
    }

    implementation("me.lucko:jar-relocator:1.7.2")

    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    archiveFileName = "${rootProject.name}-$version.jar"
    relocate("net.kyori", "io.github.subkek.customdiscs.libs.net.kyori")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(project.properties)
    }
}