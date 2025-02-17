package dev.jorel.commandapi;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.help.HelpTopic;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Handles logic for registering commands after Paper build 65, where <a href="https://github.com/PaperMC/Paper/pull/8235">https://github.com/PaperMC/Paper/pull/8235</a>
 * changed a bunch of the behind-the-scenes logic.
 */
public class PaperCommandRegistration<Source> extends CommandRegistrationStrategy<Source> {
	// References to necessary methods
	private final Supplier<CommandDispatcher<Source>> getBrigadierDispatcher;
	private final Predicate<CommandNode<Source>> isBukkitCommand;

	// Store registered commands nodes for eventual reloads
	private final RootCommandNode<Source> registeredNodes = new RootCommandNode<>();

	private static final Constructor<?> pluginCommandNodeConstructor;
	private static final CommandDispatcher<?> staticDispatcher;

	private final CommandDispatcher<Source> dispatcher;

	static {
		CommandDispatcher<?> commandDispatcher;
		Constructor<?> commandNode;
		try {
			Object paperCommandsInstance = Class.forName("io.papermc.paper.command.brigadier.PaperCommands").getField("INSTANCE").get(null);
			Field dispatcherField = Class.forName("io.papermc.paper.command.brigadier.PaperCommands").getDeclaredField("dispatcher");
			dispatcherField.setAccessible(true);
			commandDispatcher = (CommandDispatcher<?>) dispatcherField.get(paperCommandsInstance);
		} catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
			// This doesn't happen
			commandDispatcher = null;
		}

		try {
			commandNode = Class.forName("io.papermc.paper.command.brigadier.PluginCommandNode").getDeclaredConstructor(String.class, PluginMeta.class, LiteralCommandNode.class, String.class);
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			try {
				// If this happens, plugin commands on Paper are not identified with the PluginCommandNode anymore
				commandNode = Class.forName("io.papermc.paper.command.brigadier.PluginCommandMeta").getDeclaredConstructor(PluginMeta.class, String.class, List.class);
			} catch (ClassNotFoundException | NoSuchMethodException e1) {
				commandNode = null;
			}
		}
		staticDispatcher = commandDispatcher;
		pluginCommandNodeConstructor = commandNode;
	}

	public PaperCommandRegistration(Supplier<CommandDispatcher<Source>> getBrigadierDispatcher, Predicate<CommandNode<Source>> isBukkitCommand) {
		this.getBrigadierDispatcher = getBrigadierDispatcher;
		this.isBukkitCommand = isBukkitCommand;
		this.dispatcher = (CommandDispatcher<Source>) staticDispatcher;
	}

	// Provide access to internal functions that may be useful to developers

	/**
	 * Checks if a Brigadier command node came from wrapping a Bukkit command
	 *
	 * @param node The CommandNode to check
	 * @return true if the CommandNode is being handled by Paper's BukkitCommandNode
	 */
	public boolean isBukkitCommand(CommandNode<Source> node) {
		return isBukkitCommand.test(node);
	}

	// Implement CommandRegistrationStrategy methods
	@Override
	public CommandDispatcher<Source> getBrigadierDispatcher() {
		return getBrigadierDispatcher.get();
	}

	@Override
	public void runTasksAfterServerStart() {
		// Nothing to do
	}

	@Override
	public void postCommandRegistration(RegisteredCommand registeredCommand, LiteralCommandNode<Source> resultantNode, List<LiteralCommandNode<Source>> aliasNodes) {
		// Nothing to do
	}

	@Override
	public LiteralCommandNode<Source> registerCommandNode(LiteralArgumentBuilder<Source> node, String namespace) {
		LiteralCommandNode<Source> commandNode = asPluginCommand(node.build());
		LiteralCommandNode<Source> namespacedCommandNode = asPluginCommand(CommandAPIHandler.getInstance().namespaceNode(commandNode, namespace));

		// Add to registered command nodes
		registeredNodes.addChild(commandNode);
		registeredNodes.addChild(namespacedCommandNode);

		// Register commands
		dispatcher.getRoot().addChild(commandNode);
		dispatcher.getRoot().addChild(namespacedCommandNode);

		return commandNode;
	}

	@Override
	public void unregister(String commandName, boolean unregisterNamespaces, boolean unregisterBukkit) {
		// Remove nodes from the  dispatcher
		removeBrigadierCommands(getBrigadierDispatcher.get().getRoot(), commandName, unregisterNamespaces,
			// If we are unregistering a Bukkit command, ONLY unregister BukkitCommandNodes
			// If we are unregistering a Vanilla command, DO NOT unregister BukkitCommandNodes
			c -> !unregisterBukkit ^ isBukkitCommand.test(c));

		// CommandAPI commands count as non-Bukkit
		if (!unregisterBukkit) {
			// Don't add nodes back after a reload
			removeBrigadierCommands(registeredNodes, commandName, unregisterNamespaces, c -> true);
		}

		// Update the dispatcher file
		CommandAPIHandler.getInstance().writeDispatcherToFile();
	}

	@Override
	public void preReloadDataPacks() {
		RootCommandNode<Source> root = getBrigadierDispatcher.get().getRoot();
		for (CommandNode<Source> commandNode : registeredNodes.getChildren()) {
			root.addChild(commandNode);
		}
	}

	@SuppressWarnings("unchecked")
	private LiteralCommandNode<Source> asPluginCommand(LiteralCommandNode<Source> commandNode) {
		try {
			if (pluginCommandNodeConstructor.getDeclaringClass().getSimpleName().equals("PluginCommandNode")) {
				return (LiteralCommandNode<Source>) pluginCommandNodeConstructor.newInstance(
					commandNode.getLiteral(),
					CommandAPIBukkit.getConfiguration().getPlugin().getPluginMeta(),
					commandNode,
					getDescription(commandNode.getLiteral())
				);
			} else {
				setPluginCommandMeta(commandNode);
				return commandNode;
			}
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void setPluginCommandMeta(LiteralCommandNode<Source> node) {
		try {
			Field metaField = node.getClass().getSuperclass().getDeclaredField("pluginCommandMeta");
			metaField.setAccessible(true);
			metaField.set(node, pluginCommandNodeConstructor.newInstance(
				CommandAPIBukkit.getConfiguration().getPlugin().getPluginMeta(),
				getDescription(node.getLiteral()),
				Collections.emptyList()
			));
		} catch (NoSuchFieldException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
			// This doesn't happen
		}
	}

	private String getDescription(String commandName) {
		String description = "";
		for (RegisteredCommand command : CommandAPI.getRegisteredCommands()) {
			String namespaceStripped = "";
			if (commandName.contains(":")) {
				namespaceStripped = commandName.split(":")[1];
			} else {
				namespaceStripped = commandName;
			}
			if (command.commandName().equals(namespaceStripped)) {
				Object helpTopic = command.helpTopic().orElse(null);
				if (helpTopic != null) {
					description = ((HelpTopic) helpTopic).getShortText();
				} else {
					description = command.fullDescription()
						.orElse(command.shortDescription()
							.orElse("A command by the " + CommandAPIBukkit.getConfiguration().getPlugin().getName() + " plugin."));
				}
				break;
			}
		}
		return description;
	}

}
