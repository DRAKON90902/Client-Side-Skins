package com.clientsideskins.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.PlayerModelType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SkinImageProcessor {
	private SkinImageProcessor() {
	}

	public static NativeImage loadAndProcessImage(Path path, SkinCategory category) throws IOException {
		NativeImage image;
		try (InputStream stream = Files.newInputStream(path)) {
			image = NativeImage.read(stream);
		}
		if (category == SkinCategory.SKINS) {
			image = processSkinImage(image);
		}
		return image;
	}

	private static NativeImage processSkinImage(NativeImage image) {
		int width = image.getWidth();
		int height = image.getHeight();

		// Convert legacy 2:1 aspect ratio skins (e.g., 64x32) to square 1:1 format (e.g., 64x64)
		if (width == 64 && height == 32) {
			NativeImage converted = new NativeImage(64, 64, true);
			converted.copyFrom(image);
			image.close();
			image = converted;

			image.fillRect(0, 32, 64, 32, 0);
			image.copyRect(4, 16, 16, 32, 4, 4, true, false);
			image.copyRect(8, 16, 16, 32, 4, 4, true, false);
			image.copyRect(0, 20, 24, 32, 4, 12, true, false);
			image.copyRect(4, 20, 16, 32, 4, 12, true, false);
			image.copyRect(8, 20, 8, 32, 4, 12, true, false);
			image.copyRect(12, 20, 16, 32, 4, 12, true, false);
			image.copyRect(44, 16, -8, 32, 4, 4, true, false);
			image.copyRect(48, 16, -8, 32, 4, 4, true, false);
			image.copyRect(40, 20, 0, 32, 4, 12, true, false);
			image.copyRect(44, 20, -8, 32, 4, 12, true, false);
			image.copyRect(48, 20, -16, 32, 4, 12, true, false);
			image.copyRect(52, 20, -8, 32, 4, 12, true, false);

			setNoAlpha(image, 0, 0, 32, 16);
			doNotchTransparencyHack(image, 32, 0, 64, 32);
			setNoAlpha(image, 0, 16, 64, 32);
			setNoAlpha(image, 16, 48, 48, 64);
		}
		return image;
	}

	public static PlayerModelType detectModelType(NativeImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		if (width < 64 || height < 64 || width != height) {
			return PlayerModelType.WIDE;
		}
		int scale = width / 64;
		if (scale < 1) {
			return PlayerModelType.WIDE;
		}
		// Slim skins have transparent pixels in the outer 2 columns of the right arm front/back (x = 54..55, y = 20..31)
		int startX = 54 * scale;
		int endX = 56 * scale;
		int startY = 20 * scale;
		int endY = 32 * scale;
		for (int x = startX; x < endX; x++) {
			for (int y = startY; y < endY; y++) {
				int pixel = image.getPixel(x, y);
				if (ARGB.alpha(pixel) > 0) {
					return PlayerModelType.WIDE;
				}
			}
		}
		return PlayerModelType.SLIM;
	}

	private static void doNotchTransparencyHack(NativeImage image, int x0, int y0, int x1, int y1) {
		for (int x = x0; x < x1; x++) {
			for (int y = y0; y < y1; y++) {
				int pixel = image.getPixel(x, y);
				if (ARGB.alpha(pixel) < 128) {
					return;
				}
			}
		}
		for (int x = x0; x < x1; x++) {
			for (int y = y0; y < y1; y++) {
				image.setPixel(x, y, image.getPixel(x, y) & 0x00FFFFFF);
			}
		}
	}

	private static void setNoAlpha(NativeImage image, int x0, int y0, int x1, int y1) {
		for (int x = x0; x < x1; x++) {
			for (int y = y0; y < y1; y++) {
				image.setPixel(x, y, ARGB.opaque(image.getPixel(x, y)));
			}
		}
	}
}
