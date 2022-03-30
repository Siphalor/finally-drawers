package de.siphalor.finallydrawers;

import de.siphalor.finallydrawers.block.DrawerBlock;
import de.siphalor.finallydrawers.item.DrawerUpgradeItem;
import de.siphalor.finallydrawers.storage.DrawerRank;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FDDrawers {
	private static final ConcurrentMap<Identifier, DrawerData> gameRegisteredBlockData = new ConcurrentHashMap<>();
	public static final Map<Identifier, DrawerData> DRAWER_DATA = new ConcurrentHashMap<>();
	public static final Set<DrawerUpgradeItem> UPGRADE_ITEMS = ConcurrentHashMap.newKeySet();

	public static void init() {
		Registry.BLOCK.getIndexedEntries().forEach(entry -> {
			if (entry.getKey().isPresent()) {
				processRegisteredBlock(entry.getKey().get().getValue(), entry.value());
			}
		});
		RegistryEntryAddedCallback.event(Registry.BLOCK).register((rawId, id, block) -> FDDrawers.processRegisteredBlock(id, block));
	}

	private static void processRegisteredBlock(Identifier id, Block block) {
		// first, check if the block ends with a wood suffix and get the id without suffix
		Identifier woodId = null;
		Identifier baseId = null;
		for (String woodSuffix : FDConfig.general.woodSuffixes) {
			if (id.getPath().endsWith(woodSuffix)) {
				baseId = new Identifier(id.getNamespace(), StringUtils.substring(id.getPath(), 0, -woodSuffix.length()));
				woodId = id;
				break;
			}
		}
		if (woodId == null) {
			// if it doesn't, then we don't care about it
			return;
		}

		// let's see if it's a stripped variant
		for (String strippedWoodPrefix : FDConfig.general.strippedWoodPrefixes) {
			if (baseId.getPath().startsWith(strippedWoodPrefix)) {
				baseId = new Identifier(id.getNamespace(), baseId.getPath().substring(strippedWoodPrefix.length()));
				DrawerData drawerData = gameRegisteredBlockData.computeIfAbsent(baseId, DrawerData::new);
				drawerData.setStrippedWoodId(id);

				if (drawerData.hasWood()) {
					create(drawerData);
				}
				return;

			}
		}

		// ok, it's not stripped, so let's see if we have a stripped variant on the waiting list already
		DrawerData drawerData = gameRegisteredBlockData.computeIfAbsent(baseId, DrawerData::new);
		drawerData.setWood(woodId, block);

		if (drawerData.hasStrippedWood()) {
			create(drawerData);
		}

	}

	public static void create(DrawerData drawerData) {
		gameRegisteredBlockData.remove(drawerData.getBaseId());
		DRAWER_DATA.put(drawerData.getBaseId(), drawerData);

		FinallyDrawers.log(Level.INFO, "Registered drawer: " + drawerData.getBaseId());

		createDrawer(drawerData, "normal", 1, 1);
		createDrawer(drawerData, "quarter", 2, 2);
	}

	public static void createDrawer(DrawerData drawerData, String type, int gridWidth, int gridHeight) {
		Map<DrawerRank, DrawerBlock> rank2Blocks = drawerData.getBlocks().computeIfAbsent(type, _key -> new HashMap<>());

		String drawerPath = "drawers/" + type + "/"
				                    + drawerData.getBaseId().getNamespace() + "/" + drawerData.getBaseId().getPath() + "/";
		for (DrawerRank rank : FDConfig.general.ranks) {
			Identifier rankId = rank.getBaseId();
			DrawerBlock drawerBlock = new DrawerBlock(
					FabricBlockSettings.copyOf(drawerData.getWood()), type, gridWidth, gridHeight, drawerData.getBaseId(), rank
			);
			Identifier id = new Identifier(FinallyDrawers.MOD_ID, drawerPath + rankId.getNamespace() + "/" + rankId.getPath());
			Registry.register(Registry.BLOCK, id, drawerBlock);
			rank2Blocks.put(rank, drawerBlock);
			BlockItem drawerItem = Registry.register(
					Registry.ITEM, id,
					new BlockItem(drawerBlock, new Item.Settings().group(FinallyDrawers.ITEM_GROUP))
			);
			BlockItem.BLOCK_ITEMS.put(drawerBlock, drawerItem);
			FinallyDrawers.DRAWER_BLOCKS.add(drawerBlock);
		}
	}

	public static class DrawerData {
		private final Identifier baseId;
		private Identifier woodId;
		private Block wood;
		private Identifier strippedWoodId;
		private final Map<String, Map<DrawerRank, DrawerBlock>> blocks = new HashMap<>();

		public DrawerData(Identifier baseId) {
			this.baseId = baseId;
		}

		public Identifier getBaseId() {
			return baseId;
		}

		public boolean hasWood() {
			return woodId != null;
		}

		public Block getWood() {
			return wood;
		}

		public Identifier getWoodId() {
			return woodId;
		}

		public boolean hasStrippedWood() {
			return strippedWoodId != null;
		}

		public Identifier getStrippedWoodId() {
			return strippedWoodId;
		}

		public void setWood(Identifier woodId, Block wood) {
			this.woodId = woodId;
			this.wood = wood;
		}

		public void setStrippedWoodId(Identifier strippedWoodId) {
			this.strippedWoodId = strippedWoodId;
		}

		public Map<String, Map<DrawerRank, DrawerBlock>> getBlocks() {
			return blocks;
		}
	}
}
