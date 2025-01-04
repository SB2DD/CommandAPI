package dev.jorel.commandapi;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.papermc.paper.plugin.configuration.PluginMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
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
		// If we're using this class, we're operating on a Paper server with Paper's Brigadier API
		// The class io.papermc.paper.command.brigadier.PluginCommandNode exists
		Constructor<?> commandNode;
		try {
			commandNode = Class.forName("io.papermc.paper.command.brigadier.PluginCommandNode").getDeclaredConstructor(String.class, PluginMeta.class, LiteralCommandNode.class, String.class);

			Class<?> paperCommands = Class.forName("io.papermc.paper.command.brigadier.PaperCommands");
			Object paperCommandsInstance = paperCommands.getField("INSTANCE").get(null);
			Field dispatcherField = Class.forName("io.papermc.paper.command.brigadier.PaperCommands").getDeclaredField("dispatcher");
			dispatcherField.setAccessible(true);
			commandDispatcher = (CommandDispatcher<?>) dispatcherField.get(paperCommandsInstance);
		} catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
			// This doesn't happen
			commandNode = null;
			commandDispatcher = null;
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
			LiteralCommandNode<Source> node = (LiteralCommandNode<Source>) pluginCommandNodeConstructor.newInstance(
				commandNode.getLiteral(),
				CommandAPIBukkit.getConfiguration().getPlugin().getPluginMeta(),
				commandNode,
				""
			);
			return node;
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

}
