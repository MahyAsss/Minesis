package com.minesis.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

import com.minesis.entity.MinesisEntity;

public class MinesisAttackGoal extends Goal {
    private final MinesisEntity minesis;
    private int attackCooldown = 0;
    private BlockPos chaseBreakPos = null;
    private int chaseBreakProgress = 0;
    private int chaseBreakNeededTicks = 0;
    private int blockedChaseTicks = 0;

    public MinesisAttackGoal(MinesisEntity minesis) {
        this.minesis = minesis;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.minesis.isHostile() || this.minesis.isProvokedByPlayer();
    }

    @Override
    public boolean canContinueToUse() {
        return this.minesis.isHostile() || this.minesis.isProvokedByPlayer();
    }

    @Override
    public void tick() {
        Player target = this.resolveTarget();

        if (target != null) {
            // Pause dramatique de transformation : ne rien faire pendant 2s
            if (this.minesis.isTransformationFreezing()) return;

            this.minesis.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ(), 14.0F, 14.0F);

            double distance = this.minesis.distanceTo(target);
            if (distance > 2.8D) {
                if (this.minesis.isHostile() && !this.minesis.hasLineOfSight(target)) {
                    this.blockedChaseTicks++;
                    boolean hasLineOfSight = this.canBuildBridgeToTarget(target);
                    if (hasLineOfSight) {
                        this.buildBridgeTowardTarget(target);
                    } else {
                        this.tickChaseBreaking(target);
                    }

                    if (this.blockedChaseTicks >= 100) {
                        this.minesis.teleportNearCurrentTargetLikeEnderman();
                        this.blockedChaseTicks = 0;
                        this.clearChaseBreaking();
                    }
                } else {
                    this.clearChaseBreaking();
                    this.blockedChaseTicks = 0;
                }

                Vec3 chase = this.computeChasePosition(target);
                this.minesis.getNavigation().moveTo(chase.x, chase.y, chase.z, this.minesis.isHostile() ? 1.35D : 1.05D);
                this.minesis.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), this.minesis.isHostile() ? 1.35D : 1.05D);
                this.minesis.setSprinting(distance > 6.0D);
            } else {
                this.clearChaseBreaking();
                this.blockedChaseTicks = 0;
                this.minesis.getNavigation().stop();
                this.minesis.setSprinting(false);
            }

            if (distance <= 2.8D && this.attackCooldown <= 0) {
                this.minesis.swing(InteractionHand.MAIN_HAND);
                if (!this.minesis.level().isClientSide) {
                    this.minesis.level().broadcastEntityEvent(this.minesis, (byte)4);
                }
                boolean hit = this.minesis.doHurtTarget(target);
                this.attackCooldown = 10;
                if (hit && !target.isAlive()) {
                    this.minesis.discard();
                }
            }

            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }

            this.minesis.setSprinting(distance > 6.0D);
        }
    }

    private void tickChaseBreaking(Player target) {
        if (this.chaseBreakPos == null) {
            BlockPos blocking = this.findBlockingBlockTowardTarget(target);
            if (blocking == null) return;

            // Ouvre portes/trappes/barrières au lieu de les briser
            if (this.tryOpenDoor(blocking)) return;

            this.chaseBreakPos = blocking;
            this.chaseBreakProgress = 0;
            this.chaseBreakNeededTicks = 0; // calculé après équipement de l'outil
        }

        BlockState state = this.minesis.level().getBlockState(this.chaseBreakPos);
        if (state.isAir() || !this.isChaseBreakable(state)) {
            this.clearChaseBreaking();
            return;
        }

        if (this.chaseBreakNeededTicks == 0) {
            this.chaseBreakNeededTicks = this.calcBreakTicks(this.chaseBreakPos);
        }

        this.chaseBreakProgress++;
        if (this.minesis.level() instanceof ServerLevel serverLevel) {
            int stage = Math.min(9, (this.chaseBreakProgress * 10) / Math.max(1, this.chaseBreakNeededTicks));
            serverLevel.destroyBlockProgress(this.minesis.getId(), this.chaseBreakPos, stage);
        }

        this.minesis.swing(InteractionHand.MAIN_HAND);
        if (!this.minesis.level().isClientSide) {
            this.minesis.level().broadcastEntityEvent(this.minesis, (byte)4);
        }

        if (this.chaseBreakProgress >= this.chaseBreakNeededTicks) {
            if (this.minesis.level() instanceof ServerLevel serverLevel) {
                serverLevel.destroyBlock(this.chaseBreakPos, true);
            } else {
                this.minesis.level().destroyBlock(this.chaseBreakPos, true);
            }
            this.minesis.collectNearbyItems(this.chaseBreakPos);
            if (this.minesis.level() instanceof ServerLevel serverLevel) {
                serverLevel.destroyBlockProgress(this.minesis.getId(), this.chaseBreakPos, -1);
            }
            this.clearChaseBreaking();
        }
    }

    /** Ouvre portes bois/trappes/barrières. Laisse passer les portes en fer (bris). */
    private boolean tryOpenDoor(BlockPos pos) {
        BlockState state = this.minesis.level().getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.IRON_DOOR || block == Blocks.IRON_TRAPDOOR) return false;
        if (!state.hasProperty(BlockStateProperties.OPEN)) return false;
        if (state.getValue(BlockStateProperties.OPEN)) return true; // déjà ouverte

        if (block instanceof DoorBlock doorBlock) {
            doorBlock.setOpen(this.minesis, this.minesis.level(), state, pos, true);
        } else {
            this.minesis.level().setBlock(pos, state.setValue(BlockStateProperties.OPEN, Boolean.TRUE), 10);
            this.minesis.level().playSound(null, pos,
                    block instanceof FenceGateBlock
                            ? SoundEvents.FENCE_GATE_OPEN
                            : SoundEvents.WOODEN_TRAPDOOR_OPEN,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return true;
    }

    /** Calcule les ticks nécessaires selon la dureté du bloc (vitesse équivalente outil en fer). */
    private int calcBreakTicks(BlockPos pos) {
        BlockState state = this.minesis.level().getBlockState(pos);
        float hardness = state.getDestroySpeed(this.minesis.level(), pos);
        if (hardness < 0) return 80;
        return Math.max(8, Math.min((int) (hardness * 40.0f / 4.0f), 80));
    }

    private BlockPos findBlockingBlockTowardTarget(Player target) {
        Vec3 start = this.minesis.getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 delta = end.subtract(start);
        double distance = delta.length();
        if (distance < 0.5D) {
            return null;
        }

        Vec3 step = delta.scale(1.0D / distance);
        BlockPos previous = null;
        int steps = Math.max(1, (int) Math.ceil(distance * 4.0D));

        int MAX_CHASE_BREAK_DISTANCE = 8; // only consider blocks near Minesis
        for (int i = 1; i < steps; i++) {
            Vec3 point = start.add(step.scale(i * 0.25D));
            BlockPos pos = BlockPos.containing(point.x, point.y, point.z);
            if (pos.equals(previous)) {
                continue;
            }
            previous = pos;

            if (pos.closerToCenterThan(this.minesis.position(), 1.2D)) {
                continue;
            }

            if (!pos.closerToCenterThan(this.minesis.position(), MAX_CHASE_BREAK_DISTANCE)) {
                continue;
            }

            if (pos.getY() < this.minesis.blockPosition().getY()) {
                continue;
            }

            BlockState state = this.minesis.level().getBlockState(pos);
            if (!state.isAir() && this.isChaseBreakable(state)) {
                return pos;
            }
        }

        return null;
    }

    private boolean isChaseBreakable(BlockState state) {
        Block block = state.getBlock();

        if (block == Blocks.BEDROCK || block == Blocks.BARRIER || block == Blocks.END_PORTAL_FRAME) {
            return false;
        }

        float hardness = state.getDestroySpeed(this.minesis.level(), this.minesis.blockPosition());
        if (hardness < 0.0F) {
            return false;
        }

        return hardness <= 5.0F
                || block == Blocks.COBWEB
                || block == Blocks.VINE
                || block == Blocks.OAK_LEAVES
                || block == Blocks.SPRUCE_LEAVES
                || block == Blocks.BIRCH_LEAVES
                || block == Blocks.JUNGLE_LEAVES
                || block == Blocks.ACACIA_LEAVES
                || block == Blocks.DARK_OAK_LEAVES
                || block == Blocks.MANGROVE_LEAVES;
    }

    private void clearChaseBreaking() {
        if (this.chaseBreakPos != null && this.minesis.level() instanceof ServerLevel serverLevel) {
            serverLevel.destroyBlockProgress(this.minesis.getId(), this.chaseBreakPos, -1);
        }
        this.chaseBreakPos = null;
        this.chaseBreakProgress = 0;
        this.chaseBreakNeededTicks = 0;
    }

    private Player resolveTarget() {
        Player target = this.minesis.getTarget() instanceof Player ? (Player) this.minesis.getTarget() : null;
        if (target != null && target.isAlive()) {
            this.minesis.setTarget(target);
            return target;
        }

        target = this.minesis.level().getNearestPlayer(
                this.minesis.getX(),
                this.minesis.getY(),
                this.minesis.getZ(),
                this.minesis.isHostile() ? 256.0D : 96.0D,
                false);

        if (target != null) {
            this.minesis.setTarget(target);
        }
        return target;
    }

    private boolean canBuildBridgeToTarget(Player target) {
        BlockPos from = this.minesis.blockPosition();
        BlockPos to = target.blockPosition();

        if (from.getY() >= to.getY() - 1 && from.getY() <= to.getY() + 1) {
            return false;
        }

        BlockState below = this.minesis.level().getBlockState(from.below());
        return !below.isAir();
    }

    private void buildBridgeTowardTarget(Player target) {
        BlockPos base = this.minesis.blockPosition();
        Vec3 towardTarget = target.position().subtract(this.minesis.position()).normalize();
        int sx = (int) Math.signum(towardTarget.x);
        int sz = (int) Math.signum(towardTarget.z);

        if (sx == 0 && sz == 0) {
            sx = 1;
        }

        BlockPos place = base.offset(sx, 0, sz);
        BlockState belowPlace = this.minesis.level().getBlockState(place.below());
        if (this.minesis.level().getBlockState(place).isAir()
                && belowPlace.canOcclude() && belowPlace.getFluidState().isEmpty()) {
            this.minesis.level().setBlock(place, Blocks.COBBLESTONE.defaultBlockState(), 3);
            this.minesis.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (!this.minesis.level().isClientSide) {
                this.minesis.level().broadcastEntityEvent(this.minesis, (byte)4);
            }
        }
    }

    private Vec3 computeChasePosition(Player target) {
        double dx = target.getX() - this.minesis.getX();
        double dz = target.getZ() - this.minesis.getZ();
        double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
        double backoff = this.minesis.isHostile() ? 1.0D : 0.6D;
        double x = target.getX() - (dx / length) * backoff;
        double z = target.getZ() - (dz / length) * backoff;
        BlockPos pos = BlockPos.containing(x, target.getY(), z);
        return new Vec3(pos.getX() + 0.5D, target.getY(), pos.getZ() + 0.5D);
    }

    @Override
    public void stop() {
        this.clearChaseBreaking();
        this.blockedChaseTicks = 0;
        if (!this.minesis.isHostile() && !this.minesis.isProvokedByPlayer()) {
            this.minesis.setTarget(null);
        }
        if (!this.minesis.isHostile()) {
            this.minesis.clearHeldItems();
        }
    }
}
