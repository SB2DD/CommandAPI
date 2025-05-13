# Exit script if any tests fails so they can be investigated
set -e

runTest() {
	echo "Running tests for $1"
	mvn clean verify -Dmaven.javadoc.skip=true -P Platform.Bukkit -pl :commandapi-bukkit-test-tests -P $1
}

# Test all versions
# 1.20.5 & 1.20.6
runTest Minecraft_1_20_5
runTest Minecraft_1_20_5_Mojang

# 1.20.3 & 1.20.4
runTest Minecraft_1_20_3

# 1.20.2
runTest Minecraft_1_20_2

# 1.20 & 1.20.1
runTest Minecraft_1_20
runTest Minecraft_1_20_Mojang