/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.ai.goal.Goal
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 */
package com.mimesis.ai;

import com.mimesis.entity.MimesisEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MimesisBlockBreakGoal
extends Goal {
    private final MimesisEntity mimesis;
    private BlockPos targetBlock = null;
    private int breakProgress = 0;
    private int scanTimer = 0;

    public MimesisBlockBreakGoal(MimesisEntity mimesis) {
        this.mimesis = mimesis;
    }

    public boolean m_8036_() {
        --this.scanTimer;
        if (this.scanTimer <= 0) {
            this.targetBlock = this.findNearestOre();
            this.scanTimer = 10;
            return this.targetBlock != null;
        }
        return this.targetBlock != null;
    }

    public boolean m_6767_() {
        return true;
    }

    public void m_8037_() {
        if (this.targetBlock == null) {
            return;
        }
        if (!this.canReachTargetBlock()) {
            this.m_8041_();
            return;
        }
        if (!this.hasLineOfSightTo(this.targetBlock)) {
            this.m_8041_();
            return;
        }
        double distSq = this.mimesis.m_20183_().m_123331_((Vec3i)this.targetBlock);
        if (distSq > 16.0) {
            this.mimesis.m_21573_().m_26519_((double)this.targetBlock.m_123341_() + 0.5, (double)this.targetBlock.m_123342_(), (double)this.targetBlock.m_123343_() + 0.5, 0.9);
            this.breakProgress = 0;
        } else if (distSq > 4.0) {
            this.mimesis.m_21573_().m_26519_((double)this.targetBlock.m_123341_() + 0.5, (double)this.targetBlock.m_123342_(), (double)this.targetBlock.m_123343_() + 0.5, 0.8);
            this.mimesis.m_21563_().m_24946_((double)this.targetBlock.m_123341_() + 0.5, (double)this.targetBlock.m_123342_() + 0.5, (double)this.targetBlock.m_123343_() + 0.5);
            this.breakProgress = 0;
        } else {
            ServerLevel serverLevel;
            this.mimesis.m_21573_().m_26573_();
            this.mimesis.m_6858_(false);
            this.mimesis.m_20334_(0.0, this.mimesis.m_20184_().f_82480_, 0.0);
            double blockDist = this.mimesis.m_20238_(Vec3.m_82512_((Vec3i)this.targetBlock));
            if (blockDist > 16.0) {
                this.m_8041_();
                return;
            }
            this.mimesis.m_21563_().m_24946_((double)this.targetBlock.m_123341_() + 0.5, (double)this.targetBlock.m_123342_() + 0.5, (double)this.targetBlock.m_123343_() + 0.5);
            this.equipToolForBlock(this.targetBlock);
            Level level = this.mimesis.m_9236_();
            if (level instanceof ServerLevel) {
                serverLevel = (ServerLevel)level;
                serverLevel.m_6801_(this.mimesis.m_19879_(), this.targetBlock, Math.min(9, this.breakProgress / 2));
            }
            this.mimesis.m_6674_(InteractionHand.MAIN_HAND);
            if (!this.mimesis.m_9236_().f_46443_) {
                this.mimesis.m_9236_().m_7605_((Entity)this.mimesis, (byte)4);
            }
            ++this.breakProgress;
            if (this.breakProgress >= 12) {
                if (this.mimesis.m_9236_() != null && !this.mimesis.m_9236_().f_46443_ && (level = this.mimesis.m_9236_()) instanceof ServerLevel) {
                    serverLevel = (ServerLevel)level;
                    serverLevel.m_46961_(this.targetBlock, true);
                    AABB box = new AABB((double)this.targetBlock.m_123341_(), (double)this.targetBlock.m_123342_(), (double)this.targetBlock.m_123343_(), (double)(this.targetBlock.m_123341_() + 1), (double)(this.targetBlock.m_123342_() + 1), (double)(this.targetBlock.m_123343_() + 1)).m_82400_(1.5);
                    for (ItemEntity item : serverLevel.m_45976_(ItemEntity.class, box)) {
                        if (item == null || item.m_213877_()) continue;
                        ItemStack stack = item.m_32055_();
                        if (!stack.m_41619_()) {
                            this.mimesis.addToCarriedItems(stack.m_41777_());
                        }
                        item.m_142687_(Entity.RemovalReason.DISCARDED);
                    }
                    serverLevel.m_6801_(this.mimesis.m_19879_(), this.targetBlock, -1);
                }
                this.m_8041_();
            }
        }
    }

    private void equipToolForBlock(BlockPos blockPos) {
        Block block = this.mimesis.m_9236_().m_8055_(blockPos).m_60734_();
        ItemStack mainHand = this.mimesis.m_6844_(EquipmentSlot.MAINHAND);
        if (this.isRightToolForBlock(mainHand, block)) {
            return;
        }
        mainHand = this.isOre(block) ? new ItemStack((ItemLike)Items.f_42427_) : (this.isLog(block) ? new ItemStack((ItemLike)Items.f_42428_) : new ItemStack((ItemLike)Items.f_42427_));
        this.mimesis.m_8061_(EquipmentSlot.MAINHAND, mainHand);
    }

    private boolean isRightToolForBlock(ItemStack itemStack, Block block) {
        if (itemStack.m_41619_()) {
            return false;
        }
        if (this.isOre(block)) {
            return itemStack.m_41720_().toString().toLowerCase().contains("pickaxe");
        }
        if (this.isLog(block)) {
            return itemStack.m_41720_().toString().toLowerCase().contains("axe");
        }
        return false;
    }

    public void m_8041_() {
        Level level;
        if (this.targetBlock != null && (level = this.mimesis.m_9236_()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            serverLevel.m_6801_(this.mimesis.m_19879_(), this.targetBlock, -1);
        }
        this.targetBlock = null;
        this.breakProgress = 0;
        this.mimesis.clearHeldItems();
    }

    private BlockPos findNearestOre() {
        BlockPos center = this.mimesis.m_20183_();
        BlockPos nearest = null;
        double nearestDist = 64.0;
        for (int x = -8; x <= 8; ++x) {
            for (int y = -3; y <= 3; ++y) {
                for (int z = -8; z <= 8; ++z) {
                    double dist;
                    BlockPos checkPos = center.m_7918_(x, y, z);
                    Block block = this.mimesis.m_9236_().m_8055_(checkPos).m_60734_();
                    if (!this.isOre(block) || !this.hasLineOfSightTo(checkPos) || !((dist = center.m_123331_((Vec3i)checkPos)) < nearestDist)) continue;
                    nearestDist = dist;
                    nearest = checkPos;
                }
            }
        }
        return nearest;
    }

    private boolean canReachTargetBlock() {
        if (this.targetBlock == null) {
            return false;
        }
        return this.mimesis.m_21573_().m_7864_(this.targetBlock, 0) != null;
    }

    private boolean hasLineOfSightTo(BlockPos blockPos) {
        Vec3 start = this.mimesis.m_146892_();
        Vec3 end = Vec3.m_82512_((Vec3i)blockPos);
        BlockHitResult hit = this.mimesis.m_9236_().m_45547_(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)this.mimesis));
        return hit.m_6662_() == HitResult.Type.MISS || hit.m_82425_().equals((Object)blockPos);
    }

    private boolean isValuableBlock(Block block) {
        return this.isOre(block);
    }

    private boolean isOre(Block block) {
        return block == Blocks.f_49997_ || block == Blocks.f_152469_ || block == Blocks.f_49996_ || block == Blocks.f_152468_ || block == Blocks.f_152505_ || block == Blocks.f_152506_ || block == Blocks.f_49995_ || block == Blocks.f_152467_ || block == Blocks.f_50089_ || block == Blocks.f_152474_ || block == Blocks.f_50264_ || block == Blocks.f_152479_ || block == Blocks.f_50059_ || block == Blocks.f_152472_;
    }

    private boolean isLog(Block block) {
        return block == Blocks.f_49999_ || block == Blocks.f_50001_ || block == Blocks.f_50000_ || block == Blocks.f_50002_ || block == Blocks.f_50004_ || block == Blocks.f_50003_ || block == Blocks.f_220832_;
    }
}

