plugins {
	java
}

java {
	toolchain {
		// Make the compiler against Java 17
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}

version = "0.0.1-SNAPSHOT"

repositories {
	// Use Maven Central for resolving dependencies.
	mavenCentral()
	// This adds the Velocity Maven repository to the build
	maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
	// This adds the Velocity API artifact to the build
	implementation("com.velocitypowered:velocity-api:3.1.1")

	// The CommandAPI dependency used for Velocity
	implementation("dev.jorel:commandapi-velocity-core:10.0.1")
}