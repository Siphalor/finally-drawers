package de.siphalor.finallydrawers.item;

import de.siphalor.finallydrawers.FDDrawers;
import de.siphalor.finallydrawers.FinallyDrawers;
import de.siphalor.finallydrawers.block.DrawerBlock;
import de.siphalor.finallydrawers.block.DrawerBlockEntity;
import de.siphalor.finallydrawers.storage.DrawerRank;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		BlockPos pos = context.getBlockPos();
		World world = context.getWorld();
		BlockState blockState = world.getBlockState(pos);
		if (blockState.getBlock() instanceof DrawerBlock) {
			DrawerBlock block = (DrawerBlock) blockState.getBlock();
			if (from == block.getRank()) {
				if (!world.isClient) {
					FDDrawers.DrawerData drawerData = FDDrawers.DRAWER_DATA.get(block.getWoodBaseId());
					DrawerBlock newBlock = drawerData.getBlocks().get(block.getDrawerType()).get(to);

					context.getStack().decrement(1);

					world.setBlockState(
							pos, newBlock.getDefaultState().with(DrawerBlock.FACING, blockState.get(DrawerBlock.FACING))
					);
					BlockEntity blockEntity = world.getBlockEntity(pos);
					if (blockEntity instanceof DrawerBlockEntity) {
						((DrawerBlockEntity) blockEntity).getStorage().setEntryCapacity(block.getEntryCapacity());
					}
				}
				return ActionResult.SUCCESS;
			}
		}
		return super.useOnBlock(context);
	}
}
