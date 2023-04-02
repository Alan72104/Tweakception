import java.io.FileInputStream
import java.util.*

plugins {
    idea
    java
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "a7"
val baseVersion = "1.0"

val versionPropsFile = file("build-version.properties")
var patchVersion = 0

if (versionPropsFile.canRead()) {
    val versionProps = Properties()

    versionProps.load(FileInputStream(versionPropsFile))

    patchVersion = versionProps["VERSION_CODE"].toString().toInt() + 1

    versionProps["VERSION_CODE"] = patchVersion.toString()
    versionProps.store(versionPropsFile.bufferedWriter(), null)
} else {
    throw GradleException("Cannot read version.properties!")
}

version = "$baseVersion.$patchVersion"

// Toolchains:
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

// Minecraft configuration:
loom {
    log4jConfigs.from(file("log4j2.xml"))
    launchConfigs {
        "client" {
            // If you don't want mixins, remove these lines
            property("mixin.debug", "true")
            property("asmhelper.verbose", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
            arg("--mixin", "mixins.tweakception.json")
        }
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        // If you don't want mixins, remove this lines
        mixinConfig("mixins.tweakception.json")
    }
    // If you don't want mixins, remove these lines
    mixin {
        defaultRefmapName.set("mixins.tweakception.refmap.json")
    }
}

sourceSets.main {
    java.srcDir("$buildDir/generated/java")
    output.setResourcesDir(file("$buildDir/classes/java/main"))
}

// Dependencies:

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    // If you don't want to log in with your real minecraft account, remove this line
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

val shadowImplementation: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    // If you don't want mixins, remove these lines
    shadowImplementation("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    annotationProcessor("org.spongepowered:mixin:0.8.4-SNAPSHOT")

    // If you don't want to log in with your real minecraft account, remove this line
    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.1.0")

    shadowImplementation("org.java-websocket:Java-WebSocket:1.5.3")
    shadowImplementation("org.apache.commons:commons-text:1.10.0")
    shadowImplementation("org.slf4j:slf4j-api:1.7.25")
    testImplementation("org.slf4j:slf4j-simple:1.7.25")
}

// Tasks:

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(Jar::class) {
    archiveBaseName.set("tweakception")
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"

        // If you don't want mixins, remove these lines
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["MixinConfigs"] = "mixins.tweakception.json"
    }
}

val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("dep")
    from(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile)
}

tasks.shadowJar {
    archiveClassifier.set("sources")
    configurations = listOf(shadowImplementation)
    doLast {
        configurations.forEach {
            println("Config: ${it.files}")
        }
    }

    // If you want to include other dependencies and shadow them, you can relocate them in here
    fun relocate(name: String) = relocate(name, "a7.tweakception.deps.$name")
}

tasks.assemble.get().dependsOn(tasks.remapJar)

tasks.processResources {
    exclude("**/*.psd")
    filesMatching("**/*.psd") {
        println("found : $relativeSourcePath")
    }
    filesMatching(listOf("mcmod.info", "Tweakception.java")) {
        expand(
            "mod_version" to project.version,
            "minecraft_version" to "1.8.9"
        )
    }
}