package com.simibubi.create.content.contraptions.components.actors;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class DrillMovementBehaviour extends BlockBreakingMovementBehaviour {

	@Override
	public boolean isActive(MovementContext context) {
		return !VecHelper.isVecPointingTowards(context.relativeMotion, context.state.get(DrillBlock.FACING)
			.getOpposite());
	}

	@Override
	public Vector3d getActiveAreaOffset(MovementContext context) {
		return Vector3d.of(context.state.get(DrillBlock.FACING)
			.getDirectionVec()).scale(.65f);
	}

	@Override
	@OnlyIn(value = Dist.CLIENT)
	public void renderInContraption(MovementContext context, MatrixStack ms, MatrixStack msLocal,
		IRenderTypeBuffer buffer) {
		DrillRenderer.renderInContraption(context, ms, msLocal, buffer);
	}

	@Override
	protected DamageSource getDamageSource() {
		return DrillBlock.damageSourceDrill;
	}

	@Override
	public boolean canBreak(World world, BlockPos breakingPos, BlockState state) {
		return super.canBreak(world, breakingPos, state) && !state.getCollisionShape(world, breakingPos)
			.isEmpty();
	}

}
