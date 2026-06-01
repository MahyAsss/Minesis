package com.mimesis.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import com.mimesis.entity.MimesisEntity;

/**
 * Actively breaks valuable ores (no wood) when nearby with proper tools
 */
public class MimesisBlockBreakGoal extends Goal {
    private final MimesisEntity mimesis;
    private BlockPos targetBlock = null;
    private int breakProgress = 0;
    private int scanTimer = 0;

    public MimesisBlockBreakGoal(MimesisEntity mimesis) {
        this.mimesis = mimesis;
    }

    @Override
    public boolean canUse() {
        // Scan for blocks every 10 ticks
        this.scanTimer--;
        if (this.scanTimer <= 0) {
            this.targetBlock = this.findNearestOre();
            this.scanTimer = 10;
            return this.targetBlock != null;
        }
        return this.targetBlock != null;
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    @Override
    public void tick() {
        if (this.targetBlock == null) {
            return;
        }

        if (!this.canReachTargetBlock()) {
            this.stop();
            return;
        }

        if (!this.hasLineOfSightTo(this.targetBlock)) {
            this.stop();
            return;
        }

        double distSq = this.mimesis.blockPosition().distSqr(this.targetBlock);
        
        if (distSq > 16) {
            // Too far, move closer (4+ blocks away)
            this.mimesis.getNavigation().moveTo(
                this.targetBlock.getX() + 0.5,
                this.targetBlock.getY(),
                this.targetBlock.getZ() + 0.5,
                0.9
            );
            this.breakProgress = 0;
        } else if (distSq > 4) {
            // Moderately far (2+ blocks), move closer
            this.mimesis.getNavigation().moveTo(
                this.targetBlock.getX() + 0.5,
                this.targetBlock.getY(),
                this.targetBlock.getZ() + 0.5,
                0.8
            );
            
            // Look at block while approaching
            this.mimesis.getLookControl().setLookAt(
                this.targetBlock.getX() + 0.5,
                this.targetBlock.getY() + 0.5,
                this.targetBlock.getZ() + 0.5
            );
            this.breakProgress = 0;
        } else {
            // Close enough to break (within 2 blocks)
            this.mimesis.getNavigation().stop();
            this.mimesis.setSprinting(false);
            this.mimesis.setDeltaMovement(0, this.mimesis.getDeltaMovement().y, 0);
            
            // Simple distance check for direct line of sight
            double blockDist = this.mimesis.distanceToSqr(Vec3.atCenterOf(this.targetBlock));
            if (blockDist > 16.0D) {  // More than ~4 blocks away
                this.stop();
                return;
            }
            
            // Look directly at block
            this.mimesis.getLookControl().setLookAt(
                this.targetBlock.getX() + 0.5,
                this.targetBlock.getY() + 0.5,
                this.targetBlock.getZ() + 0.5
            );
            
            // Equip appropriate tool
            this.equipToolForBlock(this.targetBlock);

            if (this.mimesis.level() instanceof ServerLevel serverLevel) {
                serverLevel.destroyBlockProgress(this.mimesis.getId(), this.targetBlock, Math.min(9, this.breakProgress / 2));
            }

            this.mimesis.swing(InteractionHand.MAIN_HAND);
            if (!this.mimesis.level().isClientSide) {
                this.mimesis.level().broadcastEntityEvent(this.mimesis, (byte)4);
            }
            
            // Increment break progress
            this.breakProgress++;
            
            // Break after 12 ticks (0.6 seconds)
            if (this.breakProgress >= 12) {
                // Actually destroy the block
                if (this.mimesis.level() != null && !this.mimesis.level().isClientSide) {
                        // Destroy the block (creates item entities), then collect nearby item entities into carried inventory
                        if (this.mimesis.level() instanceof ServerLevel serverLevel) {
                            serverLevel.destroyBlock(this.targetBlock, true);

                            // collect item entities spawned by the block
                            AABB box = new AABB(this.targetBlock.getX(), this.targetBlock.getY(), this.targetBlock.getZ(), this.targetBlock.getX() + 1, this.targetBlock.getY() + 1, this.targetBlock.getZ() + 1).inflate(1.5D);
                            for (ItemEntity item : serverLevel.getEntitiesOfClass(ItemEntity.class, box)) {
                                if (item != null && !item.isRemoved()) {
                                    ItemStack stack = item.getItem();
                                    if (!stack.isEmpty()) {
                                        this.mimesis.addToCarriedItems(stack.copy());
                                    }
                                    item.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                                }
                            }

                            serverLevel.destroyBlockProgress(this.mimesis.getId(), this.targetBlock, -1);
                        }
                    }
                this.stop();
            }
        }
    }

    private void equipToolForBlock(BlockPos blockPos) {
        Block block = this.mimesis.level().getBlockState(blockPos).getBlock();
        ItemStack mainHand = this.mimesis.getItemBySlot(EquipmentSlot.MAINHAND);
        
        // Check if we already have the right tool
        if (this.isRightToolForBlock(mainHand, block)) {
            return; // Already equipped correctly
        }
        
        // Try to find and equip appropriate tool
        if (this.isOre(block)) {
            // Use pickaxe for ores
            mainHand = new ItemStack(Items.STONE_PICKAXE);
        } else if (this.isLog(block)) {
            // Use axe for logs
            mainHand = new ItemStack(Items.STONE_AXE);
        } else {
            // Use stone pickaxe as fallback
            mainHand = new ItemStack(Items.STONE_PICKAXE);
        }
        
        this.mimesis.setItemSlot(EquipmentSlot.MAINHAND, mainHand);
    }

    private boolean isRightToolForBlock(ItemStack itemStack, Block block) {
        if (itemStack.isEmpty()) {
            return false;
        }
        
        // Check if item is a pickaxe for ores
        if (this.isOre(block)) {
            return itemStack.getItem().toString().toLowerCase().contains("pickaxe");
        }
        
        // Check if item is an axe for logs
        if (this.isLog(block)) {
            return itemStack.getItem().toString().toLowerCase().contains("axe");
        }
        
        return false;
    }

    @Override
    public void stop() {
        if (this.targetBlock != null && this.mimesis.level() instanceof ServerLevel serverLevel) {
            serverLevel.destroyBlockProgress(this.mimesis.getId(), this.targetBlock, -1);
        }
        this.targetBlock = null;
        this.breakProgress = 0;
        this.mimesis.clearHeldItems();
    }

    private BlockPos findNearestOre() {
        BlockPos center = this.mimesis.blockPosition();
        BlockPos nearest = null;
        double nearestDist = 64; // 8 blocks max

        // Scan in a smaller box around the entity
        for (int x = -8; x <= 8; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -8; z <= 8; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    Block block = this.mimesis.level().getBlockState(checkPos).getBlock();
                    
                    if (this.isOre(block) && this.hasLineOfSightTo(checkPos)) {
                        double dist = center.distSqr(checkPos);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = checkPos;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    private boolean canReachTargetBlock() {
        if (this.targetBlock == null) {
            return false;
        }

        return this.mimesis.getNavigation().createPath(this.targetBlock, 0) != null;
    }

    private boolean hasLineOfSightTo(BlockPos blockPos) {
        Vec3 start = this.mimesis.getEyePosition();
        Vec3 end = Vec3.atCenterOf(blockPos);
        BlockHitResult hit = this.mimesis.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this.mimesis));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS || hit.getBlockPos().equals(blockPos);
    }

    private boolean isValuableBlock(Block block) {
        return this.isOre(block);
    }

    private boolean isOre(Block block) {
        return block == Blocks.COAL_ORE || 
               block == Blocks.DEEPSLATE_COAL_ORE ||
               block == Blocks.IRON_ORE ||
               block == Blocks.DEEPSLATE_IRON_ORE ||
               block == Blocks.COPPER_ORE ||
               block == Blocks.DEEPSLATE_COPPER_ORE ||
               block == Blocks.GOLD_ORE ||
               block == Blocks.DEEPSLATE_GOLD_ORE ||
               block == Blocks.DIAMOND_ORE ||
               block == Blocks.DEEPSLATE_DIAMOND_ORE ||
               block == Blocks.EMERALD_ORE ||
               block == Blocks.DEEPSLATE_EMERALD_ORE ||
               block == Blocks.LAPIS_ORE ||
               block == Blocks.DEEPSLATE_LAPIS_ORE;
    }

    private boolean isLog(Block block) {
        return block == Blocks.OAK_LOG ||
               block == Blocks.BIRCH_LOG ||
               block == Blocks.SPRUCE_LOG ||
               block == Blocks.JUNGLE_LOG ||
               block == Blocks.DARK_OAK_LOG ||
               block == Blocks.ACACIA_LOG ||
               block == Blocks.MANGROVE_LOG;
    }
}
