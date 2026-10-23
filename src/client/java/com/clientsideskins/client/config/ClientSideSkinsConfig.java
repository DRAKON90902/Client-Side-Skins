package com.clientsideskins.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientSideSkinsConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("ClientSideSkins");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private boolean enabled = true;
	private String selectedSkin = "";
	private String selectedCape = "";
	private String selectedElytra = "";
	private SkinModelOption modelOption = SkinModelOption.AUTO;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getSelectedSkin() {
		return this.selectedSkin != null ? this.selectedSkin : "";
	}

	public void setSelectedSkin(String selectedSkin) {
		this.selectedSkin = selectedSkin != null ? selectedSkin : "";
	}

	public String getSelectedCape() {
		return this.selectedCape != null ? this.selectedCape : "";
	}

	public void setSelectedCape(String selectedCape) {
		this.selectedCape = selectedCape != null ? selectedCape : "";
	}

	public String getSelectedElytra() {
		return this.selectedElytra != null ? this.selectedElytra : "";
	}

	public void setSelectedElytra(String selectedElytra) {
		this.selectedElytra = selectedElytra != null ? selectedElytra : "";
	}

	public SkinModelOption getModelOption() {
		return this.modelOption != null ? this.modelOption : SkinModelOption.AUTO;
	}

	public void setModelOption(SkinModelOption modelOption) {
		this.modelOption = modelOption != null ? modelOption : SkinModelOption.AUTO;
	}

	public static ClientSideSkinsConfig load(Path configFile) {
		if (Files.isRegularFile(configFile)) {
			try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
				ClientSideSkinsConfig loaded = GSON.fromJson(reader, ClientSideSkinsConfig.class);
				if (loaded != null) {
					if (loaded.selectedSkin == null) loaded.selectedSkin = "";
					if (loaded.selectedCape == null) loaded.selectedCape = "";
					if (loaded.selectedElytra == null) loaded.selectedElytra = "";
					if (loaded.modelOption == null) loaded.modelOption = SkinModelOption.AUTO;
					return loaded;
				}
			} catch (Exception e) {
				LOGGER.warn("Failed to read ClientSideSkins config from {}, using defaults", configFile, e);
			}
		}
		ClientSideSkinsConfig config = new ClientSideSkinsConfig();
		config.save(configFile);
		return config;
	}

	public void save(Path configFile) {
		try {
			if (configFile.getParent() != null) {
				Files.createDirectories(configFile.getParent());
			}
			try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to save ClientSideSkins config to {}", configFile, e);
		}
	}
}
