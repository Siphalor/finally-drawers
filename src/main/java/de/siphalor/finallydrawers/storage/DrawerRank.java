package de.siphalor.finallydrawers.storage;

import de.siphalor.finallydrawers.FDDrawers;
import de.siphalor.finallydrawers.FinallyDrawers;
import de.siphalor.finallydrawers.item.DrawerUpgradeItem;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class DrawerRank {
	public static final transient Identifier WOOD_ID = new Identifier(
			FinallyDrawers.MOD_ID, "builtin/wood"
	);
	public static final transient Identifier CUSTOM_BORDER_TEXTURE = new Identifier(
			FinallyDrawers.MOD_ID, "block/drawer_custom_border"
	);
	private static final transient Identifier WOOD_BASE_BLOCK_ID = new Identifier("oak_wood");

	public String baseId;
	public String baseItemId;
	public String baseBlockId;
	public String name;
	public int capacity;

	private transient Item baseItem;

	public DrawerRank() {

	}

	public Identifier getBaseId() {
		return new Identifier(baseId);
	}

	public DrawerRank baseId(String baseId) {
		this.baseId = baseId;
		if (baseItemId == null) {
			baseItemId = baseId;
		}
		if (baseBlockId == null) {
			baseBlockId = baseId;
		}
		return this;
	}

	public String getBaseItemId() {
		return baseItemId;
	}

	public DrawerRank baseItemId(String itemId) {
		this.baseItemId = itemId;
		baseItem = null;
		return this;
	}

	public Item getBaseItem() {
		if (baseItem != null) {
			return baseItem;
		}
		return Registry.ITEM.get(new Identifier(baseItemId));
	}

	public String getBaseBlockId() {
		if (WOOD_ID.toString().equals(baseBlockId)) {
			return WOOD_BASE_BLOCK_ID.toString();
		}
		return baseBlockId;
	}

	public DrawerRank baseBlockId(String baseBlockId) {
		this.baseBlockId = baseBlockId;
		return this;
	}

	public String getName() {
		return name;
	}

	public DrawerRank name(String name) {
		this.name = name;
		return this;
	}

	public int getCapacity() {
		return capacity;
	}

	public DrawerRank capacity(int capacity) {
		this.capacity = capacity;
		return this;
	}

	public void createUpdate(DrawerRank to) {
		Identifier baseId = getBaseId();
		Identifier toId = to.getBaseId();
		DrawerUpgradeItem upgradeItem = FinallyDrawers.register(
				new DrawerUpgradeItem(this, to),
				"upgrades/" + baseId.getNamespace() + "/" + baseId.getPath()
						+ "/" + toId.getNamespace() + "/" + toId.getPath()
		);
		FDDrawers.UPGRADE_ITEMS.add(upgradeItem);
	}
}
