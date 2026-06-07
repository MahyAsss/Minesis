package com.minesis.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.minesis.entity.MinesisEntity;

public class MinesisFollowGoal extends Goal {
    private final MinesisEntity minesis;
    private final double followDistance;
    private final double explorationRange;
    private int randomWalkTimer = 0;
    private Vec3 randomTarget = null;
    private BlockPos randomTargetBlock = null;
    private int sprintTimer = 0;
    private int sprintDuration = 0;

    public MinesisFollowGoal(MinesisEntity minesis) {
        this.minesis = minesis;
        this.followDistance = 64.0D;
        this.explorationRange = 50.0D;
    }

    @Override
    public boolean canUse() {
        Player target = this.getTargetPlayer();
        return target != null;
    }

    @Override
    public void tick() {
        Player target = this.getTargetPlayer();
        if (target == null) {
            return;
        }

        double distance = this.minesis.distanceTo(target);

        // If too far, move closer faster
        if (distance > this.explorationRange) {
            this.moveToward(target.getX(), target.getZ(), 1.1D);
            this.minesis.setSprinting(true);
            // Jump while sprinting to traverse obstacles
            if (this.minesis.onGround() && this.minesis.getRandom().nextInt(5) == 0) {
                this.makeJump();
            }
        } 
        // If moderately far, walk normally but explore
        else if (distance > 25.0D) {
            // Explore around player area
            this.exploreAroundPlayer(target);
            this.randomizeSpeed(0.9D, 1.0D);
            this.minesis.setSprinting(false);
        }
        // If close enough, explore and interact
        else {
            // Look for nearby ores to break
            BlockPos nearbyOre = this.findNearestOre();
            if (nearbyOre != null) {
                double oreDist = this.minesis.blockPosition().distSqr(nearbyOre);
                if (oreDist > 36) { // 6 blocks away
                    this.moveToward(nearbyOre.getX() + 0.5D, nearbyOre.getZ() + 0.5D, 1.0D);
                    // Very rarely sprint toward ores (reduced from 1/3 to 1/10)
                    if (this.minesis.getRandom().nextInt(10) == 0) {
                        this.minesis.setSprinting(true);
                        if (this.minesis.onGround()) {
                            this.makeJump();
                        }
                    }
                }
            } else {
                // Explore around player
                this.exploreAroundPlayer(target);
                this.randomizeSpeed(0.85D, 0.95D);
                this.minesis.setSprinting(false);
            }

            // Much less frequent sprinting (was too often)
            this.sprintTimer--;
            if (this.sprintTimer <= 0) {
                this.sprintDuration = 15 + this.minesis.getRandom().nextInt(25); // Shorter sprints
                this.sprintTimer = 100 + this.minesis.getRandom().nextInt(150); // Longer wait between sprints
                if (this.minesis.getRandom().nextInt(8) == 0) { // 1 in 8 chance instead of 1 in 3
                    this.minesis.setSprinting(true);
                }
            } else if (this.sprintDuration > 0) {
                this.sprintDuration--;
                this.minesis.setSprinting(true);
                // Jump while sprinting
                if (this.minesis.onGround() && this.minesis.getRandom().nextInt(4) == 0) {
                    this.makeJump();
                }
            } else {
                this.minesis.setSprinting(false);
            }
        }

        // Occasionally look around
        if (this.minesis.getRandom().nextInt(15) < 2) {
            this.minesis.getLookControl().setLookAt(
                target.getX() + (this.minesis.getRandom().nextDouble() - 0.5) * 10,
                target.getEyeY(),
                target.getZ() + (this.minesis.getRandom().nextDouble() - 0.5) * 10);
        }
    }

    private void makeJump() {
        // Make the entity jump by modifying velocity
        this.minesis.setDeltaMovement(
            this.minesis.getDeltaMovement().x,
            0.42D, // Minecraft jump height
            this.minesis.getDeltaMovement().z
        );
    }

    private void exploreAroundPlayer(Player target) {
        this.randomWalkTimer--;
        if (this.randomWalkTimer <= 0) {
            // Create exploration targets in much narrower radius - less wandering
            double angle = this.minesis.getRandom().nextDouble() * Math.PI * 2;
            double range = 5.0D + this.minesis.getRandom().nextDouble() * 8.0D; // Reduced from 20-50 to 5-13
            double targetX = target.getX() + Math.cos(angle) * range;
            double targetZ = target.getZ() + Math.sin(angle) * range;
            int groundY = this.minesis.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    BlockPos.containing(targetX, target.getY(), targetZ).getX(),
                    BlockPos.containing(targetX, target.getY(), targetZ).getZ());
            this.randomTarget = new Vec3(
                targetX,
                groundY,
                targetZ
            );
            this.randomTargetBlock = BlockPos.containing(this.randomTarget);
            this.randomWalkTimer = 120 + this.minesis.getRandom().nextInt(180); // Longer stays in one place
        }

        if (this.randomTarget != null && this.randomTargetBlock != null) {
            if (!this.minesis.getNavigation().isDone()) {
                return;
            }

            this.minesis.getNavigation().moveTo(
                this.randomTarget.x,
                this.randomTarget.y,
                this.randomTarget.z,
                0.8D
            );
        }
    }

    private void moveToward(double x, double z, double speed) {
        int groundY = this.minesis.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(x, this.minesis.getY(), z).getX(),
                BlockPos.containing(x, this.minesis.getY(), z).getZ());
        this.minesis.getNavigation().moveTo(x, groundY, z, speed);
    }

    private void randomizeSpeed(double min, double max) {
        double baseSpeed = min + (max - min) * this.minesis.getRandom().nextDouble();
        // Don't reset path constantly, just adjust
    }

    private BlockPos findNearestOre() {
        BlockPos pos = this.minesis.blockPosition();
        BlockPos nearest = null;
        double nearestDist = 144; // 12 blocks

        for (int x = -12; x <= 12; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -12; z <= 12; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    Block block = this.minesis.level().getBlockState(checkPos).getBlock();
                    
                    if (this.isValuableOre(block)) {
                        double dist = pos.distSqr(checkPos);
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

    private boolean isValuableOre(Block block) {
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

    @Override
    public void stop() {
        this.minesis.getNavigation().stop();
        this.minesis.setSprinting(false);
        this.randomWalkTimer = 0;
        this.randomTarget = null;
        this.randomTargetBlock = null;
    }

    private Player getTargetPlayer() {
        return this.minesis.level().getNearestPlayer(
                this.minesis.getX(),
                this.minesis.getY(),
                this.minesis.getZ(),
                this.followDistance,
                false);
    }
}
