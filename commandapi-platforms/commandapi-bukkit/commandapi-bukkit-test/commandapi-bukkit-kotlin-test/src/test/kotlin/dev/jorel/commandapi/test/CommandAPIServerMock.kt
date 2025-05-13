package dev.jorel.commandapi.test

import be.seeseemelk.mockbukkit.ServerMock
import be.seeseemelk.mockbukkit.entity.PlayerMock
import com.mojang.brigadier.context.StringRange
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import dev.jorel.commandapi.Brigadier
import org.bukkit.Keyed
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.generator.structure.StructureType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.map.MapCursor
import org.bukkit.scoreboard.Criteria
import org.junit.jupiter.api.Assertions
import java.util.concurrent.ExecutionException

class CommandAPIServerMock : ServerMock() {
	@Throws(CommandSyntaxException::class)
	fun dispatchThrowableCommand(sender: CommandSender, commandLine: String): Boolean {
		val commands = commandLine.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		val commandLabel = commands[0]
		val command = commandMap.getCommand(commandLabel)
		return if (command != null) {
			super.dispatchCommand(sender, commandLine)
		} else {
			val dispatcher = Brigadier.getCommandDispatcher()
			val css = Brigadier.getBrigadierSourceFromCommandSender(sender)
			dispatcher.execute(commandLine, css) != 0
		}
	}

	fun dispatchBrigadierCommand(sender: CommandSender, commandLine: String): Int {
		return try {
			val dispatcher = Brigadier.getCommandDispatcher()
			val css = Brigadier.getBrigadierSourceFromCommandSender(sender)
			dispatcher.execute(commandLine, css)
		} catch (e: CommandSyntaxException) {
			Assertions.fail<Any>("Command '/$commandLine' failed. If you expected this to fail, use dispatchThrowableCommand() instead.", e)
			0
		}
	}

	override fun dispatchCommand(sender: CommandSender, commandLine: String): Boolean {
		return try {
			dispatchThrowableCommand(sender, commandLine)
		} catch (e: CommandSyntaxException) {
			Assertions.fail<Any>("Command '/$commandLine' failed. If you expected this to fail, use dispatchThrowableCommand() instead.", e)
			false
		}
	}

	/**
	 * Adds a recipe to the crafting manager.
	 *
	 * @param recipe the recipe to add
	 * @param resendRecipes true to update the client with the full set of recipes
	 * @return true if the recipe was added, false if it wasn't for some reason
	 */
	override fun addRecipe(recipe: Recipe?, resendRecipes: Boolean): Boolean {
		TODO("Not yet implemented")
	}

	/**
	 * Remove a recipe from the server.
	 *
	 *
	 * **Note that removing a recipe may cause permanent loss of data
	 * associated with that recipe (eg whether it has been discovered by
	 * players).**
	 *
	 * @param key NamespacedKey of recipe to remove.
	 * @param resendRecipes true to update all clients on the new recipe list.
	 * Will only update if a recipe was actually removed
	 * @return True if recipe was removed
	 */
	override fun removeRecipe(key: NamespacedKey, resendRecipes: Boolean): Boolean {
		TODO("Not yet implemented")
	}

	fun getSuggestions(sender: CommandSender, commandLine: String?): List<String> {
		val dispatcher = Brigadier.getCommandDispatcher()
		val css = Brigadier.getBrigadierSourceFromCommandSender(sender)
		val parseResults = dispatcher.parse(commandLine, css)
		var suggestions: Suggestions? = null
		suggestions = try {
			dispatcher.getCompletionSuggestions(parseResults).get() as Suggestions
		} catch (e: InterruptedException) {
			e.printStackTrace()
			Suggestions(StringRange.at(0), ArrayList()) // Empty suggestions
		} catch (e: ExecutionException) {
			e.printStackTrace()
			Suggestions(StringRange.at(0), ArrayList())
		}
		val suggestionsAsStrings: MutableList<String> = ArrayList()
		for (suggestion in suggestions!!.list) {
			suggestionsAsStrings.add(suggestion.text)
		}
		return suggestionsAsStrings
	}

	/**
	 * Creates a new Bukkit [Player]. Unlike [PlayerMock], this uses Mockito to mock the CraftPlayer class,
	 * which allows the returned object to pass through VanillaCommandWrapper#getListener without error.
	 *
	 * @return A new [Player].
	 */
	fun setupMockedCraftPlayer(): Player {
		return setupMockedCraftPlayer("defaultName")
	}

	/**
	 * Creates a new Bukkit [Player]. Unlike [PlayerMock], this uses Mockito to mock the CraftPlayer class,
	 * which allows the returned object to pass through VanillaCommandWrapper#getListener without error.
	 *
	 * @param name The name for the player
	 * @return A new [Player].
	 */
	fun setupMockedCraftPlayer(name: String?): Player {
		return MockPlatform.getInstance<Any>().setupMockedCraftPlayer(name)
	}

	override fun isTickingWorlds(): Boolean {
		TODO("Not yet implemented")
	}

	/**
	 * Create a new explorer map targeting the closest nearby structure of a
	 * given [org.bukkit.generator.structure.StructureType].
	 *
	 * @param world the world the map will belong to
	 * @param location the origin location to find the nearest structure
	 * @param structureType the type of structure to find
	 * @param mapIcon the map icon to use on the map
	 * @param radius radius to search, see World#locateNearestStructure for more
	 * information
	 * @param findUnexplored whether to find unexplored structures
	 * @return the newly created item stack or null if it can't find a location
	 *
	 * @see World.locateNearestStructure
	 */
	override fun createExplorerMap(world: World, location: Location, structureType: StructureType, mapIcon: MapCursor.Type, radius: Int, findUnexplored: Boolean): ItemStack? {
		TODO("Not yet implemented")
	}

	/**
	 * Updates all advancement, tag, and recipe data to all connected clients.
	 * Useful for updating clients to new advancements/recipes/tags.
	 * @see .updateRecipes
	 */
	override fun updateResources() {
		TODO("Not yet implemented")
	}

	/**
	 * Updates recipe data and the recipe book to each player. Useful for
	 * updating clients to new recipes.
	 * @see .updateResources
	 */
	override fun updateRecipes() {
		TODO("Not yet implemented")
	}

	override fun shouldSendChatPreviews(): Boolean {
		return true
	}

	override fun isEnforcingSecureProfiles(): Boolean {
		TODO("Not yet implemented")
	}

	override fun getMaxChainedNeighborUpdates(): Int {
		TODO("Not yet implemented")
	}

	override fun getScoreboardCriteria(name: String): Criteria {
		TODO("Not yet implemented")
	}

	override fun <T : Keyed?> getRegistry(tClass: Class<T>): Registry<T>? {
		TODO("Not yet implemented")
	}

}