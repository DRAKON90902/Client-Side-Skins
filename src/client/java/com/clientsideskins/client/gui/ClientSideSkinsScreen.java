package com.clientsideskins.client.gui;

import com.clientsideskins.client.config.ClientSideSkinsConfig;
import com.clientsideskins.client.config.SkinModelOption;
import com.clientsideskins.client.skin.LoadedSkinAsset;
import com.clientsideskins.client.skin.SkinCategory;
import com.clientsideskins.client.skin.SkinFolderManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerModelType;

import java.util.List;
import java.util.Locale;

public class ClientSideSkinsScreen extends Screen {
	private final Screen lastScreen;
	private SkinCategory activeCategory = SkinCategory.SKINS;

	private PngSelectionList selectionList;
	private PlayerSkinWidget skinPreviewWidget;
	private Button skinsTabButton;
	private Button capesTabButton;
	private Button elytrasTabButton;

	public ClientSideSkinsScreen(Screen lastScreen) {
		super(Component.translatable("clientsideskins.title"));
		this.lastScreen = lastScreen;
	}

	@Override
	protected void init() {
		SkinFolderManager manager = SkinFolderManager.getInstance();
		manager.scanFolders();
		ClientSideSkinsConfig config = manager.getConfig();

		int listLeft = 14;
		int listWidth = Math.max(180, this.width - 175);
		int previewLeft = listLeft + listWidth + 12;
		int previewWidth = Math.max(120, this.width - previewLeft - 14);

		int topY = 32;
		int listTop = 58;
		int listBottom = this.height - 58;
		int listHeight = Math.max(60, listBottom - listTop);

		// Category Tabs (Skins / Capes / Elytras)
		int tabGap = 4;
		int tabWidth = (listWidth - (tabGap * 2)) / 3;
		this.skinsTabButton = this.addRenderableWidget(Button.builder(
			getTabTitle(SkinCategory.SKINS),
			btn -> switchCategory(SkinCategory.SKINS)
		).bounds(listLeft, topY, tabWidth, 20).build());

		this.capesTabButton = this.addRenderableWidget(Button.builder(
			getTabTitle(SkinCategory.CAPES),
			btn -> switchCategory(SkinCategory.CAPES)
		).bounds(listLeft + tabWidth + tabGap, topY, tabWidth, 20).build());

		this.elytrasTabButton = this.addRenderableWidget(Button.builder(
			getTabTitle(SkinCategory.ELYTRAS),
			btn -> switchCategory(SkinCategory.ELYTRAS)
		).bounds(listLeft + (tabWidth + tabGap) * 2, topY, listWidth - (tabWidth + tabGap) * 2, 20).build());

		// Scrollable PNG Selection List
		this.selectionList = new PngSelectionList(this.minecraft, listWidth, listHeight, listTop, 28);
		this.selectionList.setX(listLeft);
		this.addRenderableWidget(this.selectionList);
		populateList();

		// 3D Rotatable Player Skin Preview Widget
		int previewBoxHeight = Math.max(80, listHeight - 52);
		this.skinPreviewWidget = new PlayerSkinWidget(
			previewWidth,
			previewBoxHeight,
			this.minecraft.getEntityModels(),
			manager::getCurrentEffectiveSkin
		);
		this.skinPreviewWidget.setPosition(previewLeft, listTop);
		this.addRenderableWidget(this.skinPreviewWidget);

		// Bottom Controls Row 1: Arm Model & Enabled Toggle
		int row1Y = this.height - 52;
		int halfControlWidth = (this.width - 28 - 6) / 2;

		CycleButton<SkinModelOption> modelButton = CycleButton.builder(
				SkinModelOption::displayName,
				config.getModelOption()
			)
			.withValues(SkinModelOption.values())
			.create(
				14,
				row1Y,
				halfControlWidth,
				20,
				Component.translatable("clientsideskins.model.label"),
				(btn, value) -> {
					config.setModelOption(value);
					manager.saveConfig();
				}
			);
		this.addRenderableWidget(modelButton);

		CycleButton<Boolean> enabledButton = CycleButton.onOffBuilder(config.isEnabled())
			.create(
				14 + halfControlWidth + 6,
				row1Y,
				halfControlWidth,
				20,
				Component.translatable("clientsideskins.enabled.label"),
				(btn, value) -> {
					config.setEnabled(value);
					manager.saveConfig();
				}
			);
		this.addRenderableWidget(enabledButton);

		// Bottom Controls Row 2: Rescan Folder, Open Folder, Done
		int row2Y = this.height - 28;
		int btnWidth = (this.width - 28 - 12) / 3;

		Button reloadButton = Button.builder(
			Component.translatable("clientsideskins.button.reload"),
			btn -> {
				manager.reload();
				updateTabTitles();
				populateList();
			}
		).bounds(14, row2Y, btnWidth, 20)
			.tooltip(Tooltip.create(Component.literal("Rescan ClientSideSkins folders for new or modified PNG files")))
			.build();
		this.addRenderableWidget(reloadButton);

		Button openFolderButton = Button.builder(
			Component.translatable("clientsideskins.button.open_folder"),
			btn -> Util.getPlatform().openPath(manager.getCategoryDir(this.activeCategory))
		).bounds(14 + btnWidth + 6, row2Y, btnWidth, 20)
			.tooltip(Tooltip.create(Component.literal(manager.getCategoryDir(this.activeCategory).toString())))
			.build();
		this.addRenderableWidget(openFolderButton);

		Button doneButton = Button.builder(
			CommonComponents.GUI_DONE,
			btn -> onClose()
		).bounds(14 + (btnWidth + 6) * 2, row2Y, this.width - 28 - (btnWidth + 6) * 2, 20).build();
		this.addRenderableWidget(doneButton);

		updateTabTitles();
	}

	private Component getTabTitle(SkinCategory category) {
		int count = SkinFolderManager.getInstance().getAvailableFileNames(category).size();
		Component base = category.tabTitle(count);
		if (category == this.activeCategory) {
			return Component.literal("> ").append(base).append(Component.literal(" <")).withStyle(ChatFormatting.YELLOW);
		}
		return base;
	}

	private void updateTabTitles() {
		if (this.skinsTabButton != null) {
			this.skinsTabButton.setMessage(getTabTitle(SkinCategory.SKINS));
		}
		if (this.capesTabButton != null) {
			this.capesTabButton.setMessage(getTabTitle(SkinCategory.CAPES));
		}
		if (this.elytrasTabButton != null) {
			this.elytrasTabButton.setMessage(getTabTitle(SkinCategory.ELYTRAS));
		}
	}

	private void switchCategory(SkinCategory category) {
		if (this.activeCategory != category) {
			this.activeCategory = category;
			updateTabTitles();
			populateList();
		}
	}

	private void populateList() {
		if (this.selectionList == null) {
			return;
		}
		this.selectionList.reloadCategoryEntries(this.activeCategory);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		graphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

		if (this.skinPreviewWidget != null) {
			int previewLeft = this.skinPreviewWidget.getX();
			int previewRight = this.skinPreviewWidget.getRight();
			int previewCenterX = (previewLeft + previewRight) / 2;
			int previewTop = this.skinPreviewWidget.getY();
			int previewBottom = this.skinPreviewWidget.getBottom();

			// Draw a subtle border around the 3D preview box
			graphics.fill(previewLeft, previewTop, previewRight, previewBottom, 0x55000000);
			graphics.outline(previewLeft, previewTop, this.skinPreviewWidget.getWidth(), this.skinPreviewWidget.getHeight(), 0x88AAAAAA);

			graphics.centeredText(
				this.font,
				Component.translatable("clientsideskins.preview.title"),
				previewCenterX,
				previewTop - 12,
				0xFFE0E0E0
			);
			graphics.centeredText(
				this.font,
				Component.translatable("clientsideskins.preview.drag_hint"),
				previewCenterX,
				previewBottom - 11,
				0xFF888888
			);

			// Draw 2D Cape & Elytra summary box below the 3D preview
			int summaryTop = previewBottom + 4;
			int summaryHeight = 46;
			graphics.fill(previewLeft, summaryTop, previewRight, summaryTop + summaryHeight, 0x66000000);
			graphics.outline(previewLeft, summaryTop, this.skinPreviewWidget.getWidth(), summaryHeight, 0x66888888);

			SkinFolderManager manager = SkinFolderManager.getInstance();
			String skinName = emptyToDefault(manager.getSelectedFile(SkinCategory.SKINS));
			String capeName = emptyToDefault(manager.getSelectedFile(SkinCategory.CAPES));
			String elytraName = emptyToDefault(manager.getSelectedFile(SkinCategory.ELYTRAS));

			graphics.text(this.font, trimToWidth("Skin: " + skinName, this.skinPreviewWidget.getWidth() - 8), previewLeft + 4, summaryTop + 4, 0xFFDDDDDD);
			graphics.text(this.font, trimToWidth("Cape: " + capeName, this.skinPreviewWidget.getWidth() - 24), previewLeft + 4, summaryTop + 18, 0xFFBBBBBB);
			graphics.text(this.font, trimToWidth("Elytra: " + elytraName, this.skinPreviewWidget.getWidth() - 24), previewLeft + 4, summaryTop + 32, 0xFFBBBBBB);

			// Miniature 2D icon of selected cape and elytra if present
			LoadedSkinAsset capeAsset = manager.getOrLoadAsset(SkinCategory.CAPES, manager.getSelectedFile(SkinCategory.CAPES));
			if (capeAsset != null) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, capeAsset.textureId(), previewRight - 16, summaryTop + 15, 1.0f, 1.0f, 10, 14, 10, 16, 64, 32);
			}
			LoadedSkinAsset elytraAsset = manager.getOrLoadAsset(SkinCategory.ELYTRAS, manager.getSelectedFile(SkinCategory.ELYTRAS));
			if (elytraAsset != null) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, elytraAsset.textureId(), previewRight - 16, summaryTop + 30, 1.0f, 1.0f, 10, 14, 10, 16, 64, 32);
			}
		}
	}

	private String trimToWidth(String text, int maxWidth) {
		if (this.font.width(text) <= maxWidth) {
			return text;
		}
		String ellipsis = "...";
		int target = Math.max(10, maxWidth - this.font.width(ellipsis));
		return this.font.plainSubstrByWidth(text, target) + ellipsis;
	}

	private static String emptyToDefault(String value) {
		return (value == null || value.isBlank()) ? "Default" : value;
	}

	@Override
	public void onClose() {
		SkinFolderManager.getInstance().saveConfig();
		this.minecraft.gui.setScreen(this.lastScreen);
	}

	private class PngSelectionList extends ObjectSelectionList<PngEntry> {
		public PngSelectionList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
			super(minecraft, width, height, y, itemHeight);
		}

		public void reloadCategoryEntries(SkinCategory category) {
			this.clearEntries();
			SkinFolderManager manager = SkinFolderManager.getInstance();
			String currentSelected = manager.getSelectedFile(category);

			// Default / Vanilla reset entry
			PngEntry defaultEntry = new PngEntry(category, "");
			this.addEntry(defaultEntry);
			if (currentSelected.isBlank()) {
				this.setSelected(defaultEntry);
			}

			List<String> files = manager.getAvailableFileNames(category);
			for (String fileName : files) {
				PngEntry entry = new PngEntry(category, fileName);
				this.addEntry(entry);
				if (fileName.equalsIgnoreCase(currentSelected)) {
					this.setSelected(entry);
				}
			}
		}

		@Override
		public int getRowWidth() {
			return Math.max(140, this.getWidth() - 24);
		}
	}

	private class PngEntry extends ObjectSelectionList.Entry<PngEntry> {
		private final SkinCategory category;
		private final String fileName;

		public PngEntry(SkinCategory category, String fileName) {
			this.category = category;
			this.fileName = fileName != null ? fileName : "";
		}

		private boolean isDefaultEntry() {
			return this.fileName.isBlank();
		}

		private boolean isActiveSelection() {
			String active = SkinFolderManager.getInstance().getSelectedFile(this.category);
			if (isDefaultEntry()) {
				return active == null || active.isBlank();
			}
			return this.fileName.equalsIgnoreCase(active);
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			int x = this.getContentX();
			int y = this.getContentY();
			int width = this.getContentWidth();
			boolean active = isActiveSelection();

			int iconSize = 20;
			int textX = x + iconSize + 8;

			if (isDefaultEntry()) {
				// Default skin face or placeholder box
				if (this.category == SkinCategory.SKINS) {
					PlayerFaceExtractor.extractRenderState(
						graphics,
						 net.minecraft.client.resources.DefaultPlayerSkin.getDefaultTexture(),
						x + 2,
						y + 2,
						iconSize,
						true,
						false,
						-1
					);
				} else {
					graphics.fill(x + 2, y + 2, x + 2 + iconSize, y + 2 + iconSize, 0x44888888);
					graphics.outline(x + 2, y + 2, iconSize, iconSize, 0xFF888888);
				}

				int titleColor = active ? 0xFFFFCC00 : 0xFFFFFFFF;
				graphics.text(
					ClientSideSkinsScreen.this.font,
					Component.translatable("clientsideskins.entry.default"),
					textX,
					y + 3,
					titleColor
				);
				graphics.text(
					ClientSideSkinsScreen.this.font,
					Component.literal("Use standard Mojang " + this.category.folderName().substring(0, this.category.folderName().length() - 1)),
					textX,
					y + 14,
					0xFF888888
				);
			} else {
				LoadedSkinAsset asset = SkinFolderManager.getInstance().getOrLoadAsset(this.category, this.fileName);
				if (asset != null) {
					if (this.category == SkinCategory.SKINS) {
						PlayerFaceExtractor.extractRenderState(
							graphics,
							asset.textureId(),
							x + 2,
							y + 2,
							iconSize,
							true,
							false,
							-1
						);
					} else {
						// Render front of cape/elytra (UV 1,1 size 10x16 on 64x32 grid)
						graphics.fill(x + 2, y + 2, x + 2 + iconSize, y + 2 + iconSize, 0x44000000);
						graphics.blit(
							RenderPipelines.GUI_TEXTURED,
							asset.textureId(),
							x + 5,
							y + 2,
							1.0f,
							1.0f,
							14,
							20,
							10,
							16,
							64,
							32
						);
					}
				} else {
					graphics.fill(x + 2, y + 2, x + 2 + iconSize, y + 2 + iconSize, 0x44AA0000);
				}

				int titleColor = active ? 0xFFFFCC00 : 0xFFFFFFFF;
				graphics.text(
					ClientSideSkinsScreen.this.font,
					Component.literal(this.fileName),
					textX,
					y + 3,
					titleColor
				);

				String meta;
				if (asset != null) {
					if (this.category == SkinCategory.SKINS) {
						String modelStr = asset.detectedModel() == PlayerModelType.SLIM ? "Slim" : "Wide";
						meta = asset.width() + "x" + asset.height() + " • Auto: " + modelStr;
					} else {
						meta = asset.width() + "x" + asset.height() + " PNG";
					}
				} else {
					meta = "PNG File";
				}
				graphics.text(
					ClientSideSkinsScreen.this.font,
					Component.literal(meta),
					textX,
					y + 14,
					0xFF999999
				);
			}

			if (active) {
				Component badge = Component.literal("[Equipped]");
				int badgeWidth = ClientSideSkinsScreen.this.font.width(badge);
				graphics.text(
					ClientSideSkinsScreen.this.font,
					badge,
					x + width - badgeWidth - 4,
					y + 8,
					0xFF55FF55
				);
			}
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
			SkinFolderManager.getInstance().setSelectedFile(this.category, this.fileName);
			ClientSideSkinsScreen.this.selectionList.setSelected(this);
			return true;
		}

		@Override
		public Component getNarration() {
			return Component.literal(isDefaultEntry() ? "Default" : this.fileName);
		}
	}
}
