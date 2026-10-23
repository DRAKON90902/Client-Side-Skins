package com.clientsideskins.client.mixin;

import com.clientsideskins.client.gui.ClientSideSkinsScreen;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkinCustomizationScreen.class)
public abstract class SkinCustomizationScreenMixin extends OptionsSubScreen {
	protected SkinCustomizationScreenMixin(Screen lastScreen, Options options, Component title) {
		super(lastScreen, options, title);
	}

	@Inject(method = "addOptions", at = @At("HEAD"))
	private void clientsideskins$addSkinCustomizationButton(CallbackInfo ci) {
		if (this.list != null) {
			this.list.addBig(
				Button.builder(
					Component.translatable("clientsideskins.options.button"),
					btn -> this.minecraft.gui.setScreen(new ClientSideSkinsScreen((SkinCustomizationScreen) (Object) this))
				).build()
			);
		}
	}
}
