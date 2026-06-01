/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.Containers
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.ai.goal.Goal
 *  net.minecraft.world.entity.ai.goal.Goal$Flag
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LightLayer
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.ChestBlock
 *  net.minecraft.world.level.block.DoorBlock
 *  net.minecraft.world.level.block.FurnaceBlock
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.FurnaceBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.DoubleBlockHalf
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 */
package com.mimesis.ai;

import com.mimesis.entity.MimesisEntity;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MimesisPlayerLikeMovementGoal
extends Goal {
    private final MimesisEntity mimesis;
    private final Deque<Vec3> recentDestinations = new ArrayDeque<Vec3>();
    private State state = State.OBSERVE;
    private int stateTicks = 0;
    private int pauseTicks = 0;
    private int hesitationTicks = 0;
    private int repathTicks = 0;
    private int lookDriftTicks = 0;
    private int stuckTicks = 0;
    private int reactionDelayTicks = 0;
    private int attackCooldown = 0;
    private int attackWindup = 0;
    private int worldActionTicks = 0;
    private int observeMoveTicks = 0;
    private Vec3 destination;
    private Vec3 lastPos;
    private BlockPos breakingPos;
    private int breakingProgressTicks = 0;
    private int breakingNeededTicks = 0;

    public MimesisPlayerLikeMovementGoal(MimesisEntity mimesis) {
        this.mimesis = mimesis;
        this.m_7021_(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean m_8036_() {
        return this.getObservedPlayer() != null && !this.mimesis.isHostile() && !this.mimesis.isProvokedByPlayer();
    }

    public boolean m_8045_() {
        return this.getObservedPlayer() != null && !this.mimesis.isHostile() && !this.mimesis.isProvokedByPlayer();
    }

    public void m_8056_() {
        this.state = this.findNearestOreBlock(14) != null ? State.MINE : State.CRAFT;
        this.stateTicks = 120;
        this.pauseTicks = 0;
        this.hesitationTicks = 0;
        this.reactionDelayTicks = 8 + this.mimesis.m_217043_().m_188503_(10);
        this.observeMoveTicks = 0;
    }

    public void m_8037_() {
        Player observed = this.getObservedPlayer();
        if (observed == null) {
            return;
        }
        if (this.mimesis.isHostile() || this.mimesis.isProvokedByPlayer()) {
            return;
        }
        --this.stateTicks;
        --this.repathTicks;
        --this.lookDriftTicks;
        --this.attackCooldown;
        if (this.shouldSwitchState(observed)) {
            this.switchState(observed);
        }
        switch (this.state) {
            case OBSERVE: {
                this.tickObserve(observed);
                break;
            }
            case FOLLOW: {
                this.tickFollow(observed);
                break;
            }
            case WANDER: {
                this.tickWander(observed);
                break;
            }
            case INTERACT: {
                this.tickInteract(observed);
                break;
            }
            case IMITATE: {
                this.tickImitate(observed);
                break;
            }
            case CRAFT: {
                this.tickCraft(observed);
                break;
            }
            case MINE: {
                this.tickMine(observed);
                break;
            }
            case ATTACK: {
                this.tickAttack(observed);
            }
        }
        this.updateStuck(observed);
    }

    private boolean shouldSwitchState(Player observed) {
        BlockPos ore = this.findNearestOreBlock(14);
        if (ore != null) {
            return this.state != State.MINE;
        }
        return this.state != State.CRAFT;
    }

    private void switchState(Player observed) {
        if (this.findNearestOreBlock(14) != null) {
            this.enterState(State.MINE, 120);
        } else {
            this.enterState(State.CRAFT, 120);
        }
    }

    private void enterState(State next, int duration) {
        this.state = next;
        this.stateTicks = duration;
        this.destination = null;
        this.worldActionTicks = 0;
        this.observeMoveTicks = 0;
        if (next == State.ATTACK) {
            this.hesitationTicks = 0;
            this.pauseTicks = 0;
        } else {
            this.hesitationTicks = 0;
            this.pauseTicks = 0;
        }
        if (next != State.ATTACK) {
            this.clearBreaking();
        }
    }

    private void tickObserve(Player observed) {
        if (this.observeMoveTicks <= 0 && this.mimesis.m_217043_().m_188503_(100) < 35) {
            this.observeMoveTicks = 12 + this.mimesis.m_217043_().m_188503_(28);
            this.destination = this.findWanderDestination(observed);
            this.repathTicks = 8 + this.mimesis.m_217043_().m_188503_(12);
        }
        if (this.observeMoveTicks > 0) {
            if (this.destination == null || this.repathTicks <= 0 || this.mimesis.m_21573_().m_26571_()) {
                this.destination = this.findWanderDestination(observed);
                this.repathTicks = 8 + this.mimesis.m_217043_().m_188503_(14);
            }
            this.moveToDestination(this.addMovementError(0.52), false);
            --this.observeMoveTicks;
        } else {
            this.mimesis.m_21573_().m_26573_();
        }
        this.mimesis.m_6858_(false);
        this.mimesis.m_20260_(false);
        this.slowLook(observed, this.mimesis.m_217043_().m_188503_(100) < 35);
        if (this.mimesis.m_217043_().m_188503_(100) < 5) {
            this.pauseTicks = 15 + this.mimesis.m_217043_().m_188503_(55);
        }
    }

    private void tickFollow(Player observed) {
        if (this.destination == null || this.repathTicks <= 0 || this.mimesis.m_21573_().m_26571_()) {
            this.destination = this.computeImperfectFollowDestination(observed);
            this.repathTicks = 10 + this.mimesis.m_217043_().m_188503_(20);
        }
        boolean sprint = (double)this.mimesis.m_20270_((Entity)observed) > 14.0 && this.mimesis.m_217043_().m_188503_(100) < 65;
        double speed = sprint ? 0.92 : 0.7;
        speed = this.addMovementError(speed);
        this.moveToDestination(speed, sprint);
        this.slowLook(observed, this.mimesis.m_217043_().m_188503_(100) < 25);
        if (this.mimesis.m_217043_().m_188503_(100) < 10) {
            this.pauseTicks = 5 + this.mimesis.m_217043_().m_188503_(15);
        }
    }

    private void tickWander(Player observed) {
        if (this.destination == null || this.repathTicks <= 0 || this.mimesis.m_21573_().m_26571_()) {
            this.destination = this.findWanderDestination(observed);
            this.repathTicks = 12 + this.mimesis.m_217043_().m_188503_(26);
        }
        double speed = this.addMovementError(0.58 + this.mimesis.m_217043_().m_188500_() * 0.22);
        this.moveToDestination(speed, false);
        if (this.mimesis.m_217043_().m_188503_(100) < 40) {
            this.slowLook(observed, true);
        } else {
            this.lookTowardPath();
        }
        if (this.mimesis.m_217043_().m_188503_(100) < 8) {
            this.pauseTicks = 10 + this.mimesis.m_217043_().m_188503_(30);
        }
    }

    private void tickInteract(Player observed) {
        ++this.worldActionTicks;
        if (this.destination == null || this.repathTicks <= 0 || this.mimesis.m_21573_().m_26571_()) {
            this.destination = this.findInteractiveDestination(observed);
            this.repathTicks = 10 + this.mimesis.m_217043_().m_188503_(20);
        }
        double speed = this.addMovementError(0.6);
        this.moveToDestination(speed, false);
        this.lookTowardPath();
        if (this.destination != null && this.mimesis.m_20238_(this.destination) < 9.0) {
            this.tryDoorOrChestInteraction();
            this.tickSlowBreaking();
            if (this.worldActionTicks % 25 == 0 && this.mimesis.m_217043_().m_188503_(100) < 25) {
                this.placeOccasionalBlock();
            }
            if (this.mimesis.m_217043_().m_188503_(100) < 20) {
                this.abortCurrentAction();
            }
        } else {
            this.clearBreaking();
        }
    }

    private void tickCraft(Player observed) {
        boolean stationIsFurnace;
        ++this.worldActionTicks;
        this.mimesis.m_21573_().m_26573_();
        this.mimesis.m_6858_(false);
        this.mimesis.m_20260_(false);
        this.equipFurnaceAndFuel();
        BlockPos trackedStation = this.mimesis.getCookingStationPos();
        if (trackedStation != null && !this.isCookingStationPresent(trackedStation)) {
            this.mimesis.clearCookingStation();
            trackedStation = null;
            this.destination = null;
        }
        BlockPos table = this.findNearestCraftingTable(16);
        BlockPos furnace = this.findNearestFurnace(16);
        BlockPos station = trackedStation;
        boolean bl = stationIsFurnace = station != null && this.isFurnaceBlock(station);
        if (station == null) {
            station = this.chooseNearestStation(table, furnace);
            boolean bl2 = stationIsFurnace = station != null && this.isFurnaceBlock(station);
        }
        if (station == null && this.worldActionTicks % 20 == 0 && this.mimesis.m_217043_().m_188503_(100) < 70) {
            if (this.mimesis.m_217043_().m_188499_()) {
                this.placeFurnaceStationNearSelf();
                station = this.findNearestFurnace(10);
                stationIsFurnace = true;
            } else {
                this.placeCraftingTableStationNearSelf();
                station = this.findNearestCraftingTable(10);
                stationIsFurnace = false;
            }
        }
        if (station == null) {
            this.destination = this.findWanderDestination(observed);
            this.repathTicks = 0;
            this.moveToDestination(this.addMovementError(1.0), false);
            this.lookTowardPath();
            return;
        }
        this.lookAtBlock(station);
        double distance = this.mimesis.m_20275_((double)station.m_123341_() + 0.5, (double)station.m_123342_() + 0.5, (double)station.m_123343_() + 0.5);
        if (distance > 6.25) {
            Vec3 stand;
            this.destination = stand = this.findStandPositionNear(station);
            this.repathTicks = 6 + this.mimesis.m_217043_().m_188503_(10);
            this.moveToDestination(this.addMovementError(1.0), false);
        } else {
            this.destination = null;
        }
        if (stationIsFurnace) {
            this.updateCookingStationContents(station);
        }
        if (this.mimesis.m_9236_().m_45517_(LightLayer.BLOCK, this.mimesis.m_20183_()) <= 2) {
            this.placeTorchOccasionally();
        }
    }

    private void tickMine(Player observed) {
        ++this.worldActionTicks;
        this.equipIronPickaxe();
        BlockPos ore = this.findNearestOreBlock(14);
        if (ore == null) {
            this.mimesis.m_21573_().m_26573_();
            this.enterState(State.CRAFT, 120);
            return;
        }
        this.destination = new Vec3((double)ore.m_123341_() + 0.5, (double)ore.m_123342_(), (double)ore.m_123343_() + 0.5);
        this.repathTicks = 4 + this.mimesis.m_217043_().m_188503_(8);
        this.moveToDestination(this.addMovementError(1.0), false);
        this.mimesis.m_21563_().m_24950_((double)ore.m_123341_() + 0.5, (double)ore.m_123342_() + 0.5, (double)ore.m_123343_() + 0.5, 12.0f, 12.0f);
        if (this.mimesis.m_20275_((double)ore.m_123341_() + 0.5, (double)ore.m_123342_() + 0.5, (double)ore.m_123343_() + 0.5) < 10.0) {
            this.breakingPos = ore;
            if (this.breakingNeededTicks <= 0) {
                this.breakingNeededTicks = 24 + this.mimesis.m_217043_().m_188503_(24);
            }
            this.tickOreBreaking();
        } else {
            this.clearBreaking();
        }
        if (this.mimesis.m_9236_().m_45517_(LightLayer.BLOCK, this.mimesis.m_20183_()) <= 2) {
            this.placeTorchOccasionally();
        }
    }

    private void tickImitate(Player observed) {
        boolean playerSneak = observed.m_6144_();
        boolean playerSprint = observed.m_20142_();
        if (this.destination == null || this.repathTicks <= 0 || this.mimesis.m_21573_().m_26571_()) {
            Vec3 targetPos = observed.m_20182_();
            double side = this.mimesis.m_217043_().m_188499_() ? 1.0 : -1.0;
            double angle = Math.atan2(observed.m_20189_() - this.mimesis.m_20189_(), observed.m_20185_() - this.mimesis.m_20185_());
            double px = targetPos.f_82479_ + Math.cos(angle + side * 1.2) * (2.0 + this.mimesis.m_217043_().m_188500_() * 3.0);
            double pz = targetPos.f_82481_ + Math.sin(angle + side * 1.2) * (2.0 + this.mimesis.m_217043_().m_188500_() * 3.0);
            this.destination = this.snapToGround(px, pz, observed.m_20186_());
            this.repathTicks = 8 + this.mimesis.m_217043_().m_188503_(14);
        }
        double speed = playerSprint ? 0.8 : 0.58;
        speed = this.addMovementError(speed);
        this.moveToDestination(speed, playerSprint && this.mimesis.m_217043_().m_188503_(100) < 50);
        this.mimesis.m_20260_(playerSneak && this.mimesis.m_217043_().m_188503_(100) < 70);
        if (this.mimesis.m_217043_().m_188503_(100) < 35) {
            this.slowLook(observed, true);
        } else {
            this.lookTowardPath();
        }
        if (this.mimesis.m_217043_().m_188503_(100) < 12) {
            this.pauseTicks = 8 + this.mimesis.m_217043_().m_188503_(18);
        }
    }

    private void tickAttack(Player observed) {
        double distance = this.mimesis.m_20270_((Entity)observed);
        this.mimesis.m_6710_((LivingEntity)observed);
        if (this.destination == null || this.repathTicks <= 0 || this.mimesis.m_21573_().m_26571_()) {
            if (distance > 4.5) {
                this.destination = new Vec3(observed.m_20185_(), observed.m_20186_(), observed.m_20189_());
                this.repathTicks = 2 + this.mimesis.m_217043_().m_188503_(3);
            } else {
                this.destination = this.computeImperfectAttackDestination(observed);
                this.repathTicks = 3 + this.mimesis.m_217043_().m_188503_(4);
            }
        }
        boolean sprint = distance > 3.5;
        double speed = sprint ? 1.0 : 0.86;
        speed = this.addMovementError(speed);
        this.moveToDestination(speed, sprint);
        this.slowLook(observed, false);
        this.equipDiamondSword();
        if (distance < 2.6) {
            if (this.attackCooldown <= 0) {
                this.mimesis.m_6674_(InteractionHand.MAIN_HAND);
                if (!this.mimesis.m_9236_().f_46443_) {
                    this.mimesis.m_9236_().m_7605_((Entity)this.mimesis, (byte)4);
                }
                this.mimesis.m_7327_((Entity)observed);
                this.attackCooldown = 20;
                this.pauseTicks = 0;
                if (!observed.m_6084_() || observed.m_21224_() || observed.m_21223_() <= 0.0f) {
                    this.mimesis.m_146870_();
                    return;
                }
            }
        } else {
            this.attackWindup = 0;
        }
        if (this.tryOpenDoorNearSelf()) {
            this.repathTicks = 0;
        }
    }

    private void updateStuck(Player observed) {
        BlockPos table;
        if (this.state == State.CRAFT && (table = this.findNearestCraftingTable(16)) != null) {
            this.lookAtBlock(table);
            if (this.mimesis.m_20275_((double)table.m_123341_() + 0.5, (double)table.m_123342_() + 0.5, (double)table.m_123343_() + 0.5) <= 9.0) {
                this.mimesis.m_21573_().m_26573_();
                this.destination = null;
                this.stuckTicks = 0;
                return;
            }
        }
        Vec3 now = this.mimesis.m_20182_();
        this.stuckTicks = this.lastPos != null && now.m_82554_(this.lastPos) < 0.08 ? ++this.stuckTicks : 0;
        this.lastPos = now;
        if (this.stuckTicks < 8) {
            return;
        }
        if (this.state == State.CRAFT) {
            this.mimesis.m_21573_().m_26573_();
            BlockPos table2 = this.findNearestCraftingTable(10);
            if (table2 != null) {
                this.lookAtBlock(table2);
            }
            this.stuckTicks = 0;
            return;
        }
        if (this.tryOpenDoorNearSelf()) {
            this.stuckTicks = 0;
            return;
        }
        if (this.breakSoftObstacleInFront()) {
            this.stuckTicks = 0;
            return;
        }
        if (this.tryEscapeHole()) {
            this.stuckTicks = 0;
            return;
        }
        if (this.mimesis.m_217043_().m_188503_(100) < 45) {
            this.placeOccasionalBlock();
        }
        if (this.stuckTicks >= 15) {
            Vec3 newPos;
            Vec3 forward = this.mimesis.m_20154_().m_82490_(3.0);
            this.destination = newPos = now.m_82549_(forward);
            this.repathTicks = 0;
            this.stuckTicks = 0;
            return;
        }
        this.destination = this.findWanderDestination(observed);
        this.repathTicks = 0;
        this.pauseTicks = 4 + this.mimesis.m_217043_().m_188503_(12);
    }

    private void moveToDestination(double speed, boolean sprint) {
        if (this.destination == null) {
            return;
        }
        this.mimesis.m_21573_().m_26519_(this.destination.f_82479_, this.destination.f_82480_, this.destination.f_82481_, speed);
        this.mimesis.m_6858_(sprint);
    }

    private void slowLook(Player observed, boolean addNoise) {
        if (this.lookDriftTicks <= 0) {
            this.lookDriftTicks = 12 + this.mimesis.m_217043_().m_188503_(24);
        }
        double x = observed.m_20185_();
        double y = observed.m_20188_();
        double z = observed.m_20189_();
        if (addNoise) {
            x += (this.mimesis.m_217043_().m_188500_() - 0.5) * 2.2;
            y += (this.mimesis.m_217043_().m_188500_() - 0.5) * 1.8;
            z += (this.mimesis.m_217043_().m_188500_() - 0.5) * 2.2;
        }
        this.mimesis.m_21563_().m_24950_(x, y, z, 9.0f, 10.0f);
    }

    private void lookTowardPath() {
        if (this.destination == null) {
            return;
        }
        Vec3 eye = this.mimesis.m_20182_();
        Vec3 dir = this.destination.m_82546_(eye);
        if (dir.m_82556_() < 1.0E-4) {
            return;
        }
        dir = dir.m_82541_();
        double upDown = 0.0;
        int roll = this.mimesis.m_217043_().m_188503_(100);
        if (roll < 15) {
            upDown = 1.4;
        } else if (roll < 30) {
            upDown = -1.2;
        }
        this.mimesis.m_21563_().m_24950_(eye.f_82479_ + dir.f_82479_ * 4.0, this.mimesis.m_20188_() + upDown, eye.f_82481_ + dir.f_82481_ * 4.0, 11.0f, 11.0f);
    }

    private Vec3 computeImperfectFollowDestination(Player observed) {
        double z;
        Vec3 playerPos = observed.m_20182_();
        double angle = Math.atan2(observed.m_20189_() - this.mimesis.m_20189_(), observed.m_20185_() - this.mimesis.m_20185_());
        double offsetAngle = angle + (this.mimesis.m_217043_().m_188500_() - 0.5) * 1.6;
        double range = 2.5 + this.mimesis.m_217043_().m_188500_() * 5.5;
        double x = playerPos.f_82479_ - Math.cos(offsetAngle) * range;
        Vec3 snapped = this.snapToGround(x, z = playerPos.f_82481_ - Math.sin(offsetAngle) * range, observed.m_20186_());
        if (this.isRecentlyVisited(snapped)) {
            snapped = this.snapToGround(x += (this.mimesis.m_217043_().m_188500_() - 0.5) * 4.0, z += (this.mimesis.m_217043_().m_188500_() - 0.5) * 4.0, observed.m_20186_());
        }
        return snapped;
    }

    private Vec3 computeImperfectAttackDestination(Player observed) {
        Vec3 playerPos = observed.m_20182_();
        double angle = Math.atan2(observed.m_20189_() - this.mimesis.m_20189_(), observed.m_20185_() - this.mimesis.m_20185_());
        double side = this.mimesis.m_217043_().m_188499_() ? 1.0 : -1.0;
        double jitter = (this.mimesis.m_217043_().m_188500_() - 0.5) * 0.5;
        double strafeRadius = 1.2 + this.mimesis.m_217043_().m_188500_() * 1.0;
        double perpAngle = angle + side * 1.57079632679;
        double x = playerPos.f_82479_ - Math.cos(angle) * strafeRadius + Math.cos(perpAngle) * 0.8 + jitter;
        double z = playerPos.f_82481_ - Math.sin(angle) * strafeRadius + Math.sin(perpAngle) * 0.8 - jitter;
        return this.snapToGround(x, z, observed.m_20186_());
    }

    private Vec3 findWanderDestination(Player observed) {
        for (int i = 0; i < 10; ++i) {
            double z;
            double angle = this.mimesis.m_217043_().m_188500_() * Math.PI * 2.0;
            double range = 4.0 + this.mimesis.m_217043_().m_188500_() * 14.0;
            double x = this.mimesis.m_20185_() + Math.cos(angle) * range;
            Vec3 p = this.snapToGround(x, z = this.mimesis.m_20189_() + Math.sin(angle) * range, observed.m_20186_());
            if (this.isRecentlyVisited(p)) continue;
            return p;
        }
        return this.snapToGround(observed.m_20185_() + (this.mimesis.m_217043_().m_188500_() - 0.5) * 10.0, observed.m_20189_() + (this.mimesis.m_217043_().m_188500_() - 0.5) * 10.0, observed.m_20186_());
    }

    private Vec3 findInteractiveDestination(Player observed) {
        BlockPos center = BlockPos.m_274561_((double)this.mimesis.m_20185_(), (double)this.mimesis.m_20186_(), (double)this.mimesis.m_20189_());
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = -12; x <= 12; ++x) {
            for (int y = -3; y <= 3; ++y) {
                for (int z = -12; z <= 12; ++z) {
                    double d;
                    BlockPos pos = center.m_7918_(x, y, z);
                    BlockState state = this.mimesis.m_9236_().m_8055_(pos);
                    Block block = state.m_60734_();
                    if (!(block instanceof DoorBlock) && !(block instanceof ChestBlock) && block != Blocks.f_50091_ || !((d = this.mimesis.m_20275_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5)) < bestDist)) continue;
                    bestDist = d;
                    best = pos;
                }
            }
        }
        if (best != null) {
            return new Vec3((double)best.m_123341_() + 0.5, (double)best.m_123342_(), (double)best.m_123343_() + 0.5);
        }
        return this.findWanderDestination(observed);
    }

    private void tryDoorOrChestInteraction() {
        BlockPos chest;
        BlockPos base = this.mimesis.m_20183_();
        if (this.mimesis.m_217043_().m_188503_(100) < 60) {
            this.tryOpenCloseDoor(base);
            this.tryOpenCloseDoor(base.m_122012_());
            this.tryOpenCloseDoor(base.m_122019_());
            this.tryOpenCloseDoor(base.m_122029_());
            this.tryOpenCloseDoor(base.m_122024_());
            this.tryOpenCloseDoor(base.m_7494_());
            this.tryOpenCloseDoor(base.m_7495_());
        }
        if (this.mimesis.m_217043_().m_188503_(100) < 30 && (chest = this.findNearestChest(8)) != null) {
            this.mimesis.m_9236_().m_5594_(null, chest, SoundEvents.f_11749_, SoundSource.BLOCKS, 0.6f, 1.0f);
            if (this.mimesis.m_217043_().m_188503_(100) < 35) {
                this.mimesis.m_9236_().m_5594_(null, chest, SoundEvents.f_11747_, SoundSource.BLOCKS, 0.55f, 1.0f);
            }
        }
    }

    private boolean tryOpenDoorNearSelf() {
        BlockPos base = this.mimesis.m_20183_();
        return this.tryOpenCloseDoor(base) || this.tryOpenCloseDoor(base.m_122012_()) || this.tryOpenCloseDoor(base.m_122019_()) || this.tryOpenCloseDoor(base.m_122029_()) || this.tryOpenCloseDoor(base.m_122024_()) || this.tryOpenCloseDoor(base.m_7494_()) || this.tryOpenCloseDoor(base.m_7495_());
    }

    private boolean tryOpenCloseDoor(BlockPos pos) {
        boolean nextOpen;
        BlockState state = this.mimesis.m_9236_().m_8055_(pos);
        if (!(state.m_60734_() instanceof DoorBlock) || !state.m_61138_((Property)DoorBlock.f_52727_)) {
            return false;
        }
        BlockPos lower = pos;
        if (state.m_61138_((Property)BlockStateProperties.f_61401_) && state.m_61143_((Property)BlockStateProperties.f_61401_) == DoubleBlockHalf.UPPER) {
            lower = pos.m_7495_();
            state = this.mimesis.m_9236_().m_8055_(lower);
        }
        if (!(state.m_60734_() instanceof DoorBlock) || !state.m_61138_((Property)DoorBlock.f_52727_)) {
            return false;
        }
        boolean currentlyOpen = (Boolean)state.m_61143_((Property)DoorBlock.f_52727_);
        if (!currentlyOpen) {
            nextOpen = true;
        } else {
            boolean bl = nextOpen = this.mimesis.m_217043_().m_188503_(100) >= 28;
        }
        if (nextOpen == currentlyOpen) {
            return false;
        }
        this.mimesis.m_9236_().m_7731_(lower, (BlockState)state.m_61124_((Property)DoorBlock.f_52727_, (Comparable)Boolean.valueOf(nextOpen)), 10);
        BlockPos upper = lower.m_7494_();
        BlockState upperState = this.mimesis.m_9236_().m_8055_(upper);
        if (upperState.m_60734_() instanceof DoorBlock && upperState.m_61138_((Property)DoorBlock.f_52727_)) {
            this.mimesis.m_9236_().m_7731_(upper, (BlockState)upperState.m_61124_((Property)DoorBlock.f_52727_, (Comparable)Boolean.valueOf(nextOpen)), 10);
        }
        this.mimesis.m_9236_().m_5594_(null, lower, nextOpen ? SoundEvents.f_12627_ : SoundEvents.f_12626_, SoundSource.BLOCKS, 0.65f, 1.0f);
        return true;
    }

    private BlockPos findNearestChest(int radius) {
        BlockPos center = this.mimesis.m_20183_();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; ++x) {
            for (int y = -2; y <= 2; ++y) {
                for (int z = -radius; z <= radius; ++z) {
                    double d;
                    BlockPos pos = center.m_7918_(x, y, z);
                    BlockState state = this.mimesis.m_9236_().m_8055_(pos);
                    if (!(state.m_60734_() instanceof ChestBlock) || !((d = this.mimesis.m_20275_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5)) < bestDist)) continue;
                    bestDist = d;
                    best = pos;
                }
            }
        }
        return best;
    }

    private void tickSlowBreaking() {
        if (this.breakingPos == null) {
            BlockPos candidate = this.findBreakableBlockNear(3);
            if (candidate == null) {
                return;
            }
            this.breakingPos = candidate;
            this.breakingProgressTicks = 0;
            this.breakingNeededTicks = 18 + this.mimesis.m_217043_().m_188503_(45);
        }
        if (this.mimesis.m_217043_().m_188503_(100) < 12) {
            this.abortCurrentAction();
            return;
        }
        BlockState state = this.mimesis.m_9236_().m_8055_(this.breakingPos);
        if (state.m_60795_() || !this.isSoftBreakable(state)) {
            this.clearBreaking();
            return;
        }
        ++this.breakingProgressTicks;
        int stage = Math.min(9, this.breakingProgressTicks * 10 / Math.max(1, this.breakingNeededTicks));
        this.mimesis.m_9236_().m_6801_(this.mimesis.m_19879_(), this.breakingPos, stage);
        if (this.breakingProgressTicks >= this.breakingNeededTicks) {
            this.mimesis.m_6674_(InteractionHand.MAIN_HAND);
            if (!this.mimesis.m_9236_().f_46443_) {
                this.mimesis.m_9236_().m_7605_((Entity)this.mimesis, (byte)4);
            }
            this.mimesis.m_9236_().m_46961_(this.breakingPos, true);
            this.mimesis.collectNearbyItems(this.breakingPos);
            this.mimesis.m_9236_().m_6801_(this.mimesis.m_19879_(), this.breakingPos, -1);
            this.clearBreaking();
        }
    }

    private void tickOreBreaking() {
        if (this.breakingPos == null) {
            return;
        }
        BlockState state = this.mimesis.m_9236_().m_8055_(this.breakingPos);
        if (state.m_60795_() || !this.isOreBlock(state)) {
            this.clearBreaking();
            return;
        }
        if (this.breakingPos.m_123342_() < this.mimesis.m_20183_().m_123342_()) {
            this.clearBreaking();
            this.tryEscapeHole();
            return;
        }
        ++this.breakingProgressTicks;
        int stage = Math.min(9, this.breakingProgressTicks * 10 / Math.max(1, this.breakingNeededTicks));
        this.mimesis.m_9236_().m_6801_(this.mimesis.m_19879_(), this.breakingPos, stage);
        if (this.breakingProgressTicks >= this.breakingNeededTicks) {
            this.mimesis.m_6674_(InteractionHand.MAIN_HAND);
            if (!this.mimesis.m_9236_().f_46443_) {
                this.mimesis.m_9236_().m_7605_((Entity)this.mimesis, (byte)4);
            }
            this.mimesis.m_9236_().m_46961_(this.breakingPos, true);
            this.mimesis.collectNearbyItems(this.breakingPos);
            this.mimesis.m_9236_().m_6801_(this.mimesis.m_19879_(), this.breakingPos, -1);
            this.clearBreaking();
        }
    }

    private void clearBreaking() {
        if (this.breakingPos != null) {
            this.mimesis.m_9236_().m_6801_(this.mimesis.m_19879_(), this.breakingPos, -1);
        }
        this.breakingPos = null;
        this.breakingProgressTicks = 0;
        this.breakingNeededTicks = 0;
    }

    private void abortCurrentAction() {
        this.clearBreaking();
        this.mimesis.m_21573_().m_26573_();
        this.pauseTicks = 8 + this.mimesis.m_217043_().m_188503_(18);
    }

    private boolean breakSoftObstacleInFront() {
        Vec3 look = this.mimesis.m_20154_();
        int sx = (int)Math.signum(look.f_82479_);
        int sz = (int)Math.signum(look.f_82481_);
        if (sx == 0 && sz == 0) {
            sx = 1;
        }
        if (sx != 0 && sz != 0) {
            if (this.mimesis.m_217043_().m_188499_()) {
                sz = 0;
            } else {
                sx = 0;
            }
        }
        BlockPos base = this.mimesis.m_20183_();
        for (int i = 1; i <= 2; ++i) {
            BlockPos p = base.m_7918_(sx * i, 0, sz * i);
            BlockState s = this.mimesis.m_9236_().m_8055_(p);
            if (s.m_60795_() || !this.isSoftBreakable(s)) continue;
            this.mimesis.m_9236_().m_46961_(p, true);
            return true;
        }
        return false;
    }

    private void placeOccasionalBlock() {
        BlockPos base = this.mimesis.m_20183_();
        Vec3 look = this.mimesis.m_20154_();
        int sx = (int)Math.signum(look.f_82479_);
        int sz = (int)Math.signum(look.f_82481_);
        if (sx == 0 && sz == 0) {
            sx = 1;
        }
        if (sx != 0 && sz != 0) {
            if (this.mimesis.m_217043_().m_188499_()) {
                sx = 0;
            } else {
                sz = 0;
            }
        }
        BlockPos place = base.m_7918_(sx, -1, sz);
        BlockPos above = place.m_7494_();
        BlockState belowState = this.mimesis.m_9236_().m_8055_(place.m_7495_());
        if (this.mimesis.m_9236_().m_8055_(place).m_60795_() && this.mimesis.m_9236_().m_8055_(above).m_60795_() && belowState.m_60819_().m_76178_()) {
            this.mimesis.m_9236_().m_7731_(place, Blocks.f_50652_.m_49966_(), 3);
            this.mimesis.m_6674_(InteractionHand.MAIN_HAND);
            if (!this.mimesis.m_9236_().f_46443_) {
                this.mimesis.m_9236_().m_7605_((Entity)this.mimesis, (byte)4);
            }
            return;
        }
        if (!belowState.m_60819_().m_76178_()) {
            BlockPos scan = place.m_7495_();
            int maxDepth = 6;
            while (maxDepth-- > 0 && scan.m_123342_() > this.mimesis.m_9236_().m_141937_()) {
                BlockState s = this.mimesis.m_9236_().m_8055_(scan);
                if (s.m_60819_().m_76178_() && !s.m_60795_()) {
                    BlockPos target = scan.m_7494_();
                    BlockState targetState = this.mimesis.m_9236_().m_8055_(target);
                    if (targetState.m_60795_()) {
                        this.mimesis.m_9236_().m_7731_(target, Blocks.f_50652_.m_49966_(), 3);
                        this.mimesis.m_6674_(InteractionHand.MAIN_HAND);
                        if (!this.mimesis.m_9236_().f_46443_) {
                            this.mimesis.m_9236_().m_7605_((Entity)this.mimesis, (byte)4);
                        }
                        return;
                    }
                }
                scan = scan.m_7495_();
            }
        }
    }

    private boolean tryEscapeHole() {
        BlockPos below = this.mimesis.m_20183_().m_7495_();
        BlockState belowState = this.mimesis.m_9236_().m_8055_(below);
        if (!belowState.m_60795_()) {
            return false;
        }
        this.mimesis.m_21573_().m_26573_();
        this.mimesis.m_6858_(false);
        this.mimesis.m_21569_().m_24901_();
        this.mimesis.m_9236_().m_7731_(below, Blocks.f_50652_.m_49966_(), 3);
        this.mimesis.m_6674_(InteractionHand.MAIN_HAND);
        if (!this.mimesis.m_9236_().f_46443_) {
            this.mimesis.m_9236_().m_7605_((Entity)this.mimesis, (byte)4);
        }
        this.destination = new Vec3((double)below.m_123341_() + 0.5, (double)below.m_123342_() + 1.0, (double)below.m_123343_() + 0.5);
        this.pauseTicks = 0;
        return true;
    }

    private void placeTorchOccasionally() {
        BlockPos[] candidates;
        if (this.mimesis.m_217043_().m_188503_(100) >= 14) {
            return;
        }
        BlockPos base = this.mimesis.m_20183_();
        for (BlockPos pos : candidates = new BlockPos[]{base.m_122012_(), base.m_122019_(), base.m_122029_(), base.m_122024_(), base.m_7494_()}) {
            BlockState belowState;
            BlockState state = this.mimesis.m_9236_().m_8055_(pos);
            if (!state.m_60795_() || (belowState = this.mimesis.m_9236_().m_8055_(pos.m_7495_())).m_60795_() || !belowState.m_60783_((BlockGetter)this.mimesis.m_9236_(), pos.m_7495_(), Direction.UP) || !belowState.m_60819_().m_76178_()) continue;
            this.mimesis.m_9236_().m_7731_(pos, Blocks.f_50081_.m_49966_(), 3);
            this.mimesis.m_6674_(InteractionHand.MAIN_HAND);
            if (!this.mimesis.m_9236_().f_46443_) {
                this.mimesis.m_9236_().m_7605_((Entity)this.mimesis, (byte)4);
            }
            return;
        }
    }

    private void placeFurnaceStationNearSelf() {
        BlockPos[] candidates;
        BlockPos base = this.mimesis.m_20183_();
        for (BlockPos pos : candidates = new BlockPos[]{base.m_122012_(), base.m_122019_(), base.m_122029_(), base.m_122024_(), base.m_122012_().m_122029_(), base.m_122012_().m_122024_(), base.m_122019_().m_122029_(), base.m_122019_().m_122024_()}) {
            if (!this.canPlaceTableAt(pos)) continue;
            double dx = this.mimesis.m_20185_() - ((double)pos.m_123341_() + 0.5);
            double dz = this.mimesis.m_20189_() - ((double)pos.m_123343_() + 0.5);
            Direction facing = Math.abs(dx) > Math.abs(dz) ? (dx > 0.0 ? Direction.EAST : Direction.WEST) : (dz > 0.0 ? Direction.SOUTH : Direction.NORTH);
            this.mimesis.m_9236_().m_7731_(pos, (BlockState)((BlockState)Blocks.f_50094_.m_49966_().m_61124_((Property)FurnaceBlock.f_48683_, (Comparable)facing)).m_61124_((Property)FurnaceBlock.f_48684_, (Comparable)Boolean.valueOf(true)), 3);
            this.configureFurnaceStation(pos);
            this.mimesis.clearHeldItems();
            this.destination = new Vec3((double)pos.m_123341_() + 0.5, (double)pos.m_123342_(), (double)pos.m_123343_() + 0.5);
            return;
        }
    }

    private void configureFurnaceStation(BlockPos pos) {
        BlockEntity blockEntity = this.mimesis.m_9236_().m_7702_(pos);
        if (!(blockEntity instanceof FurnaceBlockEntity)) {
            return;
        }
        FurnaceBlockEntity furnace = (FurnaceBlockEntity)blockEntity;
        for (int i = 0; i < 3; ++i) {
            furnace.m_6836_(i, ItemStack.f_41583_);
        }
        furnace.m_6596_();
        this.mimesis.setCookingStation(pos);
    }

    private void updateCookingStationContents(BlockPos pos) {
        BlockEntity blockEntity = this.mimesis.m_9236_().m_7702_(pos);
        if (!(blockEntity instanceof FurnaceBlockEntity)) {
            return;
        }
        FurnaceBlockEntity furnace = (FurnaceBlockEntity)blockEntity;
        for (int i = 0; i < 3; ++i) {
            if (furnace.m_8020_(i).m_41619_()) continue;
            for (int slot = 0; slot < 3; ++slot) {
                ItemStack stack = furnace.m_8020_(slot);
                if (stack.m_41619_()) continue;
                Containers.m_18992_((Level)this.mimesis.m_9236_(), (double)((double)pos.m_123341_() + 0.5), (double)((double)pos.m_123342_() + 0.5), (double)((double)pos.m_123343_() + 0.5), (ItemStack)stack.m_41777_());
                furnace.m_6836_(slot, ItemStack.f_41583_);
            }
            furnace.m_6596_();
            this.mimesis.setCookingStation(pos);
            return;
        }
        BlockState old = this.mimesis.m_9236_().m_8055_(pos);
        if (old.m_60734_() == Blocks.f_50094_) {
            Direction facing;
            Direction direction = facing = old.m_61138_((Property)FurnaceBlock.f_48683_) ? (Direction)old.m_61143_((Property)FurnaceBlock.f_48683_) : Direction.NORTH;
            if (!((Boolean)old.m_61143_((Property)FurnaceBlock.f_48684_)).booleanValue()) {
                this.mimesis.m_9236_().m_7731_(pos, (BlockState)((BlockState)old.m_61124_((Property)FurnaceBlock.f_48683_, (Comparable)facing)).m_61124_((Property)FurnaceBlock.f_48684_, (Comparable)Boolean.valueOf(true)), 3);
            }
        }
        furnace.m_6596_();
        this.mimesis.setCookingStation(pos);
    }

    private boolean isCookingStationPresent(BlockPos pos) {
        return this.isCraftingTable(pos) || this.isFurnaceBlock(pos);
    }

    private boolean isCraftingTable(BlockPos pos) {
        return this.mimesis.m_9236_().m_8055_(pos).m_60734_() == Blocks.f_50091_;
    }

    private boolean isFurnaceBlock(BlockPos pos) {
        return this.mimesis.m_9236_().m_8055_(pos).m_60734_() == Blocks.f_50094_;
    }

    private BlockPos chooseNearestStation(BlockPos table, BlockPos furnace) {
        if (table != null && furnace != null) {
            double dt = this.mimesis.m_20275_((double)table.m_123341_() + 0.5, (double)table.m_123342_() + 0.5, (double)table.m_123343_() + 0.5);
            double df = this.mimesis.m_20275_((double)furnace.m_123341_() + 0.5, (double)furnace.m_123342_() + 0.5, (double)furnace.m_123343_() + 0.5);
            return df < dt ? furnace : table;
        }
        return table != null ? table : furnace;
    }

    private boolean canPlaceTableAt(BlockPos pos) {
        return this.mimesis.m_9236_().m_8055_(pos).m_60795_() && !this.mimesis.m_9236_().m_8055_(pos.m_7495_()).m_60795_() && this.mimesis.m_9236_().m_8055_(pos.m_7494_()).m_60795_();
    }

    private BlockPos findBreakableBlockNear(int radius) {
        BlockPos center = this.mimesis.m_20183_();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; ++x) {
            for (int y = 0; y <= 2; ++y) {
                for (int z = -radius; z <= radius; ++z) {
                    double d;
                    BlockPos pos = center.m_7918_(x, y, z);
                    BlockState state = this.mimesis.m_9236_().m_8055_(pos);
                    if (state.m_60795_() || !this.isSoftBreakable(state) || pos.m_123342_() < center.m_123342_() || !((d = this.mimesis.m_20275_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5)) < bestDist)) continue;
                    bestDist = d;
                    best = pos;
                }
            }
        }
        return best;
    }

    private boolean isSoftBreakable(BlockState state) {
        Block b = state.m_60734_();
        float hardness = state.m_60800_((BlockGetter)this.mimesis.m_9236_(), BlockPos.m_274561_((double)this.mimesis.m_20185_(), (double)this.mimesis.m_20186_(), (double)this.mimesis.m_20189_()));
        if (hardness < 0.0f) {
            return false;
        }
        if (hardness <= 3.5f) {
            return b == Blocks.f_50493_ || b == Blocks.f_50440_ || b == Blocks.f_49992_ || b == Blocks.f_49994_ || b == Blocks.f_50652_ || b == Blocks.f_50705_ || b == Blocks.f_50741_ || b == Blocks.f_50742_ || b == Blocks.f_49999_ || b == Blocks.f_50000_ || b == Blocks.f_50001_ || b == Blocks.f_50050_ || b == Blocks.f_50051_ || b == Blocks.f_50052_ || b == Blocks.f_50033_ || b == Blocks.f_50191_ || b == Blocks.f_50359_ || b == Blocks.f_50125_;
        }
        return b == Blocks.f_49997_ || b == Blocks.f_152469_ || b == Blocks.f_49996_ || b == Blocks.f_152468_ || b == Blocks.f_152505_ || b == Blocks.f_152506_ || b == Blocks.f_49995_ || b == Blocks.f_152467_ || b == Blocks.f_50089_ || b == Blocks.f_152474_ || b == Blocks.f_50264_ || b == Blocks.f_152479_ || b == Blocks.f_50059_ || b == Blocks.f_152472_ || b == Blocks.f_50173_ || b == Blocks.f_152473_;
    }

    private boolean isOreBlock(BlockState state) {
        Block b = state.m_60734_();
        return b == Blocks.f_49997_ || b == Blocks.f_152469_ || b == Blocks.f_49996_ || b == Blocks.f_152468_ || b == Blocks.f_152505_ || b == Blocks.f_152506_ || b == Blocks.f_49995_ || b == Blocks.f_152467_ || b == Blocks.f_50089_ || b == Blocks.f_152474_ || b == Blocks.f_50264_ || b == Blocks.f_152479_ || b == Blocks.f_50059_ || b == Blocks.f_152472_ || b == Blocks.f_50173_ || b == Blocks.f_152473_;
    }

    private void equipFurnaceAndFuel() {
        this.mimesis.clearHeldItems();
    }

    private void equipIronPickaxe() {
        ItemStack held = this.mimesis.m_21205_();
        if (held.m_41619_() || held.m_41720_() != Items.f_42385_) {
            this.mimesis.m_21008_(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)Items.f_42385_));
        }
    }

    private BlockPos findNearestCraftingTable(int radius) {
        BlockPos center = this.mimesis.m_20183_();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; ++x) {
            for (int y = -2; y <= 2; ++y) {
                for (int z = -radius; z <= radius; ++z) {
                    double d;
                    BlockPos pos = center.m_7918_(x, y, z);
                    if (this.mimesis.m_9236_().m_8055_(pos).m_60734_() != Blocks.f_50091_ || !((d = this.mimesis.m_20275_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5)) < bestDist)) continue;
                    bestDist = d;
                    best = pos;
                }
            }
        }
        return best;
    }

    private BlockPos findNearestFurnace(int radius) {
        BlockPos center = this.mimesis.m_20183_();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; ++x) {
            for (int y = -2; y <= 2; ++y) {
                for (int z = -radius; z <= radius; ++z) {
                    double d;
                    BlockPos pos = center.m_7918_(x, y, z);
                    if (this.mimesis.m_9236_().m_8055_(pos).m_60734_() != Blocks.f_50094_ || !((d = this.mimesis.m_20275_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5)) < bestDist)) continue;
                    bestDist = d;
                    best = pos;
                }
            }
        }
        return best;
    }

    private void placeCraftingTableStationNearSelf() {
        BlockPos[] candidates;
        BlockPos base = this.mimesis.m_20183_();
        for (BlockPos pos : candidates = new BlockPos[]{base.m_122012_(), base.m_122019_(), base.m_122029_(), base.m_122024_(), base.m_122012_().m_122029_(), base.m_122012_().m_122024_(), base.m_122019_().m_122029_(), base.m_122019_().m_122024_()}) {
            if (!this.canPlaceTableAt(pos)) continue;
            this.mimesis.m_9236_().m_7731_(pos, Blocks.f_50091_.m_49966_(), 3);
            this.mimesis.clearHeldItems();
            this.destination = new Vec3((double)pos.m_123341_() + 0.5, (double)pos.m_123342_(), (double)pos.m_123343_() + 0.5);
            return;
        }
    }

    private BlockPos findNearestOreBlock(int radius) {
        BlockPos center = this.mimesis.m_20183_();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; ++x) {
            for (int y = -3; y <= 3; ++y) {
                for (int z = -radius; z <= radius; ++z) {
                    double d;
                    BlockPos pos = center.m_7918_(x, y, z);
                    BlockState state = this.mimesis.m_9236_().m_8055_(pos);
                    if (!this.isOreBlock(state) || pos.m_123342_() < center.m_123342_() || !this.hasLineOfSightTo(pos) || !((d = this.mimesis.m_20275_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5)) < bestDist)) continue;
                    bestDist = d;
                    best = pos;
                }
            }
        }
        return best;
    }

    private boolean hasLineOfSightTo(BlockPos pos) {
        Vec3 start = this.mimesis.m_146892_();
        Vec3 end = Vec3.m_82512_((Vec3i)pos);
        BlockHitResult hit = this.mimesis.m_9236_().m_45547_(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)this.mimesis));
        return hit.m_6662_() == HitResult.Type.MISS || hit.m_82425_().equals((Object)pos);
    }

    private Vec3 findStandPositionNear(BlockPos target) {
        BlockPos[] candidates;
        for (BlockPos candidate : candidates = new BlockPos[]{target.m_122012_(), target.m_122019_(), target.m_122029_(), target.m_122024_()}) {
            if (!this.canStandAt(candidate)) continue;
            return new Vec3((double)candidate.m_123341_() + 0.5, (double)candidate.m_123342_(), (double)candidate.m_123343_() + 0.5);
        }
        return new Vec3((double)target.m_123341_() + 0.5, (double)target.m_123342_(), (double)target.m_123343_() + 1.5);
    }

    private boolean canStandAt(BlockPos pos) {
        return this.mimesis.m_9236_().m_8055_(pos).m_60795_() && this.mimesis.m_9236_().m_8055_(pos.m_7494_()).m_60795_();
    }

    private void lookAtBlock(BlockPos pos) {
        if (pos == null) {
            return;
        }
        BlockState state = this.mimesis.m_9236_().m_8055_(pos);
        boolean stationIsCraftOrFurnace = state.m_60734_() == Blocks.f_50091_ || state.m_60734_() instanceof FurnaceBlock;
        double tx = (double)pos.m_123341_() + 0.5;
        double tz = (double)pos.m_123343_() + 0.5;
        double ty = (double)pos.m_123342_() + 0.15;
        if (stationIsCraftOrFurnace) {
            double eyeX = this.mimesis.m_20185_();
            double eyeY = this.mimesis.m_20188_();
            double dx = tx - eyeX;
            double eyeZ = this.mimesis.m_20189_();
            double dz = tz - eyeZ;
            double horiz = Math.sqrt(dx * dx + dz * dz);
            if (horiz < 0.1) {
                horiz = 0.1;
            }
            if (horiz <= 6.0) {
                ty = (double)pos.m_123342_() - 1.0;
            } else {
                double vertical = -1.2 * horiz;
                ty = eyeY + vertical;
            }
            double minY = (double)pos.m_123342_() - 3.0;
            if (ty < minY) {
                ty = minY;
            }
        }
        this.mimesis.m_21563_().m_24950_(tx, ty, tz, 10.0f, 10.0f);
    }

    private void equipDiamondSword() {
        ItemStack held = this.mimesis.m_21205_();
        if (held.m_41619_() || !held.m_41720_().equals(Items.f_42388_)) {
            this.mimesis.m_21008_(InteractionHand.MAIN_HAND, new ItemStack((ItemLike)Items.f_42388_));
        }
    }

    private double addMovementError(double base) {
        double jitter = (this.mimesis.m_217043_().m_188500_() - 0.5) * 0.18;
        return Math.max(0.28, base + jitter);
    }

    private Vec3 snapToGround(double x, double z, double refY) {
        int y = this.mimesis.m_9236_().m_6924_(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.m_274561_((double)x, (double)refY, (double)z).m_123341_(), BlockPos.m_274561_((double)x, (double)refY, (double)z).m_123343_());
        return new Vec3(x, (double)y, z);
    }

    private boolean isRecentlyVisited(Vec3 pos) {
        for (Vec3 old : this.recentDestinations) {
            if (!(old.m_82557_(pos) < 9.0)) continue;
            return true;
        }
        this.recentDestinations.addLast(pos);
        while (this.recentDestinations.size() > 10) {
            this.recentDestinations.removeFirst();
        }
        return false;
    }

    private Player getObservedPlayer() {
        Player assigned;
        if (this.mimesis.getTargetPlayerUUID() != null && (assigned = this.mimesis.m_9236_().m_46003_(this.mimesis.getTargetPlayerUUID())) != null && assigned.m_6084_()) {
            return assigned;
        }
        double searchRange = this.mimesis.isHostile() ? 256.0 : 96.0;
        return this.mimesis.m_9236_().m_45924_(this.mimesis.m_20185_(), this.mimesis.m_20186_(), this.mimesis.m_20189_(), searchRange, false);
    }

    public void m_8041_() {
        this.mimesis.m_21573_().m_26573_();
        this.mimesis.m_6858_(false);
        this.mimesis.m_20260_(false);
        this.clearBreaking();
        this.state = State.OBSERVE;
        this.stateTicks = 0;
        this.pauseTicks = 0;
        this.hesitationTicks = 0;
        this.repathTicks = 0;
        this.lookDriftTicks = 0;
        this.stuckTicks = 0;
        this.reactionDelayTicks = 0;
        this.attackCooldown = 0;
        this.attackWindup = 0;
        this.worldActionTicks = 0;
        this.observeMoveTicks = 0;
        this.destination = null;
        this.lastPos = null;
        this.recentDestinations.clear();
    }

    private static enum State {
        OBSERVE,
        FOLLOW,
        WANDER,
        INTERACT,
        IMITATE,
        CRAFT,
        MINE,
        ATTACK;

    }
}

