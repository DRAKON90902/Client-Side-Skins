package com.clientsideskins.client.skin;

import net.minecraft.network.chat.Component;

public enum SkinCategory {
	SKINS("skins", "clientsideskins.tab.skins"),
	CAPES("capes", "clientsideskins.tab.capes"),
	ELYTRAS("elytras", "clientsideskins.tab.elytras");

	private final String folderName;
	private final String tabTranslationKey;

	SkinCategory(String folderName, String tabTranslationKey) {
		this.folderName = folderName;
		this.tabTranslationKey = tabTranslationKey;
	}

	public String folderName() {
		return this.folderName;
	}

	public Component tabTitle(int count) {
		return Component.translatable(this.tabTranslationKey, count);
	}
}
