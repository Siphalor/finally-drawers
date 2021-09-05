package de.siphalor.finallydrawers.client;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import de.siphalor.finallydrawers.FDConfig;
import de.siphalor.finallydrawers.FDDrawers;
import de.siphalor.finallydrawers.FinallyDrawers;
import de.siphalor.finallydrawers.block.DrawerBlock;
import de.siphalor.finallydrawers.block.DrawerBlockEntity;
import de.siphalor.finallydrawers.item.DrawerUpgradeItem;
import de.siphalor.finallydrawers.storage.DrawerRank;
import de.siphalor.finallydrawers.storage.DrawerStorage;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.model.ModelResourceProvider;
import net.fabricmc.fabric.api.client.model.ModelVariantProvider;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.ModelRotation;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.ModelVariant;
import net.minecraft.client.render.model.json.WeightedUnbakedModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class FinallyDrawersClient implements ClientModInitializer {
	public static final Object2IntMap<String> RANK_COLOR_MAP = new Object2IntOpenHashMap<>();

	@Override
	public void onInitializeClient() {
		BlockEntityRendererRegistry.INSTANCE.register(FinallyDrawers.DRAWER_BLOCK_ENTITY_TYPE, DrawerBlockEntityRenderer::new);

		// NETWORKING
		ClientPlayNetworking.registerGlobalReceiver(FinallyDrawers.DRAWER_UPDATE_PACKET_S2C_ID, (client, handler, buf, responseSender) -> {
			Identifier dimension = buf.readIdentifier();
			if (!client.world.getRegistryKey().getValue().equals(dimension)) {
				FinallyDrawers.LOGGER.warn("Received drawer update packet for dimension that the player is not in!");
				return;
			}

			BlockPos blockPos = buf.readBlockPos();
			BlockEntity blockEntity = client.world.getBlockEntity(blockPos);
			if (!(blockEntity instanceof DrawerBlockEntity)) {
				FinallyDrawers.LOGGER.warn("Received drawer update packet for block without drawer BE!");
				return;
			}

			DrawerStorage storage = ((DrawerBlockEntity) blockEntity).getStorage();
			byte entries = buf.readByte();
			for (byte i = 0; i < entries; i++) {
				storage.getEntry(buf.readByte()).read(buf);
			}
		});

		// MODEL GENERATION
		ModelLoadingRegistry.INSTANCE.registerVariantProvider(FinallyDrawersClient::mapModelVariant);
		ModelLoadingRegistry.INSTANCE.registerResourceProvider(FinallyDrawersClient::mapModelResource);

	}

	public static void configLoaded() {
		ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
			if (stack.getItem() instanceof DrawerUpgradeItem) {
				DrawerUpgradeItem item = (DrawerUpgradeItem) stack.getItem();
				switch (tintIndex) {
					case 0:
						return RANK_COLOR_MAP.getOrDefault(item.getUpgradeFrom().getBaseId().toString(), 0xff0000);
					case 1:
						return RANK_COLOR_MAP.getOrDefault(item.getUpgradeTo().getBaseId().toString(), 0x00ff00);
					default:
					case 2:
						return 0xffffff;
				}
			}
			return 0xff00ff;
		}, FDDrawers.UPGRADE_ITEMS.toArray(new DrawerUpgradeItem[0]));

		ColorProviderRegistry.ITEM.register(
				(stack, tintIndex) -> provideDrawerColors(tintIndex, ((DrawerBlock) ((BlockItem) stack.getItem()).getBlock()).getRank()),
				FinallyDrawers.DRAWER_BLOCKS.stream().map(Block::asItem).toArray(Item[]::new)
		);
		ColorProviderRegistry.BLOCK.register(
				(state, world, pos, tintIndex) -> provideDrawerColors(tintIndex, ((DrawerBlock) state.getBlock()).getRank()),
				FinallyDrawers.DRAWER_BLOCKS.toArray(new Block[0])
		);
	}

	public static int provideDrawerColors(int tintIndex, DrawerRank rank) {
		if (!FDConfig.appearance.customRankTexture || tintIndex < 1) {
			return 0xffffff;
		}
		Identifier rankBaseId = rank.getBaseId();
		if (DrawerRank.WOOD_ID.equals(rankBaseId)) {
			return 0xffffff;
		}
		switch (FDConfig.appearance.rankDisplay) {
			case HANDLE:
				if (tintIndex == 1) return 0xffffff;
				break;
			case BORDER:
				if (tintIndex == 2) return 0xffffff;
				break;
		}
		return RANK_COLOR_MAP.getInt(rankBaseId.toString());
	}

	public static void calculateRankColors(ResourceManager resourceManager, ModelLoader modelLoader) {
		for (DrawerRank rank : FDConfig.general.ranks) {
			SpriteIdentifier spriteIdentifier = FinallyDrawersClient.extractPrimaryTexture(
					Registry.BLOCK.get(new Identifier(rank.getBaseBlockId())), modelLoader::getOrLoadModel
			);

			Identifier textureId = spriteIdentifier.getTextureId();
			try (Resource resource = resourceManager.getResource(new Identifier(textureId.getNamespace(), "textures/" + textureId.getPath() + ".png"))) {
				NativeImage image = NativeImage.read(resource.getInputStream());
				Multiset<Integer> colors = TreeMultiset.create();
				// I don't have a clue why this is deprecated
				//noinspection deprecation
				int[] pixels = image.makePixelArray();
				for (int pixel : pixels) {
					colors.add(pixel);
				}
				Integer predominant = colors.entrySet().stream()
						.max(Comparator.comparingInt(Multiset.Entry::getCount))
						.map(Multiset.Entry::getElement).orElse(0);
				FinallyDrawersClient.RANK_COLOR_MAP.put(rank.getBaseId().toString(), (int) predominant);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static SpriteIdentifier extractPrimaryTexture(Block block, Function<Identifier, UnbakedModel> unbakedModelGetter) {
		return unbakedModelGetter.apply(BlockModels.getModelId(block.getDefaultState()))
				.getTextureDependencies(unbakedModelGetter, new HashSet<>()).iterator().next();
	}

	private static ModelVariantProvider mapModelVariant(ResourceManager resourceManager) {
		return (modelId, context) -> {
			if (!FinallyDrawers.MOD_ID.equals(modelId.getNamespace())) return null;
			if (modelId.getPath().startsWith("drawers/")) {
				String facing = modelId.getVariant().substring("facing=".length());

				ModelRotation rotation;
				switch (facing) {
					case "east":
						rotation = ModelRotation.X0_Y90;
						break;
					case "south":
						rotation = ModelRotation.X0_Y180;
						break;
					case "west":
						rotation = ModelRotation.X0_Y270;
						break;
					default:
						rotation = ModelRotation.X0_Y0;
				}

				return new WeightedUnbakedModel(
						Collections.singletonList(new ModelVariant(
								new Identifier(FinallyDrawers.MOD_ID, "builtin/" + modelId.getPath()),
								rotation.getRotation(), false, 1
						))
				);
			}
			if (modelId.getPath().startsWith("upgrades/")) {
				return context.loadModel(new Identifier(FinallyDrawers.MOD_ID, "item/drawer_upgrade"));
			}
			return null;
		};
	}

	private static ModelResourceProvider mapModelResource(ResourceManager resourceManager) {
		return (resourceId, context) -> {
			if (!FinallyDrawers.MOD_ID.equals(resourceId.getNamespace())) return null;
			if (resourceId.getPath().startsWith("builtin/drawers/")) {
				// Format is builtin/drawers/<type>/<drawerNS>/<drawerPath>/<rankNS>/<rankPath>
				String[] parts = StringUtils.split(resourceId.getPath(), "/", 7);
				String type = parts[2];
				Identifier baseId = new Identifier(parts[3], parts[4]);
				FDDrawers.DrawerData drawerData = FDDrawers.DRAWER_DATA.get(baseId);
				DrawerRank rank = FinallyDrawers.DRAWER_RANKS.get(new Identifier(parts[5], parts[6]));

				return new JsonUnbakedModel(
						new Identifier(FinallyDrawers.MOD_ID, "block/drawers/" + type),
						Collections.emptyList(), new HashMap<>(),
						true, null, ModelTransformation.NONE, Collections.emptyList()
				) {
					@Override
					public Collection<SpriteIdentifier> getTextureDependencies(Function<Identifier, UnbakedModel> unbakedModelGetter, Set<Pair<String, String>> unresolvedTextureReferences) {
						Either<SpriteIdentifier, String> rankType;
						Either<SpriteIdentifier, String> wood = Either.left(
								extractPrimaryTexture(drawerData.getWood(), unbakedModelGetter)
						);
						Either<SpriteIdentifier, String> stripped = Either.left(
								extractPrimaryTexture(Registry.BLOCK.get(drawerData.getStrippedWoodId()), unbakedModelGetter)
						);
						if (DrawerRank.WOOD_ID.equals(rank.getBaseId())) {
							rankType = wood;
						} else {
							if (FDConfig.appearance.customRankTexture) {
								rankType = Either.left(new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, DrawerRank.CUSTOM_BORDER_TEXTURE));
							} else {
								rankType = Either.left(
										extractPrimaryTexture(Registry.BLOCK.get(new Identifier(rank.getBaseBlockId())), unbakedModelGetter)
								);
							}
						}

						textureMap.put("stripped_wood", stripped);

						switch (FDConfig.appearance.rankDisplay) {
							case BOTH:
								textureMap.put("wood", rankType);
								textureMap.put("border", rankType);
								break;
							case BORDER:
								textureMap.put("wood", wood);
								textureMap.put("border", rankType);
								break;
							case HANDLE:
								textureMap.put("wood", rankType);
								textureMap.put("border", wood);
								break;
						}

						return super.getTextureDependencies(unbakedModelGetter, unresolvedTextureReferences);
					}
				};
			}
			return null;
		};
	}
}
