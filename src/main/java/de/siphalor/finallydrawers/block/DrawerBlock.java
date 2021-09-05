package de.siphalor.finallydrawers.block;

import de.siphalor.finallydrawers.storage.DrawerRank;
import de.siphalor.finallydrawers.util.ClientProxy;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class DrawerBlock extends BlockWithEntity {
	public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

	private final int width;
	private final int height;
	private final DrawerRank rank;
	private final int capacity;

	public DrawerBlock(Settings settings, int width, int height, DrawerRank rank, int capacity) {
		super(settings);
		this.width = width;
		this.height = height;
		this.rank = rank;
		this.capacity = capacity;
	}

	public int getSlotGridWidth() {
		return width;
	}

	public int getSlotGridHeight() {
		return height;
	}

	public DrawerRank getRank() {
		return rank;
	}

	public int getEntryCapacity() {
		return capacity / (width * height);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		super.appendProperties(builder);
	}

	@Nullable
	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getPlayerFacing().getOpposite());
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockView world) {
		return new DrawerBlockEntity(width, height, getEntryCapacity());
	}

	public static DrawerBlockEntity getBlockEntity(World world, BlockPos pos) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity instanceof DrawerBlockEntity) {
			return (DrawerBlockEntity) blockEntity;
		}
		throw new IllegalStateException("Invalid block entity at " + pos + ", expected DrawerBlockEntity!");
	}

	@SuppressWarnings("deprecation")
	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		return getBlockEntity(world, pos).onUse(pos, state, player, hand, hit);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		// TODO send packet
		if (world.isClient) {
			HitResult hitResult = ClientProxy.getCrosshairTarget();
			if (hitResult instanceof BlockHitResult) {
				getBlockEntity(world, pos).onClientAttack(pos, state, (BlockHitResult) hitResult);
			}
		} else {
			super.onBlockBreakStart(state, world, pos, player);
		}
	}

	@Override
	public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient && !player.isCreative()) {
			ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(this));
		}
		super.onBreak(world, pos, state, player);
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock())) {
			DrawerBlockEntity blockEntity = getBlockEntity(world, pos);
			ItemScatterer.spawn(world, pos, blockEntity);
			world.updateComparators(pos, this);

			super.onStateReplaced(state, world, pos, newState, moved);
		}
	}

	@SuppressWarnings("deprecation")
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@SuppressWarnings("deprecation")
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}
}
