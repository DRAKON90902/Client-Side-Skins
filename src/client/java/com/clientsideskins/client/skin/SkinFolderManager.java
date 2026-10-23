package com.clientsideskins.client.skin;

import com.clientsideskins.client.config.ClientSideSkinsConfig;
import com.clientsideskins.client.config.SkinModelOption;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public final class SkinFolderManager {
	public static final String MOD_ID = "clientsideskins";
	private static final Logger LOGGER = LoggerFactory.getLogger("ClientSideSkins");
	private static final SkinFolderManager INSTANCE = new SkinFolderManager();

	private final Path baseDir;
	private final Path skinsDir;
	private final Path capesDir;
	private final Path elytrasDir;
	private final Path configFile;

	private ClientSideSkinsConfig config;
	private final Map<SkinCategory, Map<String, Path>> discoveredFiles = new EnumMap<>(SkinCategory.class);
	private final Map<SkinCategory, Map<String, LoadedSkinAsset>> loadedTextures = new EnumMap<>(SkinCategory.class);
	private final Set<Identifier> registeredTextureIds = new HashSet<>();

	private SkinFolderManager() {
		this.baseDir = FabricLoader.getInstance().getGameDir().resolve("ClientSideSkins");
		this.skinsDir = this.baseDir.resolve(SkinCategory.SKINS.folderName());
		this.capesDir = this.baseDir.resolve(SkinCategory.CAPES.folderName());
		this.elytrasDir = this.baseDir.resolve(SkinCategory.ELYTRAS.folderName());
		this.configFile = this.baseDir.resolve("config.json");

		for (SkinCategory category : SkinCategory.values()) {
			this.discoveredFiles.put(category, new LinkedHashMap<>());
			this.loadedTextures.put(category, new LinkedHashMap<>());
		}
	}

	public static SkinFolderManager getInstance() {
		return INSTANCE;
	}

	public void initialize() {
		ensureDirectoriesExist();
		this.config = ClientSideSkinsConfig.load(this.configFile);
		scanFolders();
		LOGGER.info(
			"ClientSideSkins initialized at {}. Found {} skin(s), {} cape(s), {} elytra(s).",
			this.baseDir,
			getAvailableFileNames(SkinCategory.SKINS).size(),
			getAvailableFileNames(SkinCategory.CAPES).size(),
			getAvailableFileNames(SkinCategory.ELYTRAS).size()
		);
	}

	private void ensureDirectoriesExist() {
		try {
			Files.createDirectories(this.baseDir);
			Files.createDirectories(this.skinsDir);
			Files.createDirectories(this.capesDir);
			Files.createDirectories(this.elytrasDir);

			Path readme = this.baseDir.resolve("README.txt");
			if (!Files.exists(readme)) {
				String instructions = """
					==================================================
					            ClientSideSkins Folder
					==================================================
					Place your custom .png textures into the folders below:
					  - skins/   : Player skin .png files (64x64, 64x32 legacy, or HD 128x128+)
					  - capes/   : Cape .png files (64x32 or HD)
					  - elytras/ : Elytra .png files (64x32 or HD)
					(Note: .png files placed directly in this root folder will also appear under Skins.)

					In-Game Usage:
					  - Open Options -> "Client Side Skins..." (or Options -> Skin Customization -> "Client Side Skins...")
					  - Or use the client command:
					      /clientsideskins            (or /css) - Opens the skin selector menu
					      /css skin set <file> [model]          - Equip a skin PNG
					      /css skin clear                       - Reset skin to default
					      /css model <auto|wide|slim>           - Change arm model
					      /css cape set <file>                  - Equip a cape PNG
					      /css cape clear                       - Reset cape to default
					      /css elytra set <file>                - Equip an elytra PNG
					      /css elytra clear                     - Reset elytra to default
					      /css reload                           - Rescan folders and reload PNGs
					      /css status                           - View active skin/cape/elytra
					""";
				Files.writeString(readme, instructions, StandardCharsets.UTF_8);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to create ClientSideSkins directories at {}", this.baseDir, e);
		}
	}

	public synchronized void reload() {
		ensureDirectoriesExist();
		this.config = ClientSideSkinsConfig.load(this.configFile);
		clearLoadedTextures();
		scanFolders();
	}

	private void clearLoadedTextures() {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && mc.getTextureManager() != null && !this.registeredTextureIds.isEmpty()) {
			Set<Identifier> toRelease = new HashSet<>(this.registeredTextureIds);
			this.registeredTextureIds.clear();
			if (mc.isSameThread()) {
				TextureManager tm = mc.getTextureManager();
				for (Identifier id : toRelease) {
					tm.release(id);
				}
			} else {
				mc.execute(() -> {
					TextureManager tm = mc.getTextureManager();
					for (Identifier id : toRelease) {
						tm.release(id);
					}
				});
			}
		}
		for (SkinCategory category : SkinCategory.values()) {
			this.loadedTextures.get(category).clear();
		}
	}

	public synchronized void scanFolders() {
		ensureDirectoriesExist();
		for (SkinCategory category : SkinCategory.values()) {
			this.discoveredFiles.get(category).clear();
		}

		// Scan skins/ subfolder AND root ClientSideSkins/ folder for .png files
		scanDirectoryInto(this.skinsDir, this.discoveredFiles.get(SkinCategory.SKINS));
		scanDirectoryInto(this.baseDir, this.discoveredFiles.get(SkinCategory.SKINS));

		// Scan capes/ and elytras/ subfolders
		scanDirectoryInto(this.capesDir, this.discoveredFiles.get(SkinCategory.CAPES));
		scanDirectoryInto(this.elytrasDir, this.discoveredFiles.get(SkinCategory.ELYTRAS));
	}

	private void scanDirectoryInto(Path dir, Map<String, Path> targetMap) {
		if (!Files.isDirectory(dir)) {
			return;
		}
		try (Stream<Path> stream = Files.list(dir)) {
			List<Path> pngFiles = stream
				.filter(Files::isRegularFile)
				.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
				.sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
				.toList();

			for (Path path : pngFiles) {
				String fileName = path.getFileName().toString();
				targetMap.putIfAbsent(fileName, path);
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to scan directory {} for PNG files", dir, e);
		}
	}

	public synchronized List<String> getAvailableFileNames(SkinCategory category) {
		return new ArrayList<>(this.discoveredFiles.get(category).keySet());
	}

	public synchronized Path resolveFile(SkinCategory category, String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return null;
		}
		Map<String, Path> files = this.discoveredFiles.get(category);
		Path exact = files.get(fileName);
		if (exact != null) {
			return exact;
		}
		// Case-insensitive & optional .png suffix lookup for convenient command usage
		String normalized = fileName.trim();
		String withPng = normalized.toLowerCase(Locale.ROOT).endsWith(".png") ? normalized : normalized + ".png";
		for (Map.Entry<String, Path> entry : files.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(normalized) || entry.getKey().equalsIgnoreCase(withPng)) {
				return entry.getValue();
			}
		}
		return null;
	}

	public synchronized String findCanonicalFileName(SkinCategory category, String fileName) {
		Path path = resolveFile(category, fileName);
		return path != null ? path.getFileName().toString() : null;
	}

	public synchronized LoadedSkinAsset getOrLoadAsset(SkinCategory category, String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return null;
		}
		String canonicalName = findCanonicalFileName(category, fileName);
		if (canonicalName == null) {
			return null;
		}
		Map<String, LoadedSkinAsset> categoryCache = this.loadedTextures.get(category);
		LoadedSkinAsset existing = categoryCache.get(canonicalName);
		if (existing != null) {
			return existing;
		}

		Minecraft mc = Minecraft.getInstance();
		if (mc == null || !mc.isSameThread() || mc.getTextureManager() == null) {
			return null;
		}

		Path filePath = this.discoveredFiles.get(category).get(canonicalName);
		if (filePath == null || !Files.isRegularFile(filePath)) {
			return null;
		}

		try {
			NativeImage image = SkinImageProcessor.loadAndProcessImage(filePath, category);
			int width = image.getWidth();
			int height = image.getHeight();
			PlayerModelType detectedModel = category == SkinCategory.SKINS
				? SkinImageProcessor.detectModelType(image)
				: PlayerModelType.WIDE;

			String sanitized = sanitizeForIdentifier(canonicalName);
			String hash = Integer.toHexString(canonicalName.hashCode());
			Identifier textureId = Identifier.fromNamespaceAndPath(
				MOD_ID,
				category.folderName() + "/" + sanitized + "_" + hash
			);

			DynamicTexture dynamicTexture = new DynamicTexture(textureId::toString, image);
			mc.getTextureManager().register(textureId, dynamicTexture);
			this.registeredTextureIds.add(textureId);

			ClientAsset.Texture clientTexture = new ClientAsset.ResourceTexture(textureId, textureId);
			LoadedSkinAsset loaded = new LoadedSkinAsset(
				canonicalName,
				filePath,
				category,
				clientTexture,
				width,
				height,
				detectedModel
			);
			categoryCache.put(canonicalName, loaded);
			return loaded;
		} catch (Exception e) {
			LOGGER.warn("Failed to load PNG texture {} from {}", canonicalName, filePath, e);
			return null;
		}
	}

	private static String sanitizeForIdentifier(String name) {
		String lower = name.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".png")) {
			lower = lower.substring(0, lower.length() - 4);
		}
		StringBuilder sb = new StringBuilder(lower.length());
		for (int i = 0; i < lower.length(); i++) {
			char c = lower.charAt(i);
			if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
				sb.append(c);
			} else {
				sb.append('_');
			}
		}
		return sb.isEmpty() ? "skin" : sb.toString();
	}

	public ClientSideSkinsConfig getConfig() {
		if (this.config == null) {
			this.config = ClientSideSkinsConfig.load(this.configFile);
		}
		return this.config;
	}

	public void saveConfig() {
		if (this.config != null) {
			this.config.save(this.configFile);
		}
	}

	public Path getBaseDir() {
		return this.baseDir;
	}

	public Path getCategoryDir(SkinCategory category) {
		return switch (category) {
			case SKINS -> this.skinsDir;
			case CAPES -> this.capesDir;
			case ELYTRAS -> this.elytrasDir;
		};
	}

	public String getSelectedFile(SkinCategory category) {
		ClientSideSkinsConfig cfg = getConfig();
		return switch (category) {
			case SKINS -> cfg.getSelectedSkin();
			case CAPES -> cfg.getSelectedCape();
			case ELYTRAS -> cfg.getSelectedElytra();
		};
	}

	public void setSelectedFile(SkinCategory category, String fileName) {
		ClientSideSkinsConfig cfg = getConfig();
		String value = fileName != null ? fileName : "";
		switch (category) {
			case SKINS -> cfg.setSelectedSkin(value);
			case CAPES -> cfg.setSelectedCape(value);
			case ELYTRAS -> cfg.setSelectedElytra(value);
		}
		saveConfig();
	}

	public boolean isLocalPlayer(AbstractClientPlayer player) {
		if (player == null) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc == null) {
			return false;
		}
		if (player == mc.player) {
			return true;
		}
		return isLocalPlayer(player.getGameProfile());
	}

	public boolean isLocalPlayer(GameProfile profile) {
		if (profile == null) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc == null) {
			return false;
		}
		UUID id = profile.id();
		if (id != null) {
			if (mc.isLocalPlayer(id)) {
				return true;
			}
			if (mc.player != null && id.equals(mc.player.getUUID())) {
				return true;
			}
		}
		String name = profile.name();
		if (name != null && !name.isBlank() && mc.getUser() != null) {
			if (name.equalsIgnoreCase(mc.getUser().getName())) {
				return true;
			}
		}
		return false;
	}

	public PlayerSkin applyToPlayerSkin(PlayerSkin original) {
		if (original == null) {
			original = DefaultPlayerSkin.getDefaultSkin();
		}
		ClientSideSkinsConfig cfg = getConfig();
		if (!cfg.isEnabled()) {
			return original;
		}

		ClientAsset.Texture body = original.body();
		ClientAsset.Texture cape = original.cape();
		ClientAsset.Texture elytra = original.elytra();
		PlayerModelType model = original.model();

		LoadedSkinAsset customSkin = getOrLoadAsset(SkinCategory.SKINS, cfg.getSelectedSkin());
		if (customSkin != null) {
			body = customSkin.texture();
			model = cfg.getModelOption().resolve(customSkin.detectedModel());
		} else if (cfg.getModelOption() != SkinModelOption.AUTO) {
			model = cfg.getModelOption().resolve(original.model());
		}

		LoadedSkinAsset customCape = getOrLoadAsset(SkinCategory.CAPES, cfg.getSelectedCape());
		if (customCape != null) {
			cape = customCape.texture();
		}

		LoadedSkinAsset customElytra = getOrLoadAsset(SkinCategory.ELYTRAS, cfg.getSelectedElytra());
		if (customElytra != null) {
			elytra = customElytra.texture();
		}

		return new PlayerSkin(body, cape, elytra, model, original.secure());
	}

	public PlayerSkin getCurrentEffectiveSkin() {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && mc.player != null) {
			return mc.player.getSkin();
		}
		PlayerSkin base = (mc != null && mc.getUser() != null && mc.getUser().getProfileId() != null)
			? DefaultPlayerSkin.get(mc.getUser().getProfileId())
			: DefaultPlayerSkin.getDefaultSkin();
		return applyToPlayerSkin(base);
	}
}
