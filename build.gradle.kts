plugins {
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
    id("org.spongepowered.mixin") version "0.7.+"
    java
}

group = "dev.streamsreflowing"
version = "2.15.0-tfc.1"
base {
    archivesName.set("streamsreflowing")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

minecraft {
    mappings("official", "1.20.1")
    runs {
        create("client") {
            workingDirectory = project.file("run").absolutePath
            property("forge.logging.console.level", "debug")
            mods {
                create("streamsreflowing") {
                    source(sourceSets.main.get())
                }
            }
        }
        create("server") {
            workingDirectory = project.file("run-server").absolutePath
            args("--nogui")
            mods {
                create("streamsreflowing") {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

mixin {
    add(sourceSets.main.get(), "streamsreflowing.refmap.json")
    config("streamsreflowing.mixins.json")
}

repositories {
    mavenCentral()
    maven(url = "https://maven.minecraftforge.net/")
    maven(url = "https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    minecraft("net.minecraftforge:forge:1.20.1-47.1.3")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    compileOnly("io.github.llamalad7:mixinextras-common:0.4.1")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1")
    implementation(jarJar("io.github.llamalad7:mixinextras-forge:0.4.1")) {
        jarJar.ranged(this, "[0.4.1,)")
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("streamsreflowing.testmode", "true")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
}

tasks.jar {
    manifest {
        attributes["MixinConfigs"] = "streamsreflowing.mixins.json"
    }
}
