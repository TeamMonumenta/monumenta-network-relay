import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
	id("com.playmonumenta.gradle-config") version "1.+"
}

repositories {
	maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
	testImplementation(platform("org.junit:junit-bom:5.9.3"))
	testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
	testImplementation("org.yaml:snakeyaml:2.0")
<<<<<<< HEAD
    compileOnly("org.jetbrains:annotations:16.0.2")
    implementation("com.rabbitmq:amqp-client:5.20.0")
    compileOnly("dev.jorel:commandapi-bukkit-core:9.4.1")
    errorprone("com.google.errorprone:error_prone_core:2.29.1")
    errorprone("com.uber.nullaway:nullaway:0.10.18")
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")

    // temp from velocity
    implementation("org.slf4j:jul-to-slf4j:1.7.36")
=======
	compileOnly("org.jetbrains:annotations:16.0.2")
	implementation("com.rabbitmq:amqp-client:5.20.0")
	compileOnly("dev.jorel:commandapi-bukkit-core:9.4.1")
>>>>>>> 50b9154 (use common gradle plugin and stuff)
}

monumenta {
	name("MonumentaNetworkRelay")
	paper(
		"com.playmonumenta.networkrelay.NetworkRelay", BukkitPluginDescription.PluginLoadOrder.POSTWORLD, "1.19.4",
		depends = listOf("CommandAPI"),
		softDepends = listOf("PlaceholderAPI")
	)
	waterfall("com.playmonumenta.networkrelay.NetworkRelayBungee", "1.19")
}
