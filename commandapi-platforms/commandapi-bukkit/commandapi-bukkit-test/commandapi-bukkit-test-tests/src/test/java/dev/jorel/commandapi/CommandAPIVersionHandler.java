package dev.jorel.commandapi;

import dev.jorel.commandapi.nms.NMS_1_20_R1;
import dev.jorel.commandapi.nms.NMS_1_20_R2;
import dev.jorel.commandapi.nms.NMS_1_20_R3;
import dev.jorel.commandapi.nms.NMS_1_20_R4;
import dev.jorel.commandapi.test.MockNMS;

/**
 * This file handles loading the correct platform implementation. The CommandAPIVersionHandler
 * file within the commandapi-core module is NOT used at run time. Instead, the platform modules
 * replace this class with their own version that handles loads the correct class for their version
 */
public abstract class CommandAPIVersionHandler {
	
	public static final String profileId = getProfileId();
	public static final boolean IS_MOJANG_MAPPED = isMojangMapped();
	
	private static String getProfileId() {
		String profileIdProperty = System.getProperty("profileId");
		if(profileIdProperty != null) {
			if ( profileIdProperty.endsWith("_Mojang")) {
				return profileIdProperty.substring(0, profileIdProperty.length() - "_Mojang".length());
			} else {
				return profileIdProperty;
			}
		} else {
			return null;
		}
	}
	
	private static boolean isMojangMapped() {
		String profileIdProperty = System.getProperty("profileId");
		if(profileIdProperty != null) {
			return profileIdProperty.endsWith("_Mojang");
		} else {
			return false;
		}
	}
	
	static LoadContext getPlatform() {
		if(profileId == null) {
			System.out.println("Using default version 1.20");
			return new LoadContext(new MockNMS(new NMS_1_20_R1()));
		} else {
			return new LoadContext(new MockNMS(switch(profileId) {
				case "Minecraft_1_20_5" -> new NMS_1_20_R4();
				case "Minecraft_1_20_3" -> new NMS_1_20_R3();
				case "Minecraft_1_20_2" -> new NMS_1_20_R2();
				case "Minecraft_1_20" -> new NMS_1_20_R1();
				default -> throw new IllegalArgumentException("Unexpected value: " + System.getProperty("profileId"));
			}));
		}
	}
	
	public static MCVersion getVersion() {
		if(profileId == null) {
			System.out.println("Using default version 1.20");
			return MCVersion.V1_20;
		} else {
			return switch(profileId) {
				case "Minecraft_1_20_5" -> MCVersion.V1_20_5;
				case "Minecraft_1_20_3" -> MCVersion.V1_20_3;
				case "Minecraft_1_20_2" -> MCVersion.V1_20_2;
				case "Minecraft_1_20" -> MCVersion.V1_20;
				default -> throw new IllegalArgumentException("Unexpected value: " + System.getProperty("profileId"));
			};
		}
	}
}
