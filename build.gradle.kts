import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    java
    `maven-publish`
    id("com.palantir.git-version") version "0.12.2"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1" // Generates plugin.yml
    id("net.minecrell.plugin-yml.bungee") version "0.5.1" // Generates bungee.yml
    id("net.ltgt.errorprone") version "2.0.2"
    id("net.ltgt.nullaway") version "1.3.0"
    id("com.playmonumenta.deployment") version "1.0"
    checkstyle
    pmd
}

repositories {
    mavenLocal()
    mavenCentral()

    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }

    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://jitpack.io")
    }

    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }

    maven {
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    // NBT API, pulled in by CommandAPI
    maven {
        url = uri("https://repo.codemc.org/repository/maven-public/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
	testImplementation(platform("org.junit:junit-bom:5.9.3"))
	testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
	testImplementation("org.yaml:snakeyaml:2.0")
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:16.0.2")
    implementation("com.rabbitmq:amqp-client:5.20.0")
    compileOnly("dev.jorel:commandapi-bukkit-core:9.4.1")
    compileOnly("io.github.waterfallmc:waterfall-api:1.19-R0.1-SNAPSHOT")
    implementation("net.kyori:adventure-text-serializer-bungeecord:4.3.2") // TODO: remove when we transition to velocity
    compileOnly("me.clip:placeholderapi:2.10.4")
    errorprone("com.google.errorprone:error_prone_core:2.10.0")
    errorprone("com.uber.nullaway:nullaway:0.9.5")
}

group = "com.playmonumenta"
val gitVersion: groovy.lang.Closure<String> by extra
version = gitVersion()
description = "monumenta-network-relay"
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17
pmd {
    isConsoleOutput = true
    toolVersion = "6.41.0"
    ruleSets = listOf("$rootDir/pmd-ruleset.xml")
    setIgnoreFailures(true)
}

// Configure plugin.yml generation
bukkit {
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    main = "com.playmonumenta.networkrelay.NetworkRelay"
    apiVersion = "1.19"
    name = "MonumentaNetworkRelay"
    authors = listOf("Combustible")
    depend = listOf("CommandAPI")
    softDepend = listOf("PlaceholderAPI")
}

// Configure bungee.yml generation
bungee {
    name = "MonumentaNetworkRelay"
    main = "com.playmonumenta.networkrelay.NetworkRelayBungee"
    author = "Combustible"
}

publishing {
    publications.create<MavenPublication>("maven") {
        project.shadow.component(this)
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/TeamMonumenta/monumenta-network-relay")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xmaxwarns")
    options.compilerArgs.add("10000")
    options.compilerArgs.add("-Xlint:deprecation")

    options.errorprone {
        option("NullAway:AnnotatedPackages", "com.playmonumenta")

        allErrorsAsWarnings.set(true)

        /*** Disabled checks ***/
        // These we almost certainly don't want
        check("InlineMeSuggester", CheckSeverity.OFF) // We won't keep deprecated stuff around long enough for this to matter
        check("CatchAndPrintStackTrace", CheckSeverity.OFF) // This is the primary way a lot of exceptions are handled
        check("FutureReturnValueIgnored", CheckSeverity.OFF) // This one is dumb and doesn't let you check return values with .whenComplete()
        check("ImmutableEnumChecker", CheckSeverity.OFF) // Would like to turn this on but we'd have to annotate a bunch of base classes
        check("LockNotBeforeTry", CheckSeverity.OFF) // Very few locks in our code, those that we have are simple and refactoring like this would be ugly
        check("StaticAssignmentInConstructor", CheckSeverity.OFF) // We have tons of these on purpose
        check("StringSplitter", CheckSeverity.OFF) // We have a lot of string splits too which are fine for this use
        check("MutablePublicArray", CheckSeverity.OFF) // These are bad practice but annoying to refactor and low risk of actual bugs
    }
}

ssh.easySetup(tasks.named<ShadowJar>("shadowJar").get(), "MonumentaNetworkRelay")