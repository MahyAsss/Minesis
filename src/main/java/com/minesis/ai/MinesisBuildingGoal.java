package com.minesis.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.entity.player.Player;

import com.minesis.entity.MinesisEntity;

/**
 * Builds cobblestone structures when blocked to reach the player
 */
public class MinesisBuildingGoal extends Goal {
    private final MinesisEntity minesis;
    private int blockedTimer = 0;
    private int buildTimer = 0;
    private BlockPos buildTarget = null;

    public MinesisBuildingGoal(MinesisEntity minesis) {
        this.minesis = minesis;
    }

    @Override
    public boolean canUse() {
        // Check if entity is stuck (not moving but trying to move)
        Player target = this.minesis.level().getNearestPlayer(
            this.minesis.getX(),
            this.minesis.getY(),
            this.minesis.getZ(),
            64.0,
            false);
        
        if (target == null) {
            return false;
        }

        // If blocked for several ticks and we have cobblestone, try to build
        if (this.blockedTimer > 40) {
            return this.hasCobblestone();
        }

        return false;
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    @Override
    public void tick() {
        Player target = this.minesis.level().getNearestPlayer(
            this.minesis.getX(),
            this.minesis.getY(),
            this.minesis.getZ(),
            64.0,
            false);

        if (target == null) {
            this.stop();
            return;
        }

        // Check if moving
        double movementSpeed = this.minesis.getDeltaMovement().lengthSqr();
        if (movementSpeed < 0.01) {
            this.blockedTimer++;
        } else {
            this.blockedTimer = 0;
        }

        // Build structure
        this.buildTimer--;
        if (this.buildTimer <= 0) {
            this.buildBlockToReachPlayer(target);
            this.buildTimer = 20; // Build every 20 ticks
        }

        // Stop after a while
        if (this.blockedTimer > 200) {
            this.stop();
        }
    }

    private void buildBlockToReachPlayer(Player target) {
        // Equip cobblestone
        if (!this.hasCobblestone()) {
            this.stop();
            return;
        }

        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE);
        this.minesis.setItemSlot(EquipmentSlot.MAINHAND, cobblestone);

        // Try to place block in a cardinal direction only; never diagonal.
        BlockPos basePos = this.minesis.blockPosition();
        BlockPos buildPos = null;

        // Priority 1: Build up if player is higher
        if (target.getY() > this.minesis.getY() + 1) {
            buildPos = basePos.above();
            if (!this.canPlaceBlock(buildPos)) {
                buildPos = basePos.above().offset(1, 0, 0);
            }
        }
        // Priority 2: Build forward in direction of player
        else {
            BlockPos[] candidates = new BlockPos[] {
                    basePos.north(),
                    basePos.south(),
                    basePos.east(),
                    basePos.west(),
                    basePos.north().above(),
                    basePos.south().above(),
                    basePos.east().above(),
                    basePos.west().above()
            };

            double bestDist = Double.MAX_VALUE;
            for (BlockPos candidate : candidates) {
                if (!this.canPlaceBlock(candidate)) {
                    continue;
                }

                double d = target.distanceToSqr(candidate.getX() + 0.5D, candidate.getY() + 0.5D, candidate.getZ() + 0.5D);
                if (d < bestDist) {
                    bestDist = d;
                    buildPos = candidate;
                }
            }
        }

        // Place the block
        if (buildPos != null) {
            this.minesis.level().setBlock(buildPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
        }
    }

    private boolean canPlaceBlock(BlockPos pos) {
        net.minecraft.world.level.block.Block block = this.minesis.level().getBlockState(pos).getBlock();
        boolean spaceOk = block == Blocks.AIR || block == Blocks.TALL_GRASS || block == Blocks.GRASS ||
               block == Blocks.SEAGRASS || block == Blocks.DEAD_BUSH || block == Blocks.DANDELION ||
               block == Blocks.POPPY;
        if (!spaceOk) return false;
        net.minecraft.world.level.block.state.BlockState below = this.minesis.level().getBlockState(pos.below());
        return below.canOcclude() && below.getFluidState().isEmpty();
    }

    private boolean hasCobblestone() {
        // Check main hand
        ItemStack mainHand = this.minesis.getItemBySlot(EquipmentSlot.MAINHAND);
        if (mainHand.getItem() == Items.COBBLESTONE && mainHand.getCount() > 0) {
            return true;
        }

        // Check offhand
        ItemStack offHand = this.minesis.getItemBySlot(EquipmentSlot.OFFHAND);
        if (offHand.getItem() == Items.COBBLESTONE && offHand.getCount() > 0) {
            return true;
        }

        return false;
    }

    @Override
    public void stop() {
        this.blockedTimer = 0;
        this.buildTimer = 0;
        this.buildTarget = null;
        this.minesis.clearHeldItems();
    }
}
