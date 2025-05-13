# Automated tests (shaded)

A simple example showcasing testing CommandAPI commands with [MockBukkit](https://github.com/MockBukkit/MockBukkit). For more information on the utility provided by the CommandAPI's testing framework, check the documentation [here](https://docs.commandapi.dev/test/intro).

Key points:

- The MockBukkit and `commandapi-bukkit-test-toolkit` dependencies are listed with the `test` scope before the normal dependencies for `commandapi-bukkit-shade` and `spigot-api`. This ensures that when running tests, certain classes that are compatible with the testing environment override the regular classes. There is also a dependency for the [JUnit](https://junit.org/junit5/) API, which helps when writing the tests.

```xml
<dependencies>
	<dependency>
		<groupId>com.github.seeseemelk</groupId>
		<artifactId>MockBukkit-v1.21</artifactId>
		<version>3.128.0</version>
		<scope>test</scope>
	</dependency>

	<dependency>
		<groupId>dev.jorel</groupId>
		<artifactId>commandapi-bukkit-test-toolkit</artifactId>
		<version>10.0.1</version>
		<scope>test</scope>
	</dependency>

	<dependency>
		<groupId>dev.jorel</groupId>
		<artifactId>commandapi-bukkit-shade</artifactId>
		<version>10.0.1</version>
		<scope>compile</scope>
	</dependency>

	<!-- Can also be paper-api -->
	<dependency>
		<groupId>org.spigotmc</groupId>
		<artifactId>spigot-api</artifactId>
		<version>1.21.1-R0.1-SNAPSHOT</version>
		<scope>provided</scope>
	</dependency>

	<dependency>
		<groupId>org.junit.jupiter</groupId>
		<artifactId>junit-jupiter-engine</artifactId>
		<version>5.8.2</version>
		<scope>test</scope>
	</dependency>
</dependencies>
```

- While calling `CommandAPI.onDisable()` is usually not required when shading the CommandAPI, it is necessary to ensure that everything resets properly between tests. You still need to call `CommandAPI.onLoad()` and `CommandAPI.onEnable()` as usual. Otherwise, you should not need to make any changes to your loading or command registration logic.

```java
public class Main extends JavaPlugin {
	@Override
	public void onLoad() {
		CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
	}

	@Override
	public void onEnable() {
		CommandAPI.onEnable();

		// Register command as usual
		new CommandAPICommand("ping").executes((sender, args) -> {
			sender.sendMessage("pong!");
		}).register();
	}

	@Override
	public void onDisable() {
		CommandAPI.onDisable();
	}
}
```

- Before running a test, you need to set up MockBukkit, which is described in their documentation [here](https://mockbukkit.readthedocs.io/en/latest/first_tests.html#creating-the-test-class). Since you are shading the CommandAPI, loading your plugin will load the CommandAPI as well. After each test, call `MockBukkit.unmock()` to reset everything for the next test.

```java
@BeforeEach
public void setUp() {
	// Set up MockBukkit server
	server = MockBukkit.mock();

	// Load our plugin
	MockBukkit.load(Main.class);
}

@AfterEach
public void tearDown() {
	// Reset for a clean slate next test
	MockBukkit.unmock();
}
```

- The `CommandAPITestUtilities` class provides static methods that allow you to interact with CommandAPI commands. You can verify that a command runs successfully, fails with a certain error message, runs with certain arguments, and suggests strings or tooltips.

