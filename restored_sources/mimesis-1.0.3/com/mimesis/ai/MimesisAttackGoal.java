/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.ai.goal.Goal
 *  net.minecraft.world.entity.ai.goal.Goal$Flag
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 */
package com.mimesis.ai;

import com.mimesis.entity.MimesisEntity;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MimesisAttackGoal
extends Goal {
    private final MimesisEntity mimesis;
    private int attackCooldown = 0;
    private BlockPos chaseBreakPos = null;
    private int chaseBreakProgress = 0;
    private int chaseBreakNeededTicks = 0;
    private int blockedChaseTicks = 0;

    public MimesisAttackGoal(MimesisEntity mimesis) {
        this.mimesis = mimesis;
        this.m_7021_(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean m_8036_() {
        if (!this.mimesis.isHostile() && !this.mimesis.isProvokedByPlayer()) {
            return false;
        }
        Player target = this.resolveTarget();
        return target != null;
    }

    public boolean m_8045_() {
        if (!this.mimesis.isHostile() && !this.mimesis.isProvokedByPlayer()) {
            return false;
        }
        Player target = this.resolveTarget();
        return target != null && target.m_6084_();
    }

    public void m_8037_() {
        Player target = this.resolveTarget();
        if (target != null) {
            double distance;
            this.mimesis.m_21563_().m_24950_(target.m_20185_(), target.m_20188_(), target.m_20189_(), 14.0f, 14.0f);
            if (this.mimesis.isHostile()) {
                this.equipSword();
            }
            if ((distance = (double)this.mimesis.m_20270_((Entity)target)) > 2.8) {
                if (this.mimesis.isHostile() && !this.mimesis.m_142582_((Entity)target)) {
                    ++this.blockedChaseTicks;
                    boolean hasLineOfSight = this.canBuildBridgeToTarget(target);
                    if (hasLineOfSight) {
                        this.buildBridgeTowardTarget(target);
                    } else {
                        this.tickChaseBreaking(target);
                    }
                    if (this.blockedChaseTicks >= 100) {
                        this.mimesis.teleportNearCurrentTargetLikeEnderman();
                        this.blockedChaseTicks = 0;
                        this.clearChaseBreaking();
                    }
                } else {
                    this.clearChaseBreaking();
                    this.blockedChaseTicks = 0;
                }
                Vec3 chase = this.computeChasePosition(target);
                this.mimesis.m_21573_().m_26519_(chase.f_82479_, chase.f_82480_, chase.f_82481_, this.mimesis.isHostile() ? 1.35 : 1.05);
                this.mimesis.m_6858_(distance > 6.0);
            } else {
                this.clearChaseBreaking();
                this.blockedChaseTicks = 0;
                this.mimesis.m_21573_().m_26573_();
                this.mimesis.m_6858_(false);
            }
            if (distance <= 2.8 && this.attackCooldown <= 0) {
                this.mimesis.m_6674_(InteractionHand.MAIN_HAND);
                if (!this.mimesis.m_9236_().f_46443_) {
                    this.mimesis.m_9236_().m_7605_((Entity)this.mimesis, (byte)4);
                }
                boolean hit = this.mimesis.m_7327_((Entity)target);
                this.attackCooldown = 10;
                if (hit && !target.m_6084_()) {
                    this.mimesis.m_146870_();
                }
            }
            if (this.attackCooldown > 0) {
                --this.attackCooldown;
            }
            this.mimesis.m_6858_(distance > 6.0);
        }
    }

    private void tickChaseBreaking(Player target) {
        ServerLevel serverLevel;
        BlockState state;
        if (this.chaseBreakPos == null) {
            this.chaseBreakPos = this.findBlockingBlockTowardTarget(target);
            this.chaseBreakProgress = 0;
            this.chaseBreakNeededTicks = 18 + this.mimesis.m_217043_().m_188503_(24);
            if (this.chaseBreakPos == null) {
                return;
            }
        }
        if ((state = this.mimesis.m_9236_().m_8055_(this.chaseBreakPos)).m_60795_() || !this.isChaseBreakable(state)) {
            this.clearChaseBreaking();
            return;
        }
        ++this.chaseBreakProgress;
        Level level = this.mimesis.m_9236_();
        if (level instanceof ServerLevel) {
            serverLevel = (ServerLevel)level;
            int stage = Math.min(9, this.chaseBreakProgress * 10 / Math.max(1, this.chaseBreakNeededTicks));
            serverLevel.m_6801_(this.mimesis.m_19879_(), this.chaseBreakPos, stage);
        }
        this.mimesis.m_6674_(InteractionHand.MAIN_HAND);
        if (!this.mimesis.m_9236_().f_46443_) {
            this.mimesis.m_9236_().m_7605_((Entity)this.mimesis, (byte)4);
        }
        if (this.chaseBreakProgress >= this.chaseBreakNeededTicks) {
            level = this.mimesis.m_9236_();
            if (level instanceof ServerLevel) {
                serverLevel = (ServerLevel)level;
                serverLevel.m_46961_(this.chaseBreakPos, true);
            } else {
                this.mimesis.m_9236_().m_46961_(this.chaseBreakPos, true);
            }
            this.mimesis.collectNearbyItems(this.chaseBreakPos);
            level = this.mimesis.m_9236_();
            if (level instanceof ServerLevel) {
                serverLevel = (ServerLevel)level;
                serverLevel.m_6801_(this.mimesis.m_19879_(), this.chaseBreakPos, -1);
            }
            this.clearChaseBreaking();
        }
    }

    private BlockPos findBlockingBlockTowardTarget(Player target) {
        Vec3 start = this.mimesis.m_146892_();
        Vec3 end = target.m_146892_();
        Vec3 delta = end.m_82546_(start);
        double distance = delta.m_82553_();
        if (distance < 0.5) {
            return null;
        }
        Vec3 step = delta.m_82490_(1.0 / distance);
        BlockPos previous = null;
        int steps = Math.max(1, (int)Math.ceil(distance * 4.0));
        int MAX_CHASE_BREAK_DISTANCE = 8;
        for (int i = 1; i < steps; ++i) {
            BlockState state;
            Vec3 point = start.m_82549_(step.m_82490_((double)i * 0.25));
            BlockPos pos = BlockPos.m_274561_((double)point.f_82479_, (double)point.f_82480_, (double)point.f_82481_);
            if (pos.equals(previous)) continue;
            previous = pos;
            if (pos.m_203195_((Position)this.mimesis.m_20182_(), 1.2) || !pos.m_203195_((Position)this.mimesis.m_20182_(), (double)MAX_CHASE_BREAK_DISTANCE) || pos.m_123342_() < this.mimesis.m_20183_().m_123342_() || (state = this.mimesis.m_9236_().m_8055_(pos)).m_60795_() || !this.isChaseBreakable(state)) continue;
            return pos;
        }
        return null;
    }

    private boolean isChaseBreakable(BlockState state) {
        Block block = state.m_60734_();
        if (block == Blocks.f_50752_ || block == Blocks.f_50375_ || block == Blocks.f_50258_) {
            return false;
        }
        float hardness = state.m_60800_((BlockGetter)this.mimesis.m_9236_(), this.mimesis.m_20183_());
        if (hardness < 0.0f) {
            return false;
        }
        return hardness <= 5.0f || block == Blocks.f_50033_ || block == Blocks.f_50191_ || block == Blocks.f_50050_ || block == Blocks.f_50051_ || block == Blocks.f_50052_ || block == Blocks.f_50053_ || block == Blocks.f_50054_ || block == Blocks.f_50055_ || block == Blocks.f_220838_;
    }

    private void clearChaseBreaking() {
        Level level;
        if (this.chaseBreakPos != null && (level = this.mimesis.m_9236_()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            serverLevel.m_6801_(this.mimesis.m_19879_(), this.chaseBreakPos, -1);
        }
        this.chaseBreakPos = null;
        this.chaseBreakProgress = 0;
        this.chaseBreakNeededTicks = 0;
    }

    private Player resolveTarget() {
        Player p;
        Player target = null;
        if (this.mimesis.isHostile() && this.mimesis.getTargetPlayerUUID() != null && (p = this.mimesis.m_9236_().m_46003_(this.mimesis.getTargetPlayerUUID())) != null && p.m_6084_()) {
            target = p;
        }
        if (target == null && this.mimesis.m_5448_() instanceof Player && (p = (Player)this.mimesis.m_5448_()) != null && p.m_6084_()) {
            target = p;
        }
        if (target == null) {
            double range = this.mimesis.isHostile() ? 96.0 : 32.0;
            UUID appearanceUuid = this.mimesis.getAppearancePlayerUUID();
            double bestDist = Double.MAX_VALUE;
            for (Player candidate : this.mimesis.m_9236_().m_6907_()) {
                double d;
                if (candidate == null || !candidate.m_6084_() || appearanceUuid != null && appearanceUuid.equals(candidate.m_20148_()) || !((d = candidate.m_20280_((Entity)this.mimesis)) <= range * range) || !(d < bestDist)) continue;
                bestDist = d;
                target = candidate;
            }
        }
        if (target != null) {
            this.mimesis.m_6710_((LivingEntity)target);
        }
        return target;
    }

    private boolean canBuildBridgeToTarget(Player target) {
        BlockPos from = this.mimesis.m_20183_();
        BlockPos to = target.m_20183_();
        if (from.m_123342_() >= to.m_123342_() - 1 && from.m_123342_() <= to.m_123342_() + 1) {
            return false;
        }
        BlockState below = this.mimesis.m_9236_().m_8055_(from.m_7495_());
        return !below.m_60795_();
    }

    private void buildBridgeTowardTarget(Player target) {
        BlockPos base = this.mimesis.m_20183_();
        Vec3 towardTarget = target.m_20182_().m_82546_(this.mimesis.m_20182_()).m_82541_();
        int sx = (int)Math.signum(towardTarget.f_82479_);
        int sz = (int)Math.signum(towardTarget.f_82481_);
        if (sx == 0 && sz == 0) {
            sx = 1;
        }
        BlockPos place = base.m_7918_(sx, 0, sz);
        if (this.mimesis.m_9236_().m_8055_(place).m_60795_() && !this.mimesis.m_9236_().m_8055_(place.m_7495_()).m_60795_()) {
            this.mimesis.m_9236_().m_7731_(place, Blocks.f_50652_.m_49966_(), 3);
            this.mimesis.m_6674_(InteractionHand.MAIN_HAND);
            if (!this.mimesis.m_9236_().f_46443_) {
                this.mimesis.m_9236_().m_7605_((Entity)this.mimesis, (byte)4);
            }
        }
    }

    private void equipSword() {
        ItemStack mainHand = this.mimesis.m_6844_(EquipmentSlot.MAINHAND);
        if (!mainHand.m_41720_().toString().toLowerCase().contains("sword") || mainHand.m_41720_() != Items.f_42388_) {
            this.mimesis.m_8061_(EquipmentSlot.MAINHAND, new ItemStack((ItemLike)Items.f_42388_));
        }
    }

    private Vec3 computeChasePosition(Player target) {
        double dx = target.m_20185_() - this.mimesis.m_20185_();
        double dz = target.m_20189_() - this.mimesis.m_20189_();
        double length = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
        double backoff = this.mimesis.isHostile() ? 1.0 : 0.6;
        double x = target.m_20185_() - dx / length * backoff;
        double z = target.m_20189_() - dz / length * backoff;
        BlockPos pos = BlockPos.m_274561_((double)x, (double)target.m_20186_(), (double)z);
        return new Vec3((double)pos.m_123341_() + 0.5, target.m_20186_(), (double)pos.m_123343_() + 0.5);
    }

    public void m_8041_() {
        this.clearChaseBreaking();
        this.blockedChaseTicks = 0;
        if (!this.mimesis.isHostile() && !this.mimesis.isProvokedByPlayer()) {
            this.mimesis.m_6710_(null);
        }
        if (!this.mimesis.isHostile()) {
            this.mimesis.clearHeldItems();
        }
    }
}

