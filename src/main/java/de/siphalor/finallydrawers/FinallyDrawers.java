package de.siphalor.finallydrawers;

import de.siphalor.finallydrawers.block.DrawerBlock;
import de.siphalor.finallydrawers.block.DrawerBlockEntity;
import de.siphalor.finallydrawers.storage.DrawerRank;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FinallyDrawers implements ModInitializer {

	public static Logger LOGGER = LogManager.getLogger();

	public static final String MOD_ID = "finally_drawers";
	public static final String MOD_NAME = "Finally drawers";

	public static final Identifier HIT_DRAWER_PACKET_C2S_ID = new Identifier(MOD_ID, "hit_drawer");
	public static final Identifier DRAWER_UPDATE_PACKET_S2C_ID = new Identifier(MOD_ID, "drawer_update");

	public static final ItemGroup ITEM_GROUP = FabricItemGroupBuilder.create(new Identifier(MOD_ID, "general")).build();
	public static final Set<Block> DRAWER_BLOCKS = new HashSet<>();
	public static final Map<Identifier, DrawerRank> DRAWER_RANKS = new HashMap<>();
	public static BlockEntityType<DrawerBlockEntity> DRAWER_BLOCK_ENTITY_TYPE;

	@Override
	public void onInitialize() {
		log(Level.INFO, "Initializing Drawers");

		// BLOCKS AND ITEMS AND THINGS
		DRAWER_BLOCK_ENTITY_TYPE = register(
				new BlockEntityType<>(DrawerBlockEntity::new, DRAWER_BLOCKS, null),
				"drawer"
		);
		////noinspection ConstantConditions
		//((BlockEntityTypeAccessor) DRAWER_BLOCK_ENTITY_TYPE).setBlocks(new HashSet<>(((BlockEntityTypeAccessor) DRAWER_BLOCK_ENTITY_TYPE).getBlocks())); // makes the BE block list editable
		// EVENTS
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

			if (player.isSpectator()) return ActionResult.PASS;
			BlockState blockState = world.getBlockState(hitResult.getBlockPos());
			if (blockState.getBlock() instanceof DrawerBlock) {
				return blockState.getBlock().onUse(blockState, world, hitResult.getBlockPos(), player, hand, hitResult);
			}
			return ActionResult.PASS;
		});

		// NETWORKING
		ServerPlayNetworking.registerGlobalReceiver(HIT_DRAWER_PACKET_C2S_ID, (server, player, handler, buf, responseSender) -> {
			BlockPos blockPos = buf.readBlockPos();
			if (!player.world.canPlayerModifyAt(player, blockPos)) return;
			if (!(player.squaredDistanceTo(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D) < 64D)) return;
			int entry = buf.readByte();

			server.execute(() -> {
				BlockEntity blockEntity = player.world.getBlockEntity(blockPos);
				if (blockEntity instanceof DrawerBlockEntity) {
					((DrawerBlockEntity) blockEntity).onAttack(blockPos, player, entry);
				}
			});
		});
	}

	public static <T extends Item> T register(T item, String id) {
		return register(Registry.ITEM, item, id);
	}

	public static <T extends Block> T register(T block, String id) {
		return register(Registry.BLOCK, block, id);
	}

	public  static <BE extends BlockEntity, T extends BlockEntityType<BE>> T register(T beType, String id) {
		return register(Registry.BLOCK_ENTITY_TYPE, beType, id);
	}

	public static <T extends B, B> T register(Registry<B> registry, T value, String id) {
		Registry.register(registry, new Identifier(MOD_ID, id), value);
		return value;
	}

	public static void log(Level level, String message) {
		LOGGER.log(level, "[" + MOD_NAME + "] " + message);
	}

}
