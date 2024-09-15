package dev.jorel.commandapi.config;

import org.jetbrains.annotations.ApiStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ClassEscapesDefinedScope")
@ApiStatus.Internal
public abstract class DefaultedConfig {

	protected final Map<String, CommentedConfigOption<?>> allOptions = new LinkedHashMap<>();
	protected final Map<String, CommentedSection> allSections = new LinkedHashMap<>();

	public static final CommentedConfigOption<Boolean> VERBOSE_OUTPUTS = new CommentedConfigOption<>(
		List.of(
			"Verbose outputs (default: false)",
			"If \"true\", outputs command registration and unregistration logs in the console"
		), false
	);

	public static final CommentedConfigOption<Boolean> SILENT_LOGS = new CommentedConfigOption<>(
		List.of(
			"Silent logs (default: false)",
			"If \"true\", turns off all logging from the CommandAPI, except for errors."
		), false
	);

	public static final CommentedConfigOption<String> MISSING_EXECUTOR_IMPLEMENTATION = new CommentedConfigOption<>(
		List.of(
			"Missing executor implementation (default: \"This command has no implementations for %s\")",
			"The message to display to senders when a command has no executor. Available",
			"parameters are:",
			"  %s - the executor class (lowercase)",
			"  %S - the executor class (normal case)"
		), "This command has no implementations for %s"
	);

	public static final CommentedConfigOption<Boolean> CREATE_DISPATCHER_JSON = new CommentedConfigOption<>(
		List.of(
			"Create dispatcher JSON (default: false)",
			"If \"true\", the CommandAPI creates a command_registration.json file showing the",
			"mapping of registered commands. This is designed to be used by developers -",
			"setting this to \"false\" will improve command registration performance."
		), false
	);

	public static final CommentedSection SECTION_MESSAGE = new CommentedSection(
		List.of(
			"Messages",
			"Controls messages that the CommandAPI displays to players"
		)
	);

	public final Map<String, CommentedConfigOption<?>> getAllOptions() {
		return allOptions;
	}

	public final Map<String, CommentedSection> getAllSections() {
		return allSections;
	}

}
