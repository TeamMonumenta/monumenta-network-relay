import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
	id("com.playmonumenta.gradle-config") version "1.+"
}

repositories {
	mavenCentral()
	maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
	testImplementation(platform(libs.junit.bom))
	testImplementation(libs.junit.jupiter)
	testImplementation(libs.snakeyaml)
	implementation(libs.rabbitmq)
	compileOnly(libs.annotations)
	compileOnly(libs.commandapi)
	compileOnly(libs.placerholderapi)
	compileOnly(libs.velocity)
	annotationProcessor(libs.velocity)
	implementation(libs.slf4j)
}

monumenta {
	name("MonumentaNetworkRelay")
	paper(
		"com.playmonumenta.networkrelay.NetworkRelay", BukkitPluginDescription.PluginLoadOrder.POSTWORLD, "1.18",
		depends = listOf("CommandAPI"),
		softDepends = listOf("PlaceholderAPI")
	)
	waterfall("com.playmonumenta.networkrelay.NetworkRelayBungee", "1.18")
}
