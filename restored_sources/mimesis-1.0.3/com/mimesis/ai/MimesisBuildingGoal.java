/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.ai.goal.Goal
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 */
package com.mimesis.ai;

import com.mimesis.entity.MimesisEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class MimesisBuildingGoal
extends Goal {
    private final MimesisEntity mimesis;
    private int blockedTimer = 0;
    private int buildTimer = 0;
    private BlockPos buildTarget = null;

    public MimesisBuildingGoal(MimesisEntity mimesis) {
        this.mimesis = mimesis;
    }

    public boolean m_8036_() {
        Player target = this.mimesis.m_9236_().m_45924_(this.mimesis.m_20185_(), this.mimesis.m_20186_(), this.mimesis.m_20189_(), 64.0, false);
        if (target == null) {
            return false;
        }
        if (this.blockedTimer > 40) {
            return this.hasCobblestone();
        }
        return false;
    }

    public boolean m_6767_() {
        return true;
    }

    public void m_8037_() {
        Player target = this.mimesis.m_9236_().m_45924_(this.mimesis.m_20185_(), this.mimesis.m_20186_(), this.mimesis.m_20189_(), 64.0, false);
        if (target == null) {
            this.m_8041_();
            return;
        }
        double movementSpeed = this.mimesis.m_20184_().m_82556_();
        this.blockedTimer = movementSpeed < 0.01 ? ++this.blockedTimer : 0;
        --this.buildTimer;
        if (this.buildTimer <= 0) {
            this.buildBlockToReachPlayer(target);
            this.buildTimer = 20;
        }
        if (this.blockedTimer > 200) {
            this.m_8041_();
        }
    }

    private void buildBlockToReachPlayer(Player target) {
        if (!this.hasCobblestone()) {
            this.m_8041_();
            return;
        }
        ItemStack cobblestone = new ItemStack((ItemLike)Items.f_42594_);
        this.mimesis.m_8061_(EquipmentSlot.MAINHAND, cobblestone);
        BlockPos basePos = this.mimesis.m_20183_();
        BlockPos buildPos = null;
        if (target.m_20186_() > this.mimesis.m_20186_() + 1.0) {
            buildPos = basePos.m_7494_();
            if (!this.canPlaceBlock(buildPos)) {
                buildPos = basePos.m_7494_().m_7918_(1, 0, 0);
            }
        } else {
            BlockPos[] candidates = new BlockPos[]{basePos.m_122012_(), basePos.m_122019_(), basePos.m_122029_(), basePos.m_122024_(), basePos.m_122012_().m_7494_(), basePos.m_122019_().m_7494_(), basePos.m_122029_().m_7494_(), basePos.m_122024_().m_7494_()};
            double bestDist = Double.MAX_VALUE;
            for (BlockPos candidate : candidates) {
                double d;
                if (!this.canPlaceBlock(candidate) || !((d = target.m_20275_((double)candidate.m_123341_() + 0.5, (double)candidate.m_123342_() + 0.5, (double)candidate.m_123343_() + 0.5)) < bestDist)) continue;
                bestDist = d;
                buildPos = candidate;
            }
        }
        if (buildPos != null) {
            this.mimesis.m_9236_().m_7731_(buildPos, Blocks.f_50652_.m_49966_(), 3);
        }
    }

    private boolean canPlaceBlock(BlockPos pos) {
        Block block = this.mimesis.m_9236_().m_8055_(pos).m_60734_();
        return block == Blocks.f_50016_ || block == Blocks.f_50359_ || block == Blocks.f_50034_ || block == Blocks.f_50037_ || block == Blocks.f_50036_ || block == Blocks.f_50111_ || block == Blocks.f_50112_;
    }

    private boolean hasCobblestone() {
        ItemStack mainHand = this.mimesis.m_6844_(EquipmentSlot.MAINHAND);
        if (mainHand.m_41720_() == Items.f_42594_ && mainHand.m_41613_() > 0) {
            return true;
        }
        ItemStack offHand = this.mimesis.m_6844_(EquipmentSlot.OFFHAND);
        return offHand.m_41720_() == Items.f_42594_ && offHand.m_41613_() > 0;
    }

    public void m_8041_() {
        this.blockedTimer = 0;
        this.buildTimer = 0;
        this.buildTarget = null;
        this.mimesis.clearHeldItems();
    }
}

