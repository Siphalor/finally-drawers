package de.siphalor.finallydrawers.storage;

import de.siphalor.finallydrawers.FinallyDrawers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

public class DrawerStorage implements Inventory {
	private final Entry[] entries;
	/**
	 * Base capacity measured in stacks per entry
	 */
	private final int baseCapacity;
	private Consumer<Collection<Entry>> changeListener;

	public DrawerStorage(int size, int baseCapacity) {
		entries = new Entry[size];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = new Entry(i);
		}
		this.baseCapacity = baseCapacity;
	}

	@Nullable
	public Entry getEntry(int index) {
		if (index < 0 || index >= entries.length) {
			return null;
		}
		return entries[index];
	}

	public int getEntryCapacity() {
		return baseCapacity;
	}

	public void setChangeListener(Consumer<Collection<Entry>> changeListener) {
		this.changeListener = changeListener;
	}

	protected void onChanged(Collection<Entry> entries) {
		if (changeListener != null) {
			changeListener.accept(entries);
		}
	}

	@Override
	public int size() {
		return entries.length * getEntryCapacity();
	}

	@Override
	public boolean isEmpty() {
		for (Entry entry : entries) {
			if (!entry.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public ItemStack getStackInEntry(int slot, int entry) {
		if (entry < 0 || entry > entries.length) {
			return ItemStack.EMPTY;
		}
		if (entries[entry].isEmpty()) {
			return ItemStack.EMPTY;
		}
		slot %= getEntryCapacity(); // this is the slot position inside the entry
		slot -= entries[entry].getFullness();

		if (slot > 0) { // we're in empty space
			return ItemStack.EMPTY;
		} else {
			ItemStack stack = entries[entry].getReference().copy();
			if (slot < 0) { // we're in completely filled space
				stack.setCount(entries[entry].getStackMaxCount());
			} else { // we're on the partially filled stack
				int count = entries[entry].getAmount() % entries[entry].getStackMaxCount();
				stack.setCount(count == 0 ? entries[entry].getStackMaxCount() : count);
			}
			return stack;
		}
	}

	@Override
	public ItemStack getStack(int slot) {
		return getStackInEntry(slot, slot / getEntryCapacity());
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		int i = slot / getEntryCapacity();
		if (i < 0 || i > entries.length) {
			return ItemStack.EMPTY;
		}
		if (entries[i].isEmpty()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = entries[i].getReference().copy();
		if (amount <= entries[i].getAmount()) {
			stack.setCount(amount);
			entries[i].setAmount(entries[i].getAmount() - amount);
		} else { // not enough storage to take out
			stack.setCount(entries[i].getAmount());
			entries[i].setAmount(0);
		}
		onChanged(Collections.singleton(entries[i]));
		return stack;
	}

	@Override
	public ItemStack removeStack(int slot) {
		int entry = slot / getEntryCapacity();

		ItemStack stack = getStackInEntry(slot, entry);
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		entries[entry].setAmount(entries[entry].getAmount() - stack.getCount());
		onChanged(Collections.singleton(entries[entry]));
		return stack;
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		int entry = slot / getEntryCapacity();

		if (!ScreenHandler.canStacksCombine(stack, entries[entry].getReference())) {
			return;
		}

		ItemStack stackInEntry = getStackInEntry(slot, entry);
		entries[entry].setAmount(entries[entry].getAmount() + (stack.getCount() - stackInEntry.getCount()));
		onChanged(Collections.singleton(entries[entry]));
	}

	@Override
	public void markDirty() {

	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return true;
	}

	@Override
	public void clear() {
		for (Entry entry : entries) {
			entry.setAmount(0);
		}
		onChanged(Arrays.asList(entries));
	}

	public NbtCompound toTag(NbtCompound compound) {
		NbtList entryList = new NbtList();
		for (Entry entry : entries) {
			entryList.add(entry.toTag());
		}
		compound.put("entries", entryList);
		return compound;
	}

	public void fromTag(NbtCompound compound) {
		if (!compound.contains("entries")) return;
		int i = 0;
		for (NbtElement nbtElement : compound.getList("entries", 10)) {
			if (i >= entries.length) {
				FinallyDrawers.LOGGER.error(
						"Too many entries in drawer found (allowed: {})! The surplus will be deleted! Dump: {}",
						entries.length, compound.toString()
				);
				break;
			}
			if (nbtElement instanceof NbtCompound) {
				entries[i].fromTag(((NbtCompound) nbtElement));
				i++;
			} else {
				FinallyDrawers.LOGGER.error(
						"Found entry of invalid type in drawer! The entry will be deleted! Dump: {}",
						nbtElement.toString()
				);
			}
		}
	}

	public class Entry {
		private final int pos;
		private ItemStack reference;
		private int stackMaxCount;
		private int amount;

		public Entry(int pos) {
			this.pos = pos;
			this.reference = ItemStack.EMPTY;
			stackMaxCount = 0;
		}

		public int getPos() {
			return pos;
		}

		public boolean isEmpty() {
			return amount == 0 || reference.isEmpty();
		}

		public int getStackCapacity() {
			return getEntryCapacity();
		}

		public int getItemCapacity() {
			return getEntryCapacity() * stackMaxCount;
		}

		public int getFullness() {
			if (stackMaxCount == 0) {
				return 0;
			}
			return (amount - 1) / stackMaxCount;
		}

		public int getStackMaxCount() {
			return stackMaxCount;
		}

		public int getAmount() {
			return amount;
		}

		public void set(ItemStack reference, int amount) {
			setReference(reference);
			setAmount(amount);
			onChanged(Collections.singleton(this));
		}

		protected void setAmount(int amount) {
			this.amount = amount;
			if (amount == 0) {
				setReference(ItemStack.EMPTY);
			}
		}

		public ItemStack getReference() {
			return reference;
		}

		protected void setReference(ItemStack reference) {
			this.reference = reference;
			if (reference.isEmpty()) {
				stackMaxCount = 0;
			} else {
				stackMaxCount = reference.getMaxCount();
			}
		}

		public boolean insert(ItemStack stack) {
			if (reference.isEmpty()) {
				setReference(stack.copy());
				setAmount(Math.min(stack.getCount(), getItemCapacity()));
				stack.setCount(stack.getCount() - amount);
				onChanged(Collections.singleton(this));
				return true;
			}

			if (!ScreenHandler.canStacksCombine(stack, reference)) {
				return false;
			}

			int fill = Math.min(stack.getCount(), getItemCapacity() - amount);
			setAmount(amount + fill);
			stack.setCount(stack.getCount() - fill);
			onChanged(Collections.singleton(this));
			return fill != 0;
		}

		public boolean canInsertSingle(ItemStack stack) {
			if (stack.isEmpty()) return false;
			if (reference.isEmpty()) return true;
			return amount < getItemCapacity() && ScreenHandler.canStacksCombine(stack, reference);
		}

		public boolean insertSingle(ItemStack stack) {
			if (stack.isEmpty()) return true;
			if (reference.isEmpty()) {
				setReference(stack.copy());
				setAmount(1);
				stack.setCount(stack.getCount() - 1);
				onChanged(Collections.singleton(this));
				return true;
			}
			if (amount < getItemCapacity()) {
				if (!ScreenHandler.canStacksCombine(stack, reference)) {
					return false;
				}
				setAmount(amount + 1);
				stack.setCount(stack.getCount() - 1);
				onChanged(Collections.singleton(this));
				return true;
			}
			return false;
		}

		public ItemStack take(int amount) {
			ItemStack stack = reference.copy();
			stack.setCount(Math.min(this.amount, amount));
			setAmount(this.amount - stack.getCount());
			onChanged(Collections.singleton(this));
			return stack;
		}

		public NbtCompound toTag() {
			NbtCompound compound = new NbtCompound();
			NbtCompound referenceCompound = new NbtCompound();
			reference.writeNbt(referenceCompound);
			compound.put("reference", referenceCompound);
			compound.putInt("amount", amount);
			return compound;
		}

		public void fromTag(NbtCompound compound) {
			setReference(ItemStack.fromNbt(compound.getCompound("reference")));
			setAmount(compound.getInt("amount"));
		}

		public void write(PacketByteBuf buf) {
			buf.writeItemStack(reference);
			buf.writeVarInt(amount);
		}

		public void read(PacketByteBuf buf) {
			setReference(buf.readItemStack());
			setAmount(buf.readVarInt());
		}
	}
}
