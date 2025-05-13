package dev.jorel.commandapi;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.nms.NMS;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class PaperImplementations {

	private final boolean isPaperPresent;
	private final boolean isFoliaPresent;
	private final boolean isPaperBrigAPI;
	private final NMS<?> nmsInstance;
	private final Class<? extends CommandSender> feedbackForwardingCommandSender;
	private final Class<? extends CommandSender> nullCommandSender;

	/**
	 * Constructs a PaperImplementations object
	 * 
	 * @param isPaperPresent Whether this is a Paper server or not
	 * @param isFoliaPresent Whether this is a Folia server or not
	 * @param nmsInstance    The instance of NMS
	 */
	@SuppressWarnings("unchecked")
	public PaperImplementations(boolean isPaperPresent, boolean isFoliaPresent, NMS<?> nmsInstance) {
		this.isPaperPresent = isPaperPresent;
		this.isFoliaPresent = isFoliaPresent;
		this.nmsInstance = nmsInstance;
		
		Class<? extends CommandSender> tempFeedbackForwardingCommandSender = null;
		try {
			tempFeedbackForwardingCommandSender = (Class<? extends CommandSender>) Class.forName("io.papermc.paper.commands.FeedbackForwardingSender");
		} catch (ClassNotFoundException e) {
			// uhh...
		}
		
		this.feedbackForwardingCommandSender = tempFeedbackForwardingCommandSender;

		Class<? extends CommandSender> tempNullCommandSender = null;
		try {
			tempNullCommandSender = (Class<? extends CommandSender>) Class.forName("io.papermc.paper.brigadier.NullCommandSender");
		} catch (ClassNotFoundException e) {
			// uhh...
		}

		this.nullCommandSender = tempNullCommandSender;

		boolean paperCommandSourceStackPresent;
		try {
			Class.forName("io.papermc.paper.command.brigadier.CommandSourceStack");
			paperCommandSourceStackPresent = true;
		} catch (ClassNotFoundException e) {
			paperCommandSourceStackPresent = false;
		}
		this.isPaperBrigAPI = paperCommandSourceStackPresent;
	}

	/**
	 * Hooks into Paper's {@link ServerResourcesReloadedEvent} to detect if
	 * {@code /minecraft:reload} is called, and registers a reload handler that
	 * automatically calls the CommandAPI's internal datapack reloading function
	 * 
	 * @param plugin the plugin that the CommandAPI is being used from
	 */
	public void registerReloadHandler(Plugin plugin) {
		if (isPaperPresent) {
			Bukkit.getServer().getPluginManager().registerEvents(new Listener() {
				@EventHandler
				public void onServerReloadResources(ServerResourcesReloadedEvent event) {
					// This event is called after Paper is done with everything command related
					// which means we can put commands back
					CommandAPIBukkit.get().getCommandRegistrationStrategy().preReloadDataPacks();

					// Normally, the reloadDataPacks() method is responsible for updating commands for
					// online players. If, however, datapacks aren't supposed to be reloaded upon /minecraft:reload
					// we have to do this manually here. This won't have any effect on Spigot and Paper version prior to
					// paper-1.20.6-65
					if (!CommandAPIBukkit.getConfiguration().shouldHookPaperReload()) {
						for (Player player : Bukkit.getOnlinePlayers()) {
							player.updateCommands();
						}
						return;
					}
					CommandAPI.logNormal("/minecraft:reload detected. Reloading CommandAPI commands!");
					nmsInstance.reloadDataPacks();
				}

			}, plugin);
			CommandAPI.logNormal("Hooked into Paper ServerResourcesReloadedEvent");
		} else {
			CommandAPI.logNormal("Did not hook into Paper ServerResourcesReloadedEvent");
		}
	}

	/**
	 * @return Bukkit's {@link CommandMap}
	 */
	public CommandMap getCommandMap() {
		if (isPaperPresent) {
			return Bukkit.getServer().getCommandMap();
		} else {
			return nmsInstance.getSimpleCommandMap();
		}
	}
	
	/**
	 * @return whether we're using paper or not
	 */
	public boolean isPaperPresent() {
		return this.isPaperPresent;
	}

	/**
	 * @return whether we're running a Paper server with the Paper Brigadier command API
	 */
	public boolean isPaperBrigAPI() {
		return this.isPaperBrigAPI;
	}
	
	/**
	 * @return whether we're using folia or not
	 */
	public boolean isFoliaPresent() {
		return this.isFoliaPresent;
	}
	
	/**
	 * @return a class reference pointing to {@code io.papermc.paper.commands.FeedbackForwardingSender}
	 */
	public Class<? extends CommandSender> getFeedbackForwardingCommandSender() {
		return this.feedbackForwardingCommandSender;
	}

	/**
	 * @return a class reference pointing to {@code io.papermc.paper.brigadier.NullCommandSender}
	 */
	public Class<? extends CommandSender> getNullCommandSender() {
		return this.nullCommandSender;
	}

	/**
	 * Builds a {@link WrapperCommandSyntaxException} from a message with colour codes like {@link ChatColor} or using the § symbol.
	 *
	 * @param message the error message to be displayed
	 * @return A {@link WrapperCommandSyntaxException} with the given message as error message
	 */
	public WrapperCommandSyntaxException getExceptionFromString(String message) {
		Message brigadierMessage;
		if (isPaperPresent) {
			// Java is doing something weird here.
			//  It's like when the class loads, it makes sure all the function calls use the right classes,
			//  but it doesn't check casts until the code runs. When it checks if classes are compatible,
			//  it doesn't need to load the class if they are exactly the same, but to check inheritance
			//  it does actually need to load the classes. But I don't know ¯\_(ツ)_/¯.
			// Whatever Java thinks it is doing, this code loads on Spigot, while
			//  more obvious things cause `ClassNotFound: ComponentLike`.
			brigadierMessage = BukkitTooltip.messageFromAdventureComponent(
				(ComponentLike) (Object) LegacyComponentSerializer.legacySection().deserialize(message)
			);
		} else {
			brigadierMessage = BukkitTooltip.messageFromBaseComponents(TextComponent.fromLegacyText(message));
		}
		return new WrapperCommandSyntaxException(new SimpleCommandExceptionType(brigadierMessage).create());
	}
}
