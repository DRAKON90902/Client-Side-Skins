package com.clientsideskins.client.mixin;

import com.clientsideskins.client.gui.ClientSideSkinsScreen;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
	protected OptionsScreenMixin(Component title) {
		super(title);
	}

	@Shadow
	private Button openScreenButton(Component message, Supplier<Screen> screenToOpen) {
		throw new AssertionError();
	}

	@Inject(
		method = "init",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToContents(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"
		)
	)
	private void clientsideskins$addOptionsButton(CallbackInfo ci, @Local GridLayout.RowHelper rowHelper) {
		rowHelper.addChild(
			this.openScreenButton(
				Component.translatable("clientsideskins.options.button"),
				() -> new ClientSideSkinsScreen((OptionsScreen) (Object) this)
			)
		);
	}
}
