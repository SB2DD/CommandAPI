package dev.jorel.commandapi.arguments;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPIHandler;
import dev.jorel.commandapi.wrappers.FloatRange;

/**
 * An argument that represents a range of float values
 */
public class FloatRangeArgument extends Argument implements ISafeOverrideableSuggestions<FloatRange> {

	/**
	 * A FloatRange argument that represents
	 */
	public FloatRangeArgument() {
		super(CommandAPIHandler.getNMS()._ArgumentFloatRange());
	}

	@Override
	public Class<?> getPrimitiveType() {
		return FloatRange.class;
	}

	@Override
	public CommandAPIArgumentType getArgumentType() {
		return CommandAPIArgumentType.FLOAT_RANGE;
	}
	
	public Argument safeOverrideSuggestions(FloatRange... suggestions) {
		return super.overrideSuggestions(sMap0(FloatRange::toString, suggestions));
	}

	public Argument safeOverrideSuggestions(Function<CommandSender, FloatRange[]> suggestions) {
		return super.overrideSuggestions(sMap1(FloatRange::toString, suggestions));
	}

	public Argument safeOverrideSuggestions(BiFunction<CommandSender, Object[], FloatRange[]> suggestions) {
		return super.overrideSuggestions(sMap2(FloatRange::toString, suggestions));
	}
}