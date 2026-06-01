package com.mimesis.ai;

import com.mimesis.entity.MimesisEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;

/**
 * Unstable player-like behavior state machine.
 */
public class MimesisPlayerLikeMovementGoal extends Goal {
    private enum State {
        OBSERVE,
        FOLLOW,
        WANDER,
        INTERACT,
        IMITATE,
        CRAFT,
        MINE,
        ATTACK
    }

    private final MimesisEntity mimesis;
    private final Deque<Vec3> recentDestinations = new ArrayDeque<>();

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
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.getObservedPlayer() != null
                && !this.mimesis.isHostile()
                && !this.mimesis.isProvokedByPlayer();
    }

    @Override
    public boolean canContinueToUse() {
        return this.getObservedPlayer() != null
                && !this.mimesis.isHostile()
                && !this.mimesis.isProvokedByPlayer();
    }

    @Override
    public void start() {
        this.state = this.findNearestOreBlock(14) != null ? State.MINE : State.CRAFT;
        this.stateTicks = 120;
        this.pauseTicks = 0;
        this.hesitationTicks = 0;
        this.reactionDelayTicks = 8 + this.mimesis.getRandom().nextInt(10);
        this.observeMoveTicks = 0;
    }

    @Override
    public void tick() {
        Player observed = this.getObservedPlayer();
        if (observed == null) {
            return;
        }

        if (this.mimesis.isHostile() || this.mimesis.isProvokedByPlayer()) {
            return;
        }

        this.stateTicks--;
        this.repathTicks--;
        this.lookDriftTicks--;
        this.attackCooldown--;

        if (this.shouldSwitchState(observed)) {
            this.switchState(observed);
        }

        switch (this.state) {
            case OBSERVE -> this.tickObserve(observed);
            case FOLLOW -> this.tickFollow(observed);
            case WANDER -> this.tickWander(observed);
            case INTERACT -> this.tickInteract(observed);
            case IMITATE -> this.tickImitate(observed);
            case CRAFT -> this.tickCraft(observed);
            case MINE -> this.tickMine(observed);
            case ATTACK -> this.tickAttack(observed);
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
        if (this.observeMoveTicks <= 0 && this.mimesis.getRandom().nextInt(100) < 35) {
            this.observeMoveTicks = 12 + this.mimesis.getRandom().nextInt(28);
            this.destination = this.findWanderDestination(observed);
            this.repathTicks = 8 + this.mimesis.getRandom().nextInt(12);
        }

        if (this.observeMoveTicks > 0) {
            if (this.destination == null || this.repathTicks <= 0 || this.mimesis.getNavigation().isDone()) {
                this.destination = this.findWanderDestination(observed);
                this.repathTicks = 8 + this.mimesis.getRandom().nextInt(14);
            }
            this.moveToDestination(this.addMovementError(0.52D), false);
            this.observeMoveTicks--;
        } else {
            this.mimesis.getNavigation().stop();
        }

        this.mimesis.setSprinting(false);
        this.mimesis.setShiftKeyDown(false);
        this.slowLook(observed, this.mimesis.getRandom().nextInt(100) < 35);

        if (this.mimesis.getRandom().nextInt(100) < 5) {
            this.pauseTicks = 15 + this.mimesis.getRandom().nextInt(55);
        }
    }

    private void tickFollow(Player observed) {
        if (this.destination == null || this.repathTicks <= 0 || this.mimesis.getNavigation().isDone()) {
            this.destination = this.computeImperfectFollowDestination(observed);
            this.repathTicks = 10 + this.mimesis.getRandom().nextInt(20);
        }

        boolean sprint = this.mimesis.distanceTo(observed) > 14.0D && this.mimesis.getRandom().nextInt(100) < 65;
        double speed = sprint ? 0.92D : 0.70D;
        speed = this.addMovementError(speed);

        this.moveToDestination(speed, sprint);
        this.slowLook(observed, this.mimesis.getRandom().nextInt(100) < 25);

        if (this.mimesis.getRandom().nextInt(100) < 10) {
            this.pauseTicks = 5 + this.mimesis.getRandom().nextInt(15);
        }
    }

    private void tickWander(Player observed) {
        if (this.destination == null || this.repathTicks <= 0 || this.mimesis.getNavigation().isDone()) {
            this.destination = this.findWanderDestination(observed);
            this.repathTicks = 12 + this.mimesis.getRandom().nextInt(26);
        }

        double speed = this.addMovementError(0.58D + this.mimesis.getRandom().nextDouble() * 0.22D);
        this.moveToDestination(speed, false);

        if (this.mimesis.getRandom().nextInt(100) < 40) {
            this.slowLook(observed, true);
        } else {
            this.lookTowardPath();
        }

        if (this.mimesis.getRandom().nextInt(100) < 8) {
            this.pauseTicks = 10 + this.mimesis.getRandom().nextInt(30);
        }
    }

    private void tickInteract(Player observed) {
        this.worldActionTicks++;

        if (this.destination == null || this.repathTicks <= 0 || this.mimesis.getNavigation().isDone()) {
            this.destination = this.findInteractiveDestination(observed);
            this.repathTicks = 10 + this.mimesis.getRandom().nextInt(20);
        }

        double speed = this.addMovementError(0.60D);
        this.moveToDestination(speed, false);
        this.lookTowardPath();

        if (this.destination != null && this.mimesis.distanceToSqr(this.destination) < 9.0D) {
            this.tryDoorOrChestInteraction();
            this.tickSlowBreaking();
            if ((this.worldActionTicks % 25) == 0 && this.mimesis.getRandom().nextInt(100) < 25) {
                this.placeOccasionalBlock();
            }
            if (this.mimesis.getRandom().nextInt(100) < 20) {
                this.abortCurrentAction();
            }
        } else {
            this.clearBreaking();
        }
    }

    private void tickCraft(Player observed) {
        this.worldActionTicks++;
        this.mimesis.getNavigation().stop();
        this.mimesis.setSprinting(false);
        this.mimesis.setShiftKeyDown(false);
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
        boolean stationIsFurnace = station != null && this.isFurnaceBlock(station);

        if (station == null) {
            station = this.chooseNearestStation(table, furnace);
            stationIsFurnace = station != null && this.isFurnaceBlock(station);
        }

        if (station == null && this.worldActionTicks % 20 == 0 && this.mimesis.getRandom().nextInt(100) < 70) {
            // Place either a crafting table or a furnace near self (50/50)
            if (this.mimesis.getRandom().nextBoolean()) {
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
            this.moveToDestination(this.addMovementError(1.00D), false);
            this.lookTowardPath();
            return;
        }

        this.lookAtBlock(station);
        double distance = this.mimesis.distanceToSqr(station.getX() + 0.5D, station.getY() + 0.5D, station.getZ() + 0.5D);
            if (distance > 6.25D) {
            Vec3 stand = this.findStandPositionNear(station);
            this.destination = stand;
            this.repathTicks = 6 + this.mimesis.getRandom().nextInt(10);
            this.moveToDestination(this.addMovementError(1.00D), false);
        } else {
            this.destination = null;
        }

        if (stationIsFurnace) {
            this.updateCookingStationContents(station);
        }

        if (this.mimesis.level().getBrightness(LightLayer.BLOCK, this.mimesis.blockPosition()) <= 2) {
            this.placeTorchOccasionally();
        }
    }

    private void tickMine(Player observed) {
        this.worldActionTicks++;
        this.equipIronPickaxe();

        BlockPos ore = this.findNearestOreBlock(14);
        if (ore == null) {
            this.mimesis.getNavigation().stop();
            this.enterState(State.CRAFT, 120);
            return;
        }

        this.destination = new Vec3(ore.getX() + 0.5D, ore.getY(), ore.getZ() + 0.5D);
        this.repathTicks = 4 + this.mimesis.getRandom().nextInt(8);
        this.moveToDestination(this.addMovementError(1.00D), false);
        this.mimesis.getLookControl().setLookAt(
                ore.getX() + 0.5D,
                ore.getY() + 0.5D,
                ore.getZ() + 0.5D,
                12.0F,
                12.0F);

        if (this.mimesis.distanceToSqr(ore.getX() + 0.5D, ore.getY() + 0.5D, ore.getZ() + 0.5D) < 10.0D) {
            this.breakingPos = ore;
            if (this.breakingNeededTicks <= 0) {
                this.breakingNeededTicks = 24 + this.mimesis.getRandom().nextInt(24);
            }
            this.tickOreBreaking();
        } else {
            this.clearBreaking();
        }

        if (this.mimesis.level().getBrightness(LightLayer.BLOCK, this.mimesis.blockPosition()) <= 2) {
            this.placeTorchOccasionally();
        }
    }

    private void tickImitate(Player observed) {
        boolean playerSneak = observed.isShiftKeyDown();
        boolean playerSprint = observed.isSprinting();

        if (this.destination == null || this.repathTicks <= 0 || this.mimesis.getNavigation().isDone()) {
            Vec3 targetPos = observed.position();
            double side = this.mimesis.getRandom().nextBoolean() ? 1.0D : -1.0D;
            double angle = Math.atan2(observed.getZ() - this.mimesis.getZ(), observed.getX() - this.mimesis.getX());
            double px = targetPos.x + Math.cos(angle + side * 1.2D) * (2.0D + this.mimesis.getRandom().nextDouble() * 3.0D);
            double pz = targetPos.z + Math.sin(angle + side * 1.2D) * (2.0D + this.mimesis.getRandom().nextDouble() * 3.0D);
            this.destination = this.snapToGround(px, pz, observed.getY());
            this.repathTicks = 8 + this.mimesis.getRandom().nextInt(14);
        }

        double speed = playerSprint ? 0.80D : 0.58D;
        speed = this.addMovementError(speed);
        this.moveToDestination(speed, playerSprint && this.mimesis.getRandom().nextInt(100) < 50);
        this.mimesis.setShiftKeyDown(playerSneak && this.mimesis.getRandom().nextInt(100) < 70);

        if (this.mimesis.getRandom().nextInt(100) < 35) {
            this.slowLook(observed, true);
        } else {
            this.lookTowardPath();
        }

        if (this.mimesis.getRandom().nextInt(100) < 12) {
            this.pauseTicks = 8 + this.mimesis.getRandom().nextInt(18);
        }
    }

    private void tickAttack(Player observed) {
        double distance = this.mimesis.distanceTo(observed);
        this.mimesis.setTarget(observed);

        if (this.destination == null || this.repathTicks <= 0 || this.mimesis.getNavigation().isDone()) {
            // Rush directly when far, strafe only when close enough to fight.
            if (distance > 4.5D) {
                this.destination = new Vec3(observed.getX(), observed.getY(), observed.getZ());
                this.repathTicks = 2 + this.mimesis.getRandom().nextInt(3);
            } else {
                this.destination = this.computeImperfectAttackDestination(observed);
                this.repathTicks = 3 + this.mimesis.getRandom().nextInt(4);
            }
        }

        boolean sprint = distance > 3.5D;
        double speed = sprint ? 1.00D : 0.86D;
        speed = this.addMovementError(speed);

        this.moveToDestination(speed, sprint);
        this.slowLook(observed, false);

        this.equipDiamondSword();
        if (distance < 2.6D) {
            if (this.attackCooldown <= 0) {
                this.mimesis.swing(InteractionHand.MAIN_HAND);
                if (!this.mimesis.level().isClientSide) {
                    this.mimesis.level().broadcastEntityEvent(this.mimesis, (byte)4);
                }
                this.mimesis.doHurtTarget(observed);
                this.attackCooldown = 20;  // 20 ticks = 1 second
                this.pauseTicks = 0;
                if (!observed.isAlive() || observed.isDeadOrDying() || observed.getHealth() <= 0.0F) {
                    this.mimesis.discard();
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
        if (this.state == State.CRAFT) {
            BlockPos table = this.findNearestCraftingTable(16);
            if (table != null) {
                this.lookAtBlock(table);
                if (this.mimesis.distanceToSqr(table.getX() + 0.5D, table.getY() + 0.5D, table.getZ() + 0.5D) <= 9.0D) {
                    this.mimesis.getNavigation().stop();
                    this.destination = null;
                    this.stuckTicks = 0;
                    return;
                }
            }
        }

        Vec3 now = this.mimesis.position();
        if (this.lastPos != null && now.distanceTo(this.lastPos) < 0.08D) {
            this.stuckTicks++;
        } else {
            this.stuckTicks = 0;
        }
        this.lastPos = now;

        if (this.stuckTicks < 8) {
            return;
        }

        if (this.state == State.CRAFT) {
            this.mimesis.getNavigation().stop();
            BlockPos table = this.findNearestCraftingTable(10);
            if (table != null) {
                this.lookAtBlock(table);
            }
            this.stuckTicks = 0;
            return;
        }

        // Try to open door blocking
        if (this.tryOpenDoorNearSelf()) {
            this.stuckTicks = 0;
            return;
        }

        // Try to break soft obstacle
        if (this.breakSoftObstacleInFront()) {
            this.stuckTicks = 0;
            return;
        }

        // If Mimesis has no floor underfoot, jump and place a support block to escape the hole.
        if (this.tryEscapeHole()) {
            this.stuckTicks = 0;
            return;
        }

        // Place occasional block to escape
        if (this.mimesis.getRandom().nextInt(100) < 45) {
            this.placeOccasionalBlock();
        }

        // If still stuck after 15 ticks, teleport slightly forward
        if (this.stuckTicks >= 15) {
            Vec3 forward = this.mimesis.getLookAngle().scale(3.0D);
            Vec3 newPos = now.add(forward);
            this.destination = newPos;
            this.repathTicks = 0;
            this.stuckTicks = 0;
            return;
        }

        // Pick a new destination
        this.destination = this.findWanderDestination(observed);
        this.repathTicks = 0;
        this.pauseTicks = 4 + this.mimesis.getRandom().nextInt(12);
    }

    private void moveToDestination(double speed, boolean sprint) {
        if (this.destination == null) {
            return;
        }
        this.mimesis.getNavigation().moveTo(this.destination.x, this.destination.y, this.destination.z, speed);
        this.mimesis.setSprinting(sprint);
    }

    private void slowLook(Player observed, boolean addNoise) {
        if (this.lookDriftTicks <= 0) {
            this.lookDriftTicks = 12 + this.mimesis.getRandom().nextInt(24);
        }

        double x = observed.getX();
        double y = observed.getEyeY();
        double z = observed.getZ();

        if (addNoise) {
            x += (this.mimesis.getRandom().nextDouble() - 0.5D) * 2.2D;
            y += (this.mimesis.getRandom().nextDouble() - 0.5D) * 1.8D;
            z += (this.mimesis.getRandom().nextDouble() - 0.5D) * 2.2D;
        }

        this.mimesis.getLookControl().setLookAt(x, y, z, 9.0F, 10.0F);
    }

    private void lookTowardPath() {
        if (this.destination == null) {
            return;
        }

        Vec3 eye = this.mimesis.position();
        Vec3 dir = this.destination.subtract(eye);
        if (dir.lengthSqr() < 0.0001D) {
            return;
        }
        dir = dir.normalize();

        double upDown = 0.0D;
        int roll = this.mimesis.getRandom().nextInt(100);
        if (roll < 15) {
            upDown = 1.4D;
        } else if (roll < 30) {
            upDown = -1.2D;
        }

        this.mimesis.getLookControl().setLookAt(
                eye.x + dir.x * 4.0D,
                this.mimesis.getEyeY() + upDown,
                eye.z + dir.z * 4.0D,
                11.0F,
                11.0F);
    }

    private Vec3 computeImperfectFollowDestination(Player observed) {
        Vec3 playerPos = observed.position();
        double angle = Math.atan2(observed.getZ() - this.mimesis.getZ(), observed.getX() - this.mimesis.getX());
        double offsetAngle = angle + (this.mimesis.getRandom().nextDouble() - 0.5D) * 1.6D;
        double range = 2.5D + this.mimesis.getRandom().nextDouble() * 5.5D;

        double x = playerPos.x - Math.cos(offsetAngle) * range;
        double z = playerPos.z - Math.sin(offsetAngle) * range;
        Vec3 snapped = this.snapToGround(x, z, observed.getY());

        if (this.isRecentlyVisited(snapped)) {
            x += (this.mimesis.getRandom().nextDouble() - 0.5D) * 4.0D;
            z += (this.mimesis.getRandom().nextDouble() - 0.5D) * 4.0D;
            snapped = this.snapToGround(x, z, observed.getY());
        }

        return snapped;
    }

    private Vec3 computeImperfectAttackDestination(Player observed) {
        Vec3 playerPos = observed.position();
        double angle = Math.atan2(observed.getZ() - this.mimesis.getZ(), observed.getX() - this.mimesis.getX());
        double side = this.mimesis.getRandom().nextBoolean() ? 1.0D : -1.0D;
        double jitter = (this.mimesis.getRandom().nextDouble() - 0.5D) * 0.5D;

        // Strafe around the player in a circle
        double strafeRadius = 1.2D + this.mimesis.getRandom().nextDouble() * 1.0D;
        double perpAngle = angle + side * 1.57079632679D;  // 90 degrees perpendicular
        
        double x = playerPos.x - Math.cos(angle) * strafeRadius + Math.cos(perpAngle) * 0.8D + jitter;
        double z = playerPos.z - Math.sin(angle) * strafeRadius + Math.sin(perpAngle) * 0.8D - jitter;

        return this.snapToGround(x, z, observed.getY());
    }

    private Vec3 findWanderDestination(Player observed) {
        for (int i = 0; i < 10; i++) {
            double angle = this.mimesis.getRandom().nextDouble() * Math.PI * 2.0D;
            double range = 4.0D + this.mimesis.getRandom().nextDouble() * 14.0D;
            double x = this.mimesis.getX() + Math.cos(angle) * range;
            double z = this.mimesis.getZ() + Math.sin(angle) * range;

            Vec3 p = this.snapToGround(x, z, observed.getY());
            if (!this.isRecentlyVisited(p)) {
                return p;
            }
        }

        return this.snapToGround(
                observed.getX() + (this.mimesis.getRandom().nextDouble() - 0.5D) * 10.0D,
                observed.getZ() + (this.mimesis.getRandom().nextDouble() - 0.5D) * 10.0D,
                observed.getY());
    }

    private Vec3 findInteractiveDestination(Player observed) {
        BlockPos center = BlockPos.containing(this.mimesis.getX(), this.mimesis.getY(), this.mimesis.getZ());
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int x = -12; x <= 12; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -12; z <= 12; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = this.mimesis.level().getBlockState(pos);
                    Block block = state.getBlock();
                    if (!(block instanceof DoorBlock) && !(block instanceof ChestBlock) && block != Blocks.CRAFTING_TABLE) {
                        continue;
                    }
                    double d = this.mimesis.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
                    if (d < bestDist) {
                        bestDist = d;
                        best = pos;
                    }
                }
            }
        }

        if (best != null) {
            return new Vec3(best.getX() + 0.5D, best.getY(), best.getZ() + 0.5D);
        }

        return this.findWanderDestination(observed);
    }

    private void tryDoorOrChestInteraction() {
        BlockPos base = this.mimesis.blockPosition();

        if (this.mimesis.getRandom().nextInt(100) < 60) {
            this.tryOpenCloseDoor(base);
            this.tryOpenCloseDoor(base.north());
            this.tryOpenCloseDoor(base.south());
            this.tryOpenCloseDoor(base.east());
            this.tryOpenCloseDoor(base.west());
            this.tryOpenCloseDoor(base.above());
            this.tryOpenCloseDoor(base.below());
        }

        if (this.mimesis.getRandom().nextInt(100) < 30) {
            BlockPos chest = this.findNearestChest(8);
            if (chest != null) {
                this.mimesis.level().playSound(null, chest, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.6F, 1.0F);
                if (this.mimesis.getRandom().nextInt(100) < 35) {
                    this.mimesis.level().playSound(null, chest, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.55F, 1.0F);
                }
            }
        }
    }

    private boolean tryOpenDoorNearSelf() {
        BlockPos base = this.mimesis.blockPosition();
        return this.tryOpenCloseDoor(base)
                || this.tryOpenCloseDoor(base.north())
                || this.tryOpenCloseDoor(base.south())
                || this.tryOpenCloseDoor(base.east())
                || this.tryOpenCloseDoor(base.west())
                || this.tryOpenCloseDoor(base.above())
                || this.tryOpenCloseDoor(base.below());
    }

    private boolean tryOpenCloseDoor(BlockPos pos) {
        BlockState state = this.mimesis.level().getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock) || !state.hasProperty(DoorBlock.OPEN)) {
            return false;
        }

        BlockPos lower = pos;
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            lower = pos.below();
            state = this.mimesis.level().getBlockState(lower);
        }

        if (!(state.getBlock() instanceof DoorBlock) || !state.hasProperty(DoorBlock.OPEN)) {
            return false;
        }

        boolean currentlyOpen = state.getValue(DoorBlock.OPEN);
        boolean nextOpen;
        if (!currentlyOpen) {
            nextOpen = true;
        } else {
            nextOpen = this.mimesis.getRandom().nextInt(100) < 28 ? false : true;
        }

        if (nextOpen == currentlyOpen) {
            return false;
        }

        this.mimesis.level().setBlock(lower, state.setValue(DoorBlock.OPEN, nextOpen), 10);
        BlockPos upper = lower.above();
        BlockState upperState = this.mimesis.level().getBlockState(upper);
        if (upperState.getBlock() instanceof DoorBlock && upperState.hasProperty(DoorBlock.OPEN)) {
            this.mimesis.level().setBlock(upper, upperState.setValue(DoorBlock.OPEN, nextOpen), 10);
        }

        this.mimesis.level().playSound(
                null,
                lower,
                nextOpen ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE,
                SoundSource.BLOCKS,
                0.65F,
                1.0F);
        return true;
    }

    private BlockPos findNearestChest(int radius) {
        BlockPos center = this.mimesis.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = this.mimesis.level().getBlockState(pos);
                    if (!(state.getBlock() instanceof ChestBlock)) {
                        continue;
                    }
                    double d = this.mimesis.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
                    if (d < bestDist) {
                        bestDist = d;
                        best = pos;
                    }
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
            this.breakingNeededTicks = 18 + this.mimesis.getRandom().nextInt(45);
        }

        if (this.mimesis.getRandom().nextInt(100) < 12) {
            this.abortCurrentAction();
            return;
        }

        BlockState state = this.mimesis.level().getBlockState(this.breakingPos);
        if (state.isAir() || !this.isSoftBreakable(state)) {
            this.clearBreaking();
            return;
        }

        this.breakingProgressTicks++;
        int stage = Math.min(9, (this.breakingProgressTicks * 10) / Math.max(1, this.breakingNeededTicks));
        this.mimesis.level().destroyBlockProgress(this.mimesis.getId(), this.breakingPos, stage);

        if (this.breakingProgressTicks >= this.breakingNeededTicks) {
            this.mimesis.swing(InteractionHand.MAIN_HAND);
            if (!this.mimesis.level().isClientSide) {
                this.mimesis.level().broadcastEntityEvent(this.mimesis, (byte)4);
            }
            this.mimesis.level().destroyBlock(this.breakingPos, true);
            this.mimesis.collectNearbyItems(this.breakingPos);
            this.mimesis.level().destroyBlockProgress(this.mimesis.getId(), this.breakingPos, -1);
            this.clearBreaking();
        }
    }

    private void tickOreBreaking() {
        if (this.breakingPos == null) {
            return;
        }

        BlockState state = this.mimesis.level().getBlockState(this.breakingPos);
        if (state.isAir() || !this.isOreBlock(state)) {
            this.clearBreaking();
            return;
        }

        if (this.breakingPos.getY() < this.mimesis.blockPosition().getY()) {
            this.clearBreaking();
            this.tryEscapeHole();
            return;
        }

        this.breakingProgressTicks++;
        int stage = Math.min(9, (this.breakingProgressTicks * 10) / Math.max(1, this.breakingNeededTicks));
        this.mimesis.level().destroyBlockProgress(this.mimesis.getId(), this.breakingPos, stage);

        if (this.breakingProgressTicks >= this.breakingNeededTicks) {
            this.mimesis.swing(InteractionHand.MAIN_HAND);
            if (!this.mimesis.level().isClientSide) {
                this.mimesis.level().broadcastEntityEvent(this.mimesis, (byte)4);
            }
            this.mimesis.level().destroyBlock(this.breakingPos, true);
            this.mimesis.collectNearbyItems(this.breakingPos);
            this.mimesis.level().destroyBlockProgress(this.mimesis.getId(), this.breakingPos, -1);
            this.clearBreaking();
        }
    }

    private void clearBreaking() {
        if (this.breakingPos != null) {
            this.mimesis.level().destroyBlockProgress(this.mimesis.getId(), this.breakingPos, -1);
        }
        this.breakingPos = null;
        this.breakingProgressTicks = 0;
        this.breakingNeededTicks = 0;
    }

    private void abortCurrentAction() {
        this.clearBreaking();
        this.mimesis.getNavigation().stop();
        this.pauseTicks = 8 + this.mimesis.getRandom().nextInt(18);
    }

    private boolean breakSoftObstacleInFront() {
        Vec3 look = this.mimesis.getLookAngle();
        int sx = (int) Math.signum(look.x);
        int sz = (int) Math.signum(look.z);
        if (sx == 0 && sz == 0) {
            sx = 1;
        }
        // Avoid diagonal placements that create incoherent bridges: prefer a cardinal direction
        if (sx != 0 && sz != 0) {
            if (this.mimesis.getRandom().nextBoolean()) {
                sz = 0;
            } else {
                sx = 0;
            }
        }

        BlockPos base = this.mimesis.blockPosition();
        for (int i = 1; i <= 2; i++) {
            BlockPos p = base.offset(sx * i, 0, sz * i);
            BlockState s = this.mimesis.level().getBlockState(p);
            if (!s.isAir() && this.isSoftBreakable(s)) {
                this.mimesis.level().destroyBlock(p, true);
                return true;
            }
        }

        return false;
    }

    private void placeOccasionalBlock() {
        BlockPos base = this.mimesis.blockPosition();
        Vec3 look = this.mimesis.getLookAngle();
        int sx = (int) Math.signum(look.x);
        int sz = (int) Math.signum(look.z);
        if (sx == 0 && sz == 0) {
            sx = 1;
        }
        if (sx != 0 && sz != 0) {
            if (this.mimesis.getRandom().nextBoolean()) {
                sx = 0;
            } else {
                sz = 0;
            }
        }

        BlockPos place = base.offset(sx, -1, sz);
        BlockPos above = place.above();

        // If the intended placement is simple (air above solid/non-fluid), place normally
        BlockState belowState = this.mimesis.level().getBlockState(place.below());
        if (this.mimesis.level().getBlockState(place).isAir() && this.mimesis.level().getBlockState(above).isAir() && belowState.getFluidState().isEmpty()) {
            this.mimesis.level().setBlock(place, Blocks.COBBLESTONE.defaultBlockState(), 3);
            this.mimesis.swing(InteractionHand.MAIN_HAND);
            if (!this.mimesis.level().isClientSide) {
                this.mimesis.level().broadcastEntityEvent(this.mimesis, (byte)4);
            }
            return;
        }

        // If there's water below, attempt to place through the water by finding the first non-fluid block below
        if (!belowState.getFluidState().isEmpty()) {
            BlockPos scan = place.below();
            int maxDepth = 6; // don't scan infinitely deep
            while (maxDepth-- > 0 && scan.getY() > this.mimesis.level().getMinBuildHeight()) {
                BlockState s = this.mimesis.level().getBlockState(scan);
                if (s.getFluidState().isEmpty() && !s.isAir()) {
                    BlockPos target = scan.above();
                    BlockState targetState = this.mimesis.level().getBlockState(target);
                    if (targetState.isAir()) {
                        this.mimesis.level().setBlock(target, Blocks.COBBLESTONE.defaultBlockState(), 3);
                        this.mimesis.swing(InteractionHand.MAIN_HAND);
                        if (!this.mimesis.level().isClientSide) {
                            this.mimesis.level().broadcastEntityEvent(this.mimesis, (byte)4);
                        }
                        return;
                    }
                }
                scan = scan.below();
            }
        }
    }

    private boolean tryEscapeHole() {
        BlockPos below = this.mimesis.blockPosition().below();
        BlockState belowState = this.mimesis.level().getBlockState(below);

        if (!belowState.isAir()) {
            return false;
        }

        this.mimesis.getNavigation().stop();
        this.mimesis.setSprinting(false);
        this.mimesis.getJumpControl().jump();
        this.mimesis.level().setBlock(below, Blocks.COBBLESTONE.defaultBlockState(), 3);
        this.mimesis.swing(InteractionHand.MAIN_HAND);
        if (!this.mimesis.level().isClientSide) {
            this.mimesis.level().broadcastEntityEvent(this.mimesis, (byte)4);
        }
        this.destination = new Vec3(below.getX() + 0.5D, below.getY() + 1.0D, below.getZ() + 0.5D);
        this.pauseTicks = 0;
        return true;
    }

    private void placeTorchOccasionally() {
        if (this.mimesis.getRandom().nextInt(100) >= 14) {
            return;
        }

        BlockPos base = this.mimesis.blockPosition();
        BlockPos[] candidates = new BlockPos[] {
                base.north(),
                base.south(),
                base.east(),
                base.west(),
                base.above()
        };

        for (BlockPos pos : candidates) {
            BlockState state = this.mimesis.level().getBlockState(pos);
            if (!state.isAir()) {
                continue;
            }

            BlockState belowState = this.mimesis.level().getBlockState(pos.below());
            if (belowState.isAir()) {
                continue;
            }

            if (!belowState.isFaceSturdy(this.mimesis.level(), pos.below(), Direction.UP)) {
                continue;
            }

            if (!belowState.getFluidState().isEmpty()) {
                continue;
            }

            this.mimesis.level().setBlock(pos, Blocks.TORCH.defaultBlockState(), 3);
            this.mimesis.swing(InteractionHand.MAIN_HAND);
            if (!this.mimesis.level().isClientSide) {
                this.mimesis.level().broadcastEntityEvent(this.mimesis, (byte)4);
            }
            return;
        }
    }

    private void placeFurnaceStationNearSelf() {
        BlockPos base = this.mimesis.blockPosition();
        BlockPos[] candidates = new BlockPos[] {
                base.north(),
                base.south(),
                base.east(),
                base.west(),
                base.north().east(),
                base.north().west(),
                base.south().east(),
                base.south().west()
        };

        for (BlockPos pos : candidates) {
            if (this.canPlaceTableAt(pos)) {
            double dx = this.mimesis.getX() - (pos.getX() + 0.5D);
            double dz = this.mimesis.getZ() - (pos.getZ() + 0.5D);
            Direction facing = Math.abs(dx) > Math.abs(dz)
                ? (dx > 0.0D ? Direction.EAST : Direction.WEST)
                : (dz > 0.0D ? Direction.SOUTH : Direction.NORTH);

            this.mimesis.level().setBlock(
                pos,
                Blocks.FURNACE.defaultBlockState()
                    .setValue(FurnaceBlock.FACING, facing)
                    .setValue(FurnaceBlock.LIT, true),
                3);
                this.configureFurnaceStation(pos);
            this.mimesis.clearHeldItems();
                this.destination = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                return;
            }
        }
    }

    private void configureFurnaceStation(BlockPos pos) {
        if (!(this.mimesis.level().getBlockEntity(pos) instanceof FurnaceBlockEntity furnace)) {
            return;
        }

        // Keep station visually active but empty: no smelting input/fuel/output.
        for (int i = 0; i < 3; i++) {
            furnace.setItem(i, ItemStack.EMPTY);
        }
        furnace.setChanged();
        this.mimesis.setCookingStation(pos);
    }

    private void updateCookingStationContents(BlockPos pos) {
        if (!(this.mimesis.level().getBlockEntity(pos) instanceof FurnaceBlockEntity furnace)) {
            return;
        }

        // In survival, do not delete player resources: drop only furnace contents and keep the furnace block.
        for (int i = 0; i < 3; i++) {
            if (!furnace.getItem(i).isEmpty()) {
                for (int slot = 0; slot < 3; slot++) {
                    ItemStack stack = furnace.getItem(slot);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(
                                this.mimesis.level(),
                                pos.getX() + 0.5D,
                                pos.getY() + 0.5D,
                                pos.getZ() + 0.5D,
                                stack.copy());
                        furnace.setItem(slot, ItemStack.EMPTY);
                    }
                }
                furnace.setChanged();
                this.mimesis.setCookingStation(pos);
                return;
            }
        }

        // Force visual lit state each update so fire animation keeps running.
        BlockState old = this.mimesis.level().getBlockState(pos);
        if (old.getBlock() == Blocks.FURNACE) {
            Direction facing = old.hasProperty(FurnaceBlock.FACING) ? old.getValue(FurnaceBlock.FACING) : Direction.NORTH;
            if (!old.getValue(FurnaceBlock.LIT)) {
                this.mimesis.level().setBlock(pos,
                        old.setValue(FurnaceBlock.FACING, facing).setValue(FurnaceBlock.LIT, true),
                        3);
            }
        }

        furnace.setChanged();
        this.mimesis.setCookingStation(pos);
    }

    private boolean isCookingStationPresent(BlockPos pos) {
        return this.isCraftingTable(pos) || this.isFurnaceBlock(pos);
    }

    private boolean isCraftingTable(BlockPos pos) {
        return this.mimesis.level().getBlockState(pos).getBlock() == Blocks.CRAFTING_TABLE;
    }

    private boolean isFurnaceBlock(BlockPos pos) {
        return this.mimesis.level().getBlockState(pos).getBlock() == Blocks.FURNACE;
    }

    private BlockPos chooseNearestStation(BlockPos table, BlockPos furnace) {
        if (table != null && furnace != null) {
            double dt = this.mimesis.distanceToSqr(table.getX() + 0.5D, table.getY() + 0.5D, table.getZ() + 0.5D);
            double df = this.mimesis.distanceToSqr(furnace.getX() + 0.5D, furnace.getY() + 0.5D, furnace.getZ() + 0.5D);
            return df < dt ? furnace : table;
        }
        return table != null ? table : furnace;
    }

    private boolean canPlaceTableAt(BlockPos pos) {
        return this.mimesis.level().getBlockState(pos).isAir()
                && !this.mimesis.level().getBlockState(pos.below()).isAir()
                && this.mimesis.level().getBlockState(pos.above()).isAir();
    }

    private BlockPos findBreakableBlockNear(int radius) {
        BlockPos center = this.mimesis.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = this.mimesis.level().getBlockState(pos);
                    if (state.isAir() || !this.isSoftBreakable(state)) {
                        continue;
                    }
                    if (pos.getY() < center.getY()) {
                        continue;
                    }

                    double d = this.mimesis.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
                    if (d < bestDist) {
                        bestDist = d;
                        best = pos;
                    }
                }
            }
        }

        return best;
    }

    private boolean isSoftBreakable(BlockState state) {
        Block b = state.getBlock();
        float hardness = state.getDestroySpeed(this.mimesis.level(), BlockPos.containing(
                this.mimesis.getX(), this.mimesis.getY(), this.mimesis.getZ()));
        
        // Allow breaking of standard soft blocks and ores
        if (hardness < 0.0F) {
            return false;
        }

        // Soft blocks (hardness <= 3.5)
        if (hardness <= 3.5F) {
            return b == Blocks.DIRT
                    || b == Blocks.GRASS_BLOCK
                    || b == Blocks.SAND
                    || b == Blocks.GRAVEL
                    || b == Blocks.COBBLESTONE
                    || b == Blocks.OAK_PLANKS
                    || b == Blocks.SPRUCE_PLANKS
                    || b == Blocks.BIRCH_PLANKS
                    || b == Blocks.OAK_LOG
                    || b == Blocks.SPRUCE_LOG
                    || b == Blocks.BIRCH_LOG
                    || b == Blocks.OAK_LEAVES
                    || b == Blocks.SPRUCE_LEAVES
                    || b == Blocks.BIRCH_LEAVES
                    || b == Blocks.COBWEB
                    || b == Blocks.VINE
                    || b == Blocks.TALL_GRASS
                    || b == Blocks.SNOW;
        }

        // Ores (harder blocks that should still be breakable)
        return b == Blocks.COAL_ORE
                || b == Blocks.DEEPSLATE_COAL_ORE
                || b == Blocks.IRON_ORE
                || b == Blocks.DEEPSLATE_IRON_ORE
                || b == Blocks.COPPER_ORE
                || b == Blocks.DEEPSLATE_COPPER_ORE
                || b == Blocks.GOLD_ORE
                || b == Blocks.DEEPSLATE_GOLD_ORE
                || b == Blocks.DIAMOND_ORE
                || b == Blocks.DEEPSLATE_DIAMOND_ORE
                || b == Blocks.EMERALD_ORE
                || b == Blocks.DEEPSLATE_EMERALD_ORE
                || b == Blocks.LAPIS_ORE
                || b == Blocks.DEEPSLATE_LAPIS_ORE
                || b == Blocks.REDSTONE_ORE
                || b == Blocks.DEEPSLATE_REDSTONE_ORE;
    }

    private boolean isOreBlock(BlockState state) {
        Block b = state.getBlock();
        return b == Blocks.COAL_ORE
                || b == Blocks.DEEPSLATE_COAL_ORE
                || b == Blocks.IRON_ORE
                || b == Blocks.DEEPSLATE_IRON_ORE
                || b == Blocks.COPPER_ORE
                || b == Blocks.DEEPSLATE_COPPER_ORE
                || b == Blocks.GOLD_ORE
                || b == Blocks.DEEPSLATE_GOLD_ORE
                || b == Blocks.DIAMOND_ORE
                || b == Blocks.DEEPSLATE_DIAMOND_ORE
                || b == Blocks.EMERALD_ORE
                || b == Blocks.DEEPSLATE_EMERALD_ORE
                || b == Blocks.LAPIS_ORE
                || b == Blocks.DEEPSLATE_LAPIS_ORE
                || b == Blocks.REDSTONE_ORE
                || b == Blocks.DEEPSLATE_REDSTONE_ORE;
    }

    private void equipFurnaceAndFuel() {
        this.mimesis.clearHeldItems();
    }

    private void equipIronPickaxe() {
        ItemStack held = this.mimesis.getMainHandItem();
        if (held.isEmpty() || held.getItem() != Items.IRON_PICKAXE) {
            this.mimesis.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        }
    }

    private BlockPos findNearestCraftingTable(int radius) {
        BlockPos center = this.mimesis.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (this.mimesis.level().getBlockState(pos).getBlock() != Blocks.CRAFTING_TABLE) {
                        continue;
                    }
                    double d = this.mimesis.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
                    if (d < bestDist) {
                        bestDist = d;
                        best = pos;
                    }
                }
            }
        }

        return best;
    }

    private BlockPos findNearestFurnace(int radius) {
        BlockPos center = this.mimesis.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (this.mimesis.level().getBlockState(pos).getBlock() != Blocks.FURNACE) {
                        continue;
                    }
                    double d = this.mimesis.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
                    if (d < bestDist) {
                        bestDist = d;
                        best = pos;
                    }
                }
            }
        }

        return best;
    }

    private void placeCraftingTableStationNearSelf() {
        BlockPos base = this.mimesis.blockPosition();
        BlockPos[] candidates = new BlockPos[] {
                base.north(),
                base.south(),
                base.east(),
                base.west(),
                base.north().east(),
                base.north().west(),
                base.south().east(),
                base.south().west()
        };

        for (BlockPos pos : candidates) {
            if (this.canPlaceTableAt(pos)) {
                this.mimesis.level().setBlock(pos, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                this.mimesis.clearHeldItems();
                this.destination = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                return;
            }
        }
    }

    private BlockPos findNearestOreBlock(int radius) {
        BlockPos center = this.mimesis.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = this.mimesis.level().getBlockState(pos);
                    if (!this.isOreBlock(state)) {
                        continue;
                    }
                    if (pos.getY() < center.getY()) {
                        continue;
                    }
                    if (!this.hasLineOfSightTo(pos)) {
                        continue;
                    }
                    double d = this.mimesis.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
                    if (d < bestDist) {
                        bestDist = d;
                        best = pos;
                    }
                }
            }
        }

        return best;
    }

    private boolean hasLineOfSightTo(BlockPos pos) {
        Vec3 start = this.mimesis.getEyePosition();
        Vec3 end = Vec3.atCenterOf(pos);
        BlockHitResult hit = this.mimesis.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this.mimesis));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    private Vec3 findStandPositionNear(BlockPos target) {
        BlockPos[] candidates = new BlockPos[] {
                target.north(),
                target.south(),
                target.east(),
                target.west()
        };

        for (BlockPos candidate : candidates) {
            if (this.canStandAt(candidate)) {
                return new Vec3(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
            }
        }

        return new Vec3(target.getX() + 0.5D, target.getY(), target.getZ() + 1.5D);
    }

    private boolean canStandAt(BlockPos pos) {
        return this.mimesis.level().getBlockState(pos).isAir()
                && this.mimesis.level().getBlockState(pos.above()).isAir();
    }

    private void lookAtBlock(BlockPos pos) {
        if (pos == null) {
            return;
        }
        // If targeting a crafting table or a furnace, look up at ~45° between ground and forward
        BlockState state = this.mimesis.level().getBlockState(pos);
        boolean stationIsCraftOrFurnace = state.getBlock() == Blocks.CRAFTING_TABLE || state.getBlock() instanceof FurnaceBlock;
        double tx = pos.getX() + 0.5D;
        double tz = pos.getZ() + 0.5D;
        double ty = pos.getY() + 0.15D;

        if (stationIsCraftOrFurnace) {
            double eyeX = this.mimesis.getX();
            double eyeY = this.mimesis.getEyeY();
            double eyeZ = this.mimesis.getZ();
            double dx = tx - eyeX;
            double dz = tz - eyeZ;
            double horiz = Math.sqrt(dx * dx + dz * dz);
            if (horiz < 0.1D) {
                horiz = 0.1D;
            }
            // Make the entity look lower: when close, aim about 1 block below the station; when far, aim more steeply down
            if (horiz <= 6.0D) {
                ty = pos.getY() - 1.0D; // look one block below the block's base
            } else {
                // At distance, aim down more aggressively than 45°: vertical delta = -1.2 * horiz
                double vertical = -1.2D * horiz;
                ty = eyeY + vertical;
            }
            // Clamp: don't look more than 3 blocks below the block's Y
            double minY = pos.getY() - 3.0D;
            if (ty < minY) ty = minY;
        }

        this.mimesis.getLookControl().setLookAt(
                tx,
                ty,
                tz,
                10.0F,
                10.0F);
    }

    private void equipDiamondSword() {
        ItemStack held = this.mimesis.getMainHandItem();
        if (held.isEmpty() || !held.getItem().equals(Items.IRON_SWORD)) {
            this.mimesis.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        }
    }

    private double addMovementError(double base) {
        double jitter = (this.mimesis.getRandom().nextDouble() - 0.5D) * 0.18D;
        return Math.max(0.28D, base + jitter);
    }

    private Vec3 snapToGround(double x, double z, double refY) {
        int y = this.mimesis.level().getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(x, refY, z).getX(),
                BlockPos.containing(x, refY, z).getZ());
        return new Vec3(x, y, z);
    }

    private boolean isRecentlyVisited(Vec3 pos) {
        for (Vec3 old : this.recentDestinations) {
            if (old.distanceToSqr(pos) < 9.0D) {
                return true;
            }
        }

        this.recentDestinations.addLast(pos);
        while (this.recentDestinations.size() > 10) {
            this.recentDestinations.removeFirst();
        }

        return false;
    }

    private Player getObservedPlayer() {
        if (this.mimesis.getTargetPlayerUUID() != null) {
            Player assigned = this.mimesis.level().getPlayerByUUID(this.mimesis.getTargetPlayerUUID());
            if (assigned != null && assigned.isAlive()) {
                return assigned;
            }
        }

        // Increase detection range when hostile to 128 blocks (256.0D squared)
        double searchRange = this.mimesis.isHostile() ? 256.0D : 96.0D;

        return this.mimesis.level().getNearestPlayer(
                this.mimesis.getX(),
                this.mimesis.getY(),
                this.mimesis.getZ(),
                searchRange,
                false);
    }

    @Override
    public void stop() {
        this.mimesis.getNavigation().stop();
        this.mimesis.setSprinting(false);
        this.mimesis.setShiftKeyDown(false);
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
}
