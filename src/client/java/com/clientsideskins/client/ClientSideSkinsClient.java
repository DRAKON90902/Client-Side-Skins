package com.clientsideskins.client;

import com.clientsideskins.client.command.ClientSideSkinsCommands;
import com.clientsideskins.client.skin.SkinFolderManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.Screen;

public class ClientSideSkinsClient implements ClientModInitializer {
	private static volatile Screen pendingScreen = null;

	@Override
	public void onInitializeClient() {
		// Create ClientSideSkins folder (and skins/, capes/, elytras/ subfolders) on first bootup and scan PNGs
		SkinFolderManager.getInstance().initialize();

		// Register level 0 client commands (/clientsideskins and /css)
		ClientSideSkinsCommands.register();

		// Open any screen queued by a client command on the next tick after ChatScreen closes
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Screen screenToOpen = pendingScreen;
			if (screenToOpen != null) {
				pendingScreen = null;
				client.gui.setScreen(screenToOpen);
			}
		});
	}

	public static void scheduleOpenScreen(Screen screen) {
		pendingScreen = screen;
	}
}
