package de.siphalor.finallydrawers;

import de.siphalor.finallydrawers.client.FinallyDrawersClient;
import de.siphalor.finallydrawers.storage.DrawerRank;
import de.siphalor.finallydrawers.util.DrawerRankDisplay;
import de.siphalor.tweed4.annotated.AConfigEntry;
import de.siphalor.tweed4.annotated.AConfigListener;
import de.siphalor.tweed4.annotated.ATweedConfig;
import de.siphalor.tweed4.config.ConfigEnvironment;
import de.siphalor.tweed4.config.ConfigScope;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ATweedConfig(scope = ConfigScope.SMALLEST, environment = ConfigEnvironment.UNIVERSAL, tailors = "tweed4:coat")
public class FDConfig {
	public static General general;
	public static class General {
		@AConfigEntry(scope = ConfigScope.GAME)
		public List<DrawerRank> ranks = new ArrayList<>(Arrays.asList(
				new DrawerRank()
						.baseId(DrawerRank.WOOD_ID.toString())
						.name("Wood")
						.capacity(32),
				new DrawerRank()
						.baseId("minecraft:iron")
						.baseItemId("minecraft:iron_ingot")
						.baseBlockId("minecraft:iron_block")
						.name("Iron").capacity(64),
				new DrawerRank()
						.baseId("minecraft:gold")
						.baseItemId("minecraft:gold_ingot")
						.baseBlockId("minecraft:gold_block")
						.name("Gold").capacity(128),
				new DrawerRank()
						.baseId("minecraft:emerald")
						.baseBlockId("minecraft:emerald_block")
						.name("Emerald").capacity(256),
				new DrawerRank()
						.baseId("minecraft:diamond")
						.baseBlockId("minecraft:diamond_block")
						.name("Diamond").capacity(512),
				new DrawerRank()
						.baseId("minecraft:netherite")
						.baseBlockId("minecraft:netherite_block")
						.name("Netherite").capacity(1024)
		));

		@AConfigEntry(scope = ConfigScope.GAME)
		public List<String> woodSuffixes = new ArrayList<>(Arrays.asList(
				"_wood",
				"_hyphae"
		));

		@AConfigEntry(scope = ConfigScope.GAME)
		public List<String> strippedWoodPrefixes = new ArrayList<>(Collections.singletonList(
				"stripped_"
		));

		@AConfigListener
		public void onLoad() {
			for (int i = 0; i < ranks.size(); i++) {
				DrawerRank rank = ranks.get(i);

				FinallyDrawers.DRAWER_RANKS.put(rank.getBaseId(), rank);

				for (int j = 0; j < i; j++) {
					ranks.get(j).createUpdate(rank);
				}
			}
			FDDrawers.init();

			if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
				FinallyDrawersClient.configLoaded();
			}
		}

		public static DrawerRank getBaseRank() {
			return new DrawerRank().baseId(FinallyDrawers.MOD_ID + ":builtin/wood").capacity(32);
		}
	}

	@AConfigEntry(environment = ConfigEnvironment.CLIENT, scope = ConfigScope.SMALLEST)
	public static Appearance appearance;
	public static class Appearance {
		@AConfigEntry(comment = "Set how the rank of a drawer will be displayed: " +
				                        "on the HANDLE, the BORDER or BOTH.")
		public DrawerRankDisplay rankDisplay = DrawerRankDisplay.HANDLE;

		public boolean customRankTexture = true;

		@AConfigListener
		public void onLoad() {
			FinallyDrawersClient.configLoaded();
		}
	}
}
