package com.simibubi.create.foundation.tileEntity.behaviour.linked;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.RedstoneLinkNetworkHandler;
import com.simibubi.create.content.logistics.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.tileEntity.behaviour.ValueBoxTransform;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.vector.Vector3d;

public class LinkBehaviour extends TileEntityBehaviour {

	public static BehaviourType<LinkBehaviour> TYPE = new BehaviourType<>();

	enum Mode {
		TRANSMIT, RECEIVE
	}

	Frequency frequencyFirst;
	Frequency frequencyLast;
	ValueBoxTransform firstSlot;
	ValueBoxTransform secondSlot;
	Vector3d textShift;

	public boolean newPosition;
	private Mode mode;
	private Supplier<Integer> transmission;
	private Consumer<Integer> signalCallback;

	protected LinkBehaviour(SmartTileEntity te, Pair<ValueBoxTransform, ValueBoxTransform> slots) {
		super(te);
		frequencyFirst = new Frequency(ItemStack.EMPTY);
		frequencyLast = new Frequency(ItemStack.EMPTY);
		firstSlot = slots.getLeft();
		secondSlot = slots.getRight();
		textShift = Vector3d.ZERO;
		newPosition = true;
	}

	public static LinkBehaviour receiver(SmartTileEntity te, Pair<ValueBoxTransform, ValueBoxTransform> slots,
		Consumer<Integer> signalCallback) {
		LinkBehaviour behaviour = new LinkBehaviour(te, slots);
		behaviour.signalCallback = signalCallback;
		behaviour.mode = Mode.RECEIVE;
		return behaviour;
	}

	public static LinkBehaviour transmitter(SmartTileEntity te, Pair<ValueBoxTransform, ValueBoxTransform> slots,
		Supplier<Integer> transmission) {
		LinkBehaviour behaviour = new LinkBehaviour(te, slots);
		behaviour.transmission = transmission;
		behaviour.mode = Mode.TRANSMIT;
		return behaviour;
	}

	public LinkBehaviour moveText(Vector3d shift) {
		textShift = shift;
		return this;
	}

	public void copyItemsFrom(LinkBehaviour behaviour) {
		if (behaviour == null)
			return;
		frequencyFirst = behaviour.frequencyFirst;
		frequencyLast = behaviour.frequencyLast;
	}

	public boolean isListening() {
		return mode == Mode.RECEIVE;
	}

	public int getTransmittedStrength() {
		return mode == Mode.TRANSMIT ? transmission.get() : 0;
	}

	public void updateReceiver(int networkPower) {
		if (!newPosition)
			return;
		signalCallback.accept(networkPower);
	}

	public void notifySignalChange() {
		Create.redstoneLinkNetworkHandler.updateNetworkOf(this);
	}

	@Override
	public void initialize() {
		super.initialize();
		if (tileEntity.getWorld().isRemote)
			return;
		getHandler().addToNetwork(this);
		newPosition = true;
	}

	public Pair<Frequency, Frequency> getNetworkKey() {
		return Pair.of(frequencyFirst, frequencyLast);
	}

	@Override
	public void remove() {
		super.remove();
		if (tileEntity.getWorld().isRemote)
			return;
		getHandler().removeFromNetwork(this);
	}

	@Override
	public void write(CompoundNBT nbt, boolean clientPacket) {
		super.write(nbt, clientPacket);
		nbt.put("FrequencyFirst", frequencyFirst.getStack()
			.write(new CompoundNBT()));
		nbt.put("FrequencyLast", frequencyLast.getStack()
			.write(new CompoundNBT()));
		nbt.putLong("LastKnownPosition", tileEntity.getPos()
			.toLong());
	}

	@Override
	public void read(CompoundNBT nbt, boolean clientPacket) {
		long positionInTag = tileEntity.getPos()
			.toLong();
		long positionKey = nbt.getLong("LastKnownPosition");
		newPosition = positionInTag != positionKey;

		super.read(nbt, clientPacket);
		frequencyFirst = new Frequency(ItemStack.read(nbt.getCompound("FrequencyFirst")));
		frequencyLast = new Frequency(ItemStack.read(nbt.getCompound("FrequencyLast")));
	}

	public void setFrequency(boolean first, ItemStack stack) {
		stack = stack.copy();
		stack.setCount(1);
		ItemStack toCompare = first ? frequencyFirst.getStack() : frequencyLast.getStack();
		boolean changed =
			!ItemStack.areItemsEqual(stack, toCompare) || !ItemStack.areItemStackTagsEqual(stack, toCompare);

		if (changed)
			getHandler().removeFromNetwork(this);

		if (first)
			frequencyFirst = new Frequency(stack);
		else
			frequencyLast = new Frequency(stack);

		if (!changed)
			return;

		tileEntity.sendData();
		getHandler().addToNetwork(this);
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}

	private RedstoneLinkNetworkHandler getHandler() {
		return Create.redstoneLinkNetworkHandler;
	}

	public static class SlotPositioning {
		Function<BlockState, Pair<Vector3d, Vector3d>> offsets;
		Function<BlockState, Vector3d> rotation;
		float scale;

		public SlotPositioning(Function<BlockState, Pair<Vector3d, Vector3d>> offsetsForState,
			Function<BlockState, Vector3d> rotationForState) {
			offsets = offsetsForState;
			rotation = rotationForState;
			scale = 1;
		}

		public SlotPositioning scale(float scale) {
			this.scale = scale;
			return this;
		}

	}

	public boolean testHit(Boolean first, Vector3d hit) {
		BlockState state = tileEntity.getBlockState();
		Vector3d localHit = hit.subtract(Vector3d.of(tileEntity.getPos()));
		return (first ? firstSlot : secondSlot).testHit(state, localHit);
	}

}
