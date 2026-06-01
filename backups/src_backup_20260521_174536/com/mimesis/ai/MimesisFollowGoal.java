/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.ai.goal.Goal
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.phys.Vec3
 */
package com.mimesis.ai;

import com.mimesis.entity.MimesisEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class MimesisFollowGoal
extends Goal {
    private final MimesisEntity mimesis;
    private final double followDistance;
    private final double explorationRange;
    private int randomWalkTimer = 0;
    private Vec3 randomTarget = null;
    private BlockPos randomTargetBlock = null;
    private int sprintTimer = 0;
    private int sprintDuration = 0;

    public MimesisFollowGoal(MimesisEntity mimesis) {
        this.mimesis = mimesis;
        this.followDistance = 64.0;
        this.explorationRange = 50.0;
    }

    public boolean m_8036_() {
        Player target = this.getTargetPlayer();
        return target != null;
    }

    public void m_8037_() {
        Player target = this.getTargetPlayer();
        if (target == null) {
            return;
        }
        double distance = this.mimesis.m_20270_((Entity)target);
        if (distance > this.explorationRange) {
            this.moveToward(target.m_20185_(), target.m_20189_(), 1.1);
            this.mimesis.m_6858_(true);
            if (this.mimesis.m_20096_() && this.mimesis.m_217043_().m_188503_(5) == 0) {
                this.makeJump();
            }
        } else if (distance > 25.0) {
            this.exploreAroundPlayer(target);
            this.randomizeSpeed(0.9, 1.0);
            this.mimesis.m_6858_(false);
        } else {
            BlockPos nearbyOre = this.findNearestOre();
            if (nearbyOre != null) {
                double oreDist = this.mimesis.m_20183_().m_123331_((Vec3i)nearbyOre);
                if (oreDist > 36.0) {
                    this.moveToward((double)nearbyOre.m_123341_() + 0.5, (double)nearbyOre.m_123343_() + 0.5, 1.0);
                    if (this.mimesis.m_217043_().m_188503_(10) == 0) {
                        this.mimesis.m_6858_(true);
                        if (this.mimesis.m_20096_()) {
                            this.makeJump();
                        }
                    }
                }
            } else {
                this.exploreAroundPlayer(target);
                this.randomizeSpeed(0.85, 0.95);
                this.mimesis.m_6858_(false);
            }
            --this.sprintTimer;
            if (this.sprintTimer <= 0) {
                this.sprintDuration = 15 + this.mimesis.m_217043_().m_188503_(25);
                this.sprintTimer = 100 + this.mimesis.m_217043_().m_188503_(150);
                if (this.mimesis.m_217043_().m_188503_(8) == 0) {
                    this.mimesis.m_6858_(true);
                }
            } else if (this.sprintDuration > 0) {
                --this.sprintDuration;
                this.mimesis.m_6858_(true);
                if (this.mimesis.m_20096_() && this.mimesis.m_217043_().m_188503_(4) == 0) {
                    this.makeJump();
                }
            } else {
                this.mimesis.m_6858_(false);
            }
        }
        if (this.mimesis.m_217043_().m_188503_(15) < 2) {
            this.mimesis.m_21563_().m_24946_(target.m_20185_() + (this.mimesis.m_217043_().m_188500_() - 0.5) * 10.0, target.m_20188_(), target.m_20189_() + (this.mimesis.m_217043_().m_188500_() - 0.5) * 10.0);
        }
    }

    private void makeJump() {
        this.mimesis.m_20334_(this.mimesis.m_20184_().f_82479_, 0.42, this.mimesis.m_20184_().f_82481_);
    }

    private void exploreAroundPlayer(Player target) {
        --this.randomWalkTimer;
        if (this.randomWalkTimer <= 0) {
            double angle = this.mimesis.m_217043_().m_188500_() * Math.PI * 2.0;
            double range = 5.0 + this.mimesis.m_217043_().m_188500_() * 8.0;
            double targetX = target.m_20185_() + Math.cos(angle) * range;
            double targetZ = target.m_20189_() + Math.sin(angle) * range;
            int groundY = this.mimesis.m_9236_().m_6924_(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.m_274561_((double)targetX, (double)target.m_20186_(), (double)targetZ).m_123341_(), BlockPos.m_274561_((double)targetX, (double)target.m_20186_(), (double)targetZ).m_123343_());
            this.randomTarget = new Vec3(targetX, (double)groundY, targetZ);
            this.randomTargetBlock = BlockPos.m_274446_((Position)this.randomTarget);
            this.randomWalkTimer = 120 + this.mimesis.m_217043_().m_188503_(180);
        }
        if (this.randomTarget != null && this.randomTargetBlock != null) {
            if (!this.mimesis.m_21573_().m_26571_()) {
                return;
            }
            this.mimesis.m_21573_().m_26519_(this.randomTarget.f_82479_, this.randomTarget.f_82480_, this.randomTarget.f_82481_, 0.8);
        }
    }

    private void moveToward(double x, double z, double speed) {
        int groundY = this.mimesis.m_9236_().m_6924_(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.m_274561_((double)x, (double)this.mimesis.m_20186_(), (double)z).m_123341_(), BlockPos.m_274561_((double)x, (double)this.mimesis.m_20186_(), (double)z).m_123343_());
        this.mimesis.m_21573_().m_26519_(x, (double)groundY, z, speed);
    }

    private void randomizeSpeed(double min, double max) {
        double baseSpeed = min + (max - min) * this.mimesis.m_217043_().m_188500_();
    }

    private BlockPos findNearestOre() {
        BlockPos pos = this.mimesis.m_20183_();
        BlockPos nearest = null;
        double nearestDist = 144.0;
        for (int x = -12; x <= 12; ++x) {
            for (int y = -3; y <= 3; ++y) {
                for (int z = -12; z <= 12; ++z) {
                    double dist;
                    BlockPos checkPos = pos.m_7918_(x, y, z);
                    Block block = this.mimesis.m_9236_().m_8055_(checkPos).m_60734_();
                    if (!this.isValuableOre(block) || !((dist = pos.m_123331_((Vec3i)checkPos)) < nearestDist)) continue;
                    nearestDist = dist;
                    nearest = checkPos;
                }
            }
        }
        return nearest;
    }

    private boolean isValuableOre(Block block) {
        return block == Blocks.f_49997_ || block == Blocks.f_152469_ || block == Blocks.f_49996_ || block == Blocks.f_152468_ || block == Blocks.f_152505_ || block == Blocks.f_152506_ || block == Blocks.f_49995_ || block == Blocks.f_152467_ || block == Blocks.f_50089_ || block == Blocks.f_152474_ || block == Blocks.f_50264_ || block == Blocks.f_152479_ || block == Blocks.f_50059_ || block == Blocks.f_152472_;
    }

    public void m_8041_() {
        this.mimesis.m_21573_().m_26573_();
        this.mimesis.m_6858_(false);
        this.randomWalkTimer = 0;
        this.randomTarget = null;
        this.randomTargetBlock = null;
    }

    private Player getTargetPlayer() {
        return this.mimesis.m_9236_().m_45924_(this.mimesis.m_20185_(), this.mimesis.m_20186_(), this.mimesis.m_20189_(), this.followDistance, false);
    }
}

