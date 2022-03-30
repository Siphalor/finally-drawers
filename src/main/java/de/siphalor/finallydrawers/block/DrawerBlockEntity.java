package de.siphalor.finallydrawers.block;

import de.siphalor.finallydrawers.FinallyDrawers;
import de.siphalor.finallydrawers.storage.DrawerStorage;
import de.siphalor.finallydrawers.util.ClientProxy;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DrawerBlockEntity extends LockableContainerBlockEntity {
	@NotNull
	private DrawerStorage storage;
	private long lastTrigger;
	private int gridWidth;
	private int gridHeight;

	public DrawerBlockEntity(BlockPos blockPos, BlockState blockState) {
		this(blockPos, blockState, 1, 1, 32);
	}

	public DrawerBlockEntity(BlockPos blockPos, BlockState blockState, int width, int height, int entryCapacity) {
		super(FinallyDrawers.DRAWER_BLOCK_ENTITY_TYPE, blockPos, blockState);
		gridWidth = width;
		gridHeight = height;
		this.storage = new DrawerStorage(width * height, entryCapacity);
	}

	@Override
	public void setWorld(World world) {
		super.setWorld(world);
		if (!world.isClient) {
			storage.setChangeListener(entries -> {
				markDirty();

				BlockEntityUpdateS2CPacket packet = BlockEntityUpdateS2CPacket.create(this);
				for (ServerPlayerEntity player : PlayerLookup.tracking(this)) {
					player.networkHandler.sendPacket(packet);
				}
			});
		}
	}

	public DrawerStorage getStorage() {
		return storage;
	}

	@Nullable
	public DrawerStorage.Entry getStorageEntry(Direction facing, Vec3d position) {
		position = position.subtract(0.5D, 0.5D, 0.5D).rotateY(facing.asRotation() / 180F * (float) Math.PI).add(0.5D, 0.5D, 0.5D);
		double y = 1 - position.getY();

		return storage.getEntry(gridWidth * (int) (gridHeight * y) + (int) (position.getX() * gridWidth));
	}

	public ActionResult onUse(BlockPos pos, BlockState state, PlayerEntity playerEntity, Hand hand, BlockHitResult hit) {
		Direction facing = state.get(DrawerBlock.FACING);
		if (hit.getSide() != facing) {
			return ActionResult.PASS;
		}

		ItemStack playerStack = playerEntity.getStackInHand(hand);
		DrawerStorage.Entry entry = getStorageEntry(facing, hit.getPos().subtract(pos.getX(), pos.getY(), pos.getZ()));
		if (entry == null) return ActionResult.PASS;
		//noinspection ConstantConditions
		if (world.isClient) {
			if (entry.canInsertSingle(playerStack)) {
				return ActionResult.SUCCESS; // TODO make fail configurable
			}
			long time = System.currentTimeMillis();
			if (time - lastTrigger < 1000) { // TODO make delay configurable
				// TODO request deposit
				lastTrigger = -1;
				return ActionResult.SUCCESS;
			} else {
				lastTrigger = time;
				return ActionResult.CONSUME;
			}
		} else {
			if (playerEntity.isSneaking()) {
				if (entry.insert(playerStack)) {
					return ActionResult.SUCCESS;
				}
				return ActionResult.FAIL;
			}

			if (entry.insertSingle(playerStack)) {
				return ActionResult.SUCCESS;
			}
			return ActionResult.FAIL;
		}
	}

	public void onClientAttack(BlockPos pos, BlockState state, BlockHitResult hit) {
		Direction facing = state.get(DrawerBlock.FACING);
		if (hit.getSide() != facing) {
			return;
		}

		DrawerStorage.Entry entry = getStorageEntry(facing, hit.getPos().subtract(pos.getX(), pos.getY(), pos.getZ()));
		if (entry == null) return;
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		buf.writeBlockPos(pos);
		buf.writeByte(entry.getPos());
		buf.writeEnumConstant(hit.getSide());
		ClientProxy.sendPacket(FinallyDrawers.HIT_DRAWER_PACKET_C2S_ID, buf);
	}

	public void onAttack(BlockPos pos, PlayerEntity player, int entryPos) {
		ItemStack dropStack = ItemStack.EMPTY;

		DrawerStorage.Entry entry = storage.getEntry(entryPos);
		if (entry == null) return;
		if (player.isSneaking()) {
			ItemStack stack = entry.take(entry.getStackMaxCount());
			if (!player.giveItemStack(stack)) {
				dropStack = stack;
			}
		} else {
			ItemStack stack = entry.take(1);
			if (!player.giveItemStack(stack)) {
				dropStack = stack;
			}
		}
		if (!dropStack.isEmpty()) {
			//noinspection IntegerDivisionInFloatingPointContext
			Vec3d itemPos = new Vec3d(entryPos / gridWidth + 0.5D, entryPos % gridWidth + 0.5D, 0.5D)
					.rotateY(getCachedState().get(DrawerBlock.FACING).asRotation());

			ItemEntity itemEntity = new ItemEntity(
					world, pos.getX() + itemPos.x, pos.getY() + itemPos.y, pos.getZ() + itemPos.z, dropStack
			);
			itemEntity.setVelocity(itemPos.subtract(0.5D, 0.5D, 0.5D).normalize().multiply(0.2D));
			world.spawnEntity(itemEntity);
		}
	}

	@Override
	protected Text getContainerName() {
		return new LiteralText("missingno");
	}

	@Override
	protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return null;
	}

	@Override
	public int size() {
		return storage.size();
	}

	@Override
	public boolean isEmpty() {
		return storage.isEmpty();
	}

	@Override
	public ItemStack getStack(int slot) {
		return storage.getStack(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		return storage.removeStack(slot, amount);
	}

	@Override
	public ItemStack removeStack(int slot) {
		return storage.removeStack(slot);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		storage.setStack(slot, stack);
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return !player.isSpectator() && checkUnlocked(player);
	}

	@Override
	public void clear() {
		storage.clear();
	}

	@Override
	public void readNbt(NbtCompound tag) {
		super.readNbt(tag);
		DrawerBlock drawerBlock = (DrawerBlock) getCachedState().getBlock();
		gridWidth = drawerBlock.getSlotGridWidth();
		gridHeight = drawerBlock.getSlotGridHeight();
		storage = new DrawerStorage(gridHeight * gridWidth, drawerBlock.getEntryCapacity());
		if (tag.contains("storage", 10)) {
			storage.fromTag(tag.getCompound("storage"));
		}
	}

	@Override
	public void writeNbt(NbtCompound nbt) {
		nbt.put("storage", storage.toTag(new NbtCompound()));
		super.writeNbt(nbt);
	}

	@Nullable
	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt() {
		return createNbt();
	}
}
