import org.apache.tools.ant.util.JavaEnvUtils

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version ("8.1.1")
}

allprojects {
    apply {
        plugin("java")
    }

    group = "io.github.subkek.customdiscs"
    version = "1.3.5"
}

java.sourceCompatibility = JavaVersion.VERSION_16
java.targetCompatibility = JavaVersion.VERSION_16
java.disableAutoTargetJvm()

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.googlecode.json-simple:json-simple:1.1.1")
    compileOnly("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    compileOnly("de.maxhenkel.voicechat:voicechat-api:2.3.3")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0")
    compileOnly("org.jflac:jflac-codec:1.5.2")
    compileOnly("com.sedmelluq:lavaplayer:1.3.78")
    compileOnly("com.fasterxml.jackson.core:jackson-core:2.15.3")
    compileOnly("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    compileOnly("commons-io:commons-io:2.14.0")
    compileOnly("org.projectlombok:lombok:1.18.30")

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
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(project.properties)
    }
}