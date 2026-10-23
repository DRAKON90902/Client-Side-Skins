package com.clientsideskins.client.mixin;

import com.clientsideskins.client.skin.SkinFolderManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void clientsideskins$overridePlayerSkin(CallbackInfoReturnable<PlayerSkin> cir) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		SkinFolderManager manager = SkinFolderManager.getInstance();
		if (manager.isLocalPlayer(self)) {
			cir.setReturnValue(manager.applyToPlayerSkin(cir.getReturnValue()));
		}
	}
}
