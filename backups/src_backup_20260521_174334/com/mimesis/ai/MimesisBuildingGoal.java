package com.mimesis.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.entity.player.Player;

import com.mimesis.entity.MimesisEntity;

/**
 * Builds cobblestone structures when blocked to reach the player
 */
public class MimesisBuildingGoal extends Goal {
    private final MimesisEntity mimesis;
    private int blockedTimer = 0;
    private int buildTimer = 0;
    private BlockPos buildTarget = null;

    public MimesisBuildingGoal(MimesisEntity mimesis) {
        this.mimesis = mimesis;
    }

    @Override
    public boolean canUse() {
        // Check if entity is stuck (not moving but trying to move)
        Player target = this.mimesis.level().getNearestPlayer(
            this.mimesis.getX(),
            this.mimesis.getY(),
            this.mimesis.getZ(),
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
        Player target = this.mimesis.level().getNearestPlayer(
            this.mimesis.getX(),
            this.mimesis.getY(),
            this.mimesis.getZ(),
            64.0,
            false);

        if (target == null) {
            this.stop();
            return;
        }

        // Check if moving
        double movementSpeed = this.mimesis.getDeltaMovement().lengthSqr();
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
        this.mimesis.setItemSlot(EquipmentSlot.MAINHAND, cobblestone);

        // Try to place block in a cardinal direction only; never diagonal.
        BlockPos basePos = this.mimesis.blockPosition();
        BlockPos buildPos = null;

        // Priority 1: Build up if player is higher
        if (target.getY() > this.mimesis.getY() + 1) {
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
            this.mimesis.level().setBlock(buildPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
        }
    }

    private boolean canPlaceBlock(BlockPos pos) {
        // Check if we can place a block here (only in air or plants)
        net.minecraft.world.level.block.Block block = this.mimesis.level().getBlockState(pos).getBlock();
        return block == Blocks.AIR || block == Blocks.TALL_GRASS || block == Blocks.GRASS || 
               block == Blocks.SEAGRASS || block == Blocks.DEAD_BUSH || block == Blocks.DANDELION ||
               block == Blocks.POPPY;
    }

    private boolean hasCobblestone() {
        // Check main hand
        ItemStack mainHand = this.mimesis.getItemBySlot(EquipmentSlot.MAINHAND);
        if (mainHand.getItem() == Items.COBBLESTONE && mainHand.getCount() > 0) {
            return true;
        }

        // Check offhand
        ItemStack offHand = this.mimesis.getItemBySlot(EquipmentSlot.OFFHAND);
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
        this.mimesis.clearHeldItems();
    }
}
