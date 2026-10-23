package com.clientsideskins.client.command;

import com.clientsideskins.client.ClientSideSkinsClient;
import com.clientsideskins.client.config.ClientSideSkinsConfig;
import com.clientsideskins.client.config.SkinModelOption;
import com.clientsideskins.client.gui.ClientSideSkinsScreen;
import com.clientsideskins.client.skin.SkinCategory;
import com.clientsideskins.client.skin.SkinFolderManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ClientSideSkinsCommands {
	private static final List<Consumer<LiteralArgumentBuilder<FabricClientCommandSource>>> EXTENSIONS = new ArrayList<>();

	private static final SuggestionProvider<FabricClientCommandSource> SKIN_FILE_SUGGESTIONS = (context, builder) -> {
		SkinFolderManager.getInstance().scanFolders();
		return SharedSuggestionProvider.suggest(
			SkinFolderManager.getInstance().getAvailableFileNames(SkinCategory.SKINS),
			builder
		);
	};

	private static final SuggestionProvider<FabricClientCommandSource> CAPE_FILE_SUGGESTIONS = (context, builder) -> {
		SkinFolderManager.getInstance().scanFolders();
		return SharedSuggestionProvider.suggest(
			SkinFolderManager.getInstance().getAvailableFileNames(SkinCategory.CAPES),
			builder
		);
	};

	private static final SuggestionProvider<FabricClientCommandSource> ELYTRA_FILE_SUGGESTIONS = (context, builder) -> {
		SkinFolderManager.getInstance().scanFolders();
		return SharedSuggestionProvider.suggest(
			SkinFolderManager.getInstance().getAvailableFileNames(SkinCategory.ELYTRAS),
			builder
		);
	};

	private static final SuggestionProvider<FabricClientCommandSource> MODEL_SUGGESTIONS = (context, builder) ->
		SharedSuggestionProvider.suggest(List.of("auto", "wide", "slim"), builder);

	private ClientSideSkinsCommands() {
	}

	/**
	 * Allows future expansions of ClientSideSkins to attach new subcommands to /clientsideskins and /css.
	 */
	public static void registerExtension(Consumer<LiteralArgumentBuilder<FabricClientCommandSource>> extension) {
		if (extension != null) {
			EXTENSIONS.add(extension);
		}
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			registerRootCommand(dispatcher, "clientsideskins");
			registerRootCommand(dispatcher, "css");
		});
	}

	private static void registerRootCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, String rootLiteral) {
		LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommands.literal(rootLiteral)
			// Level 0 client command: usable by any player in singleplayer, LAN, and public servers
			.requires(source -> true)
			.executes(ctx -> openMenu(ctx.getSource()))
			.then(ClientCommands.literal("menu").executes(ctx -> openMenu(ctx.getSource())))
			.then(ClientCommands.literal("gui").executes(ctx -> openMenu(ctx.getSource())))
			.then(buildSkinNode())
			.then(buildCapeNode())
			.then(buildElytraNode())
			.then(buildModelNode())
			.then(buildToggleNode())
			.then(ClientCommands.literal("reload").executes(ctx -> reloadFolders(ctx.getSource())))
			.then(buildListNode())
			.then(ClientCommands.literal("status").executes(ctx -> showStatus(ctx.getSource())))
			.then(ClientCommands.literal("info").executes(ctx -> showStatus(ctx.getSource())))
			.then(buildFolderNode());

		for (Consumer<LiteralArgumentBuilder<FabricClientCommandSource>> extension : EXTENSIONS) {
			extension.accept(root);
		}

		dispatcher.register(root);
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> buildSkinNode() {
		return ClientCommands.literal("skin")
			.then(ClientCommands.literal("set")
				.then(ClientCommands.argument("file", StringArgumentType.string())
					.suggests(SKIN_FILE_SUGGESTIONS)
					.executes(ctx -> setCategoryFile(
						ctx.getSource(),
						SkinCategory.SKINS,
						StringArgumentType.getString(ctx, "file"),
						null
					))
					.then(ClientCommands.argument("model", StringArgumentType.word())
						.suggests(MODEL_SUGGESTIONS)
						.executes(ctx -> setCategoryFile(
							ctx.getSource(),
							SkinCategory.SKINS,
							StringArgumentType.getString(ctx, "file"),
							StringArgumentType.getString(ctx, "model")
						))
					)
				)
			)
			.then(ClientCommands.literal("clear").executes(ctx -> clearCategoryFile(ctx.getSource(), SkinCategory.SKINS)))
			.then(ClientCommands.literal("reset").executes(ctx -> clearCategoryFile(ctx.getSource(), SkinCategory.SKINS)));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> buildCapeNode() {
		return ClientCommands.literal("cape")
			.then(ClientCommands.literal("set")
				.then(ClientCommands.argument("file", StringArgumentType.greedyString())
					.suggests(CAPE_FILE_SUGGESTIONS)
					.executes(ctx -> setCategoryFile(
						ctx.getSource(),
						SkinCategory.CAPES,
						StringArgumentType.getString(ctx, "file"),
						null
					))
				)
			)
			.then(ClientCommands.literal("clear").executes(ctx -> clearCategoryFile(ctx.getSource(), SkinCategory.CAPES)))
			.then(ClientCommands.literal("reset").executes(ctx -> clearCategoryFile(ctx.getSource(), SkinCategory.CAPES)));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> buildElytraNode() {
		return ClientCommands.literal("elytra")
			.then(ClientCommands.literal("set")
				.then(ClientCommands.argument("file", StringArgumentType.greedyString())
					.suggests(ELYTRA_FILE_SUGGESTIONS)
					.executes(ctx -> setCategoryFile(
						ctx.getSource(),
						SkinCategory.ELYTRAS,
						StringArgumentType.getString(ctx, "file"),
						null
					))
				)
			)
			.then(ClientCommands.literal("clear").executes(ctx -> clearCategoryFile(ctx.getSource(), SkinCategory.ELYTRAS)))
			.then(ClientCommands.literal("reset").executes(ctx -> clearCategoryFile(ctx.getSource(), SkinCategory.ELYTRAS)));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> buildModelNode() {
		return ClientCommands.literal("model")
			.then(ClientCommands.argument("type", StringArgumentType.word())
				.suggests(MODEL_SUGGESTIONS)
				.executes(ctx -> setModelOption(ctx.getSource(), StringArgumentType.getString(ctx, "type")))
			);
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> buildToggleNode() {
		return ClientCommands.literal("toggle")
			.executes(ctx -> toggleEnabled(ctx.getSource(), null))
			.then(ClientCommands.literal("on").executes(ctx -> toggleEnabled(ctx.getSource(), Boolean.TRUE)))
			.then(ClientCommands.literal("off").executes(ctx -> toggleEnabled(ctx.getSource(), Boolean.FALSE)));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> buildListNode() {
		return ClientCommands.literal("list")
			.executes(ctx -> listFiles(ctx.getSource(), null))
			.then(ClientCommands.literal("skins").executes(ctx -> listFiles(ctx.getSource(), SkinCategory.SKINS)))
			.then(ClientCommands.literal("capes").executes(ctx -> listFiles(ctx.getSource(), SkinCategory.CAPES)))
			.then(ClientCommands.literal("elytras").executes(ctx -> listFiles(ctx.getSource(), SkinCategory.ELYTRAS)));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> buildFolderNode() {
		return ClientCommands.literal("folder")
			.executes(ctx -> openFolder(ctx.getSource(), null))
			.then(ClientCommands.literal("skins").executes(ctx -> openFolder(ctx.getSource(), SkinCategory.SKINS)))
			.then(ClientCommands.literal("capes").executes(ctx -> openFolder(ctx.getSource(), SkinCategory.CAPES)))
			.then(ClientCommands.literal("elytras").executes(ctx -> openFolder(ctx.getSource(), SkinCategory.ELYTRAS)));
	}

	private static int openMenu(FabricClientCommandSource source) {
		ClientSideSkinsClient.scheduleOpenScreen(new ClientSideSkinsScreen(null));
		return Command.SINGLE_SUCCESS;
	}

	private static int setCategoryFile(
		FabricClientCommandSource source,
		SkinCategory category,
		String requestedFile,
		String optionalModel
	) {
		SkinFolderManager manager = SkinFolderManager.getInstance();
		manager.scanFolders();
		String canonical = manager.findCanonicalFileName(category, requestedFile);
		if (canonical == null) {
			source.sendError(Component.literal("[ClientSideSkins] Could not find PNG file '" + requestedFile + "' in " + category.folderName() + "/"));
			return 0;
		}

		if (optionalModel != null && !optionalModel.isBlank()) {
			SkinModelOption parsedModel = SkinModelOption.fromString(optionalModel);
			if (parsedModel == null) {
				source.sendError(Component.literal("[ClientSideSkins] Invalid arm model '" + optionalModel + "'. Use auto, wide, or slim."));
				return 0;
			}
			manager.getConfig().setModelOption(parsedModel);
		}

		manager.setSelectedFile(category, canonical);
		manager.getOrLoadAsset(category, canonical);

		source.sendFeedback(
			Component.literal("[ClientSideSkins] ").withStyle(ChatFormatting.GOLD)
				.append(Component.literal("Equipped " + category.folderName().substring(0, category.folderName().length() - 1) + ": ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(canonical).withStyle(ChatFormatting.GREEN))
		);
		return Command.SINGLE_SUCCESS;
	}

	private static int clearCategoryFile(FabricClientCommandSource source, SkinCategory category) {
		SkinFolderManager.getInstance().setSelectedFile(category, "");
		String singular = category.folderName().substring(0, category.folderName().length() - 1);
		source.sendFeedback(
			Component.literal("[ClientSideSkins] ").withStyle(ChatFormatting.GOLD)
				.append(Component.literal("Reset " + singular + " to default Mojang texture.").withStyle(ChatFormatting.YELLOW))
		);
		return Command.SINGLE_SUCCESS;
	}

	private static int setModelOption(FabricClientCommandSource source, String modelStr) {
		SkinModelOption option = SkinModelOption.fromString(modelStr);
		if (option == null) {
			source.sendError(Component.literal("[ClientSideSkins] Invalid model '" + modelStr + "'. Choose: auto, wide, or slim."));
			return 0;
		}
		SkinFolderManager manager = SkinFolderManager.getInstance();
		manager.getConfig().setModelOption(option);
		manager.saveConfig();

		source.sendFeedback(
			Component.literal("[ClientSideSkins] ").withStyle(ChatFormatting.GOLD)
				.append(Component.literal("Arm model set to: ").withStyle(ChatFormatting.GRAY))
				.append(option.displayName().copy().withStyle(ChatFormatting.AQUA))
		);
		return Command.SINGLE_SUCCESS;
	}

	private static int toggleEnabled(FabricClientCommandSource source, Boolean explicitState) {
		SkinFolderManager manager = SkinFolderManager.getInstance();
		ClientSideSkinsConfig config = manager.getConfig();
		boolean newState = explicitState != null ? explicitState : !config.isEnabled();
		config.setEnabled(newState);
		manager.saveConfig();

		source.sendFeedback(
			Component.literal("[ClientSideSkins] ").withStyle(ChatFormatting.GOLD)
				.append(Component.literal("Custom skins are now ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(newState ? "ENABLED" : "DISABLED")
					.withStyle(newState ? ChatFormatting.GREEN : ChatFormatting.RED))
		);
		return Command.SINGLE_SUCCESS;
	}

	private static int reloadFolders(FabricClientCommandSource source) {
		SkinFolderManager manager = SkinFolderManager.getInstance();
		manager.reload();
		int skins = manager.getAvailableFileNames(SkinCategory.SKINS).size();
		int capes = manager.getAvailableFileNames(SkinCategory.CAPES).size();
		int elytras = manager.getAvailableFileNames(SkinCategory.ELYTRAS).size();

		source.sendFeedback(
			Component.literal("[ClientSideSkins] ").withStyle(ChatFormatting.GOLD)
				.append(Component.literal(
					"Reloaded folders (" + skins + " skin(s), " + capes + " cape(s), " + elytras + " elytra(s))."
				).withStyle(ChatFormatting.GREEN))
		);
		return Command.SINGLE_SUCCESS;
	}

	private static int listFiles(FabricClientCommandSource source, SkinCategory filter) {
		SkinFolderManager manager = SkinFolderManager.getInstance();
		manager.scanFolders();
		SkinCategory[] categories = filter != null ? new SkinCategory[]{filter} : SkinCategory.values();
		for (SkinCategory category : categories) {
			List<String> files = manager.getAvailableFileNames(category);
			String joined = files.isEmpty() ? "(none)" : String.join(", ", files);
			source.sendFeedback(
				Component.literal("[ClientSideSkins] " + category.folderName() + " (" + files.size() + "): ")
					.withStyle(ChatFormatting.GOLD)
					.append(Component.literal(joined).withStyle(ChatFormatting.WHITE))
			);
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int showStatus(FabricClientCommandSource source) {
		SkinFolderManager manager = SkinFolderManager.getInstance();
		ClientSideSkinsConfig cfg = manager.getConfig();

		String skin = cfg.getSelectedSkin().isBlank() ? "Default (Vanilla)" : cfg.getSelectedSkin();
		String cape = cfg.getSelectedCape().isBlank() ? "Default (Vanilla)" : cfg.getSelectedCape();
		String elytra = cfg.getSelectedElytra().isBlank() ? "Default (Vanilla)" : cfg.getSelectedElytra();

		source.sendFeedback(Component.literal("=== ClientSideSkins Status ===").withStyle(ChatFormatting.GOLD));
		source.sendFeedback(Component.literal("  Enabled: ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(String.valueOf(cfg.isEnabled())).withStyle(cfg.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED)));
		source.sendFeedback(Component.literal("  Skin: ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(skin).withStyle(ChatFormatting.AQUA)));
		source.sendFeedback(Component.literal("  Arm Model: ").withStyle(ChatFormatting.GRAY)
			.append(cfg.getModelOption().displayName().copy().withStyle(ChatFormatting.AQUA)));
		source.sendFeedback(Component.literal("  Cape: ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(cape).withStyle(ChatFormatting.AQUA)));
		source.sendFeedback(Component.literal("  Elytra: ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(elytra).withStyle(ChatFormatting.AQUA)));
		source.sendFeedback(Component.literal("  Folder: ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(manager.getBaseDir().toString()).withStyle(ChatFormatting.YELLOW)));
		return Command.SINGLE_SUCCESS;
	}

	private static int openFolder(FabricClientCommandSource source, SkinCategory category) {
		SkinFolderManager manager = SkinFolderManager.getInstance();
		Path dir = category != null ? manager.getCategoryDir(category) : manager.getBaseDir();
		Util.getPlatform().openPath(dir);
		source.sendFeedback(
			Component.literal("[ClientSideSkins] ").withStyle(ChatFormatting.GOLD)
				.append(Component.literal("Opened folder: " + dir).withStyle(ChatFormatting.GREEN))
		);
		return Command.SINGLE_SUCCESS;
	}
}
