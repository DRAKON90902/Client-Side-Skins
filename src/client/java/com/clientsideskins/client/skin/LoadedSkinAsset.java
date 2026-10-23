package com.clientsideskins.client.skin;

import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;

import java.nio.file.Path;

public record LoadedSkinAsset(
	String fileName,
	Path filePath,
	SkinCategory category,
	ClientAsset.Texture texture,
	int width,
	int height,
	PlayerModelType detectedModel
) {
	public Identifier textureId() {
		return this.texture.texturePath();
	}
}
