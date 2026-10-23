package com.clientsideskins.client.mixin;

import com.clientsideskins.client.skin.SkinFolderManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerSkinRenderCache.RenderInfo.class)
public abstract class PlayerSkinRenderCacheInfoMixin {
	@Shadow
	@Final
	private GameProfile gameProfile;

	@Inject(method = "playerSkin", at = @At("RETURN"), cancellable = true)
	private void clientsideskins$overrideCachedPlayerSkin(CallbackInfoReturnable<PlayerSkin> cir) {
		SkinFolderManager manager = SkinFolderManager.getInstance();
		if (manager.isLocalPlayer(this.gameProfile)) {
			cir.setReturnValue(manager.applyToPlayerSkin(cir.getReturnValue()));
		}
	}
}
