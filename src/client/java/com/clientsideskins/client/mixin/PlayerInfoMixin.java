package com.clientsideskins.client.mixin;

import com.clientsideskins.client.skin.SkinFolderManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin {
	@Shadow
	@Final
	private GameProfile profile;

	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void clientsideskins$overridePlayerInfoSkin(CallbackInfoReturnable<PlayerSkin> cir) {
		SkinFolderManager manager = SkinFolderManager.getInstance();
		if (manager.isLocalPlayer(this.profile)) {
			cir.setReturnValue(manager.applyToPlayerSkin(cir.getReturnValue()));
		}
	}
}
