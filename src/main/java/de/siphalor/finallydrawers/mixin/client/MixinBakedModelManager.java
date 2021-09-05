package de.siphalor.finallydrawers.mixin.client;

import de.siphalor.finallydrawers.client.FinallyDrawersClient;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BakedModelManager.class)
public class MixinBakedModelManager {
	@Inject(
			method = "prepare(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)Lnet/minecraft/client/render/model/ModelLoader;",
			at = @At(
					value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;endTick()V"
			),
			locals = LocalCapture.CAPTURE_FAILSOFT
	)
	public void prepared(ResourceManager resourceManager, Profiler profiler, CallbackInfoReturnable<ModelLoader> cir, ModelLoader modelLoader) {
		profiler.push("Finally Drawers - rank color calculation");
		FinallyDrawersClient.calculateRankColors(resourceManager, modelLoader);
		profiler.pop();
	}
}
