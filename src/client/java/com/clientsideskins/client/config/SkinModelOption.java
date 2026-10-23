package com.clientsideskins.client.config;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelType;

import java.util.Locale;

public enum SkinModelOption {
	AUTO("auto", "clientsideskins.model.auto"),
	WIDE("wide", "clientsideskins.model.wide"),
	SLIM("slim", "clientsideskins.model.slim");

	private final String id;
	private final String translationKey;

	SkinModelOption(String id, String translationKey) {
		this.id = id;
		this.translationKey = translationKey;
	}

	public String id() {
		return this.id;
	}

	public Component displayName() {
		return Component.translatable(this.translationKey);
	}

	public PlayerModelType resolve(PlayerModelType detectedOrFallback) {
		return switch (this) {
			case WIDE -> PlayerModelType.WIDE;
			case SLIM -> PlayerModelType.SLIM;
			case AUTO -> detectedOrFallback != null ? detectedOrFallback : PlayerModelType.WIDE;
		};
	}

	public static SkinModelOption fromString(String input) {
		if (input == null || input.isBlank()) {
			return AUTO;
		}
		return switch (input.trim().toLowerCase(Locale.ROOT)) {
			case "wide", "classic", "default", "steve" -> WIDE;
			case "slim", "alex", "thin" -> SLIM;
			case "auto", "detect" -> AUTO;
			default -> null;
		};
	}
}
