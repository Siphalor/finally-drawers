package de.siphalor.finallydrawers.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import de.siphalor.finallydrawers.FDDrawers;
import de.siphalor.finallydrawers.block.DrawerBlock;
import de.siphalor.finallydrawers.storage.DrawerRank;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

@Mixin(RecipeManager.class)
public class MixinRecipeManager {
	@Inject(
			method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V",
			at = @At(
					value = "FIELD",
					target = "Lnet/minecraft/recipe/RecipeManager;recipes:Ljava/util/Map;"

			),
			locals = LocalCapture.CAPTURE_FAILSOFT
	)
	public void appendCustomRecipes(
			Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci,
			Map<RecipeType<?>, ImmutableMap.Builder<Identifier, Recipe<?>>> recipeAggregator
	) {
		ImmutableMap.Builder<Identifier, Recipe<?>> craftingRecipeAggregator = recipeAggregator.computeIfAbsent(
				RecipeType.CRAFTING, recipeType -> ImmutableMap.builder()
		);
		for (Map.Entry<Identifier, FDDrawers.DrawerData> entry : FDDrawers.DRAWER_DATA.entrySet()) {
			FDDrawers.DrawerData drawerData = entry.getValue();
			Map<String, Map<DrawerRank, DrawerBlock>> type2blocks = drawerData.getBlocks();
			Map<DrawerRank, DrawerBlock> rank2blocks = type2blocks.get("type");
		}
	}
}
