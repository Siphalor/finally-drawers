package de.siphalor.finallydrawers.item;

import de.siphalor.finallydrawers.FinallyDrawers;
import de.siphalor.finallydrawers.storage.DrawerRank;
import net.minecraft.item.Item;

public class DrawerUpgradeItem extends Item {
	private final DrawerRank from;
	private final DrawerRank to;

	public DrawerUpgradeItem(DrawerRank from, DrawerRank to) {
		super(new Settings().group(FinallyDrawers.ITEM_GROUP));
		this.from = from;
		this.to = to;
	}

	public DrawerRank getUpgradeFrom() {
		return from;
	}

	public DrawerRank getUpgradeTo() {
		return to;
	}
}
