package com.mimesis.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

import com.mimesis.entity.MimesisEntity;

public class MimesisAttackGoal extends Goal {
    private final MimesisEntity mimesis;
    private int attackCooldown = 0;
    private BlockPos chaseBreakPos = null;
    private int chaseBreakProgress = 0;
    private int chaseBreakNeededTicks = 0;
    private int blockedChaseTicks = 0;

    public MimesisAttackGoal(MimesisEntity mimesis) {
        this.mimesis = mimesis;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.mimesis.isHostile() && !this.mimesis.isProvokedByPlayer()) {
            return false;
        }

        Player target = this.resolveTarget();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.mimesis.isHostile() && !this.mimesis.isProvokedByPlayer()) {
            return false;
        }
        Player target = this.resolveTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void tick() {
        Player target = this.resolveTarget();

        if (target != null) {
            this.mimesis.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ(), 14.0F, 14.0F);

            if (this.mimesis.isHostile()) {
                this.equipSword();
            }

            double distance = this.mimesis.distanceTo(target);
            if (distance > 2.8D) {
                if (this.mimesis.isHostile() && !this.mimesis.hasLineOfSight(target)) {
                    this.blockedChaseTicks++;
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
                this.mimesis.getNavigation().moveTo(chase.x, chase.y, chase.z, this.mimesis.isHostile() ? 1.35D : 1.05D);
                this.mimesis.setSprinting(distance > 6.0D);
            } else {
                this.clearChaseBreaking();
                this.blockedChaseTicks = 0;
                this.mimesis.getNavigation().stop();
                this.mimesis.setSprinting(false);
            }

            if (distance <= 2.8D && this.attackCooldown <= 0) {
                this.mimesis.swing(InteractionHand.MAIN_HAND);
                if (!this.mimesis.level().isClientSide) {
                    this.mimesis.level().broadcastEntityEvent(this.mimesis, (byte)4);
                }
                boolean hit = this.mimesis.doHurtTarget(target);
                this.attackCooldown = 10;
                if (hit && !target.isAlive()) {
                    this.mimesis.discard();
                }
            }

            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }

            this.mimesis.setSprinting(distance > 6.0D);
        }
    }

    private void tickChaseBreaking(Player target) {
        if (this.chaseBreakPos == null) {
            this.chaseBreakPos = this.findBlockingBlockTowardTarget(target);
            this.chaseBreakProgress = 0;
            this.chaseBreakNeededTicks = 18 + this.mimesis.getRandom().nextInt(24);
            if (this.chaseBreakPos == null) {
                return;
            }
        }

        BlockState state = this.mimesis.level().getBlockState(this.chaseBreakPos);
        if (state.isAir() || !this.isChaseBreakable(state)) {
            this.clearChaseBreaking();
            return;
        }

        this.chaseBreakProgress++;
        if (this.mimesis.level() instanceof ServerLevel serverLevel) {
            int stage = Math.min(9, (this.chaseBreakProgress * 10) / Math.max(1, this.chaseBreakNeededTicks));
            serverLevel.destroyBlockProgress(this.mimesis.getId(), this.chaseBreakPos, stage);
        }

        this.mimesis.swing(InteractionHand.MAIN_HAND);
        if (!this.mimesis.level().isClientSide) {
            this.mimesis.level().broadcastEntityEvent(this.mimesis, (byte)4);
        }

        if (this.chaseBreakProgress >= this.chaseBreakNeededTicks) {
            if (this.mimesis.level() instanceof ServerLevel serverLevel) {
                serverLevel.destroyBlock(this.chaseBreakPos, true);
            } else {
                this.mimesis.level().destroyBlock(this.chaseBreakPos, true);
            }
            // Collect drops into Mimesis carried inventory
            this.mimesis.collectNearbyItems(this.chaseBreakPos);
            if (this.mimesis.level() instanceof ServerLevel serverLevel) {
                serverLevel.destroyBlockProgress(this.mimesis.getId(), this.chaseBreakPos, -1);
            }
            this.clearChaseBreaking();
        }
    }

    private BlockPos findBlockingBlockTowardTarget(Player target) {
        Vec3 start = this.mimesis.getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 delta = end.subtract(start);
        double distance = delta.length();
        if (distance < 0.5D) {
            return null;
        }

        Vec3 step = delta.scale(1.0D / distance);
        BlockPos previous = null;
        int steps = Math.max(1, (int) Math.ceil(distance * 4.0D));

        int MAX_CHASE_BREAK_DISTANCE = 8; // only consider blocks near Mimesis
        for (int i = 1; i < steps; i++) {
            Vec3 point = start.add(step.scale(i * 0.25D));
            BlockPos pos = BlockPos.containing(point.x, point.y, point.z);
            if (pos.equals(previous)) {
                continue;
            }
            previous = pos;

            if (pos.closerToCenterThan(this.mimesis.position(), 1.2D)) {
                continue;
            }

            if (!pos.closerToCenterThan(this.mimesis.position(), MAX_CHASE_BREAK_DISTANCE)) {
                continue;
            }

            if (pos.getY() < this.mimesis.blockPosition().getY()) {
                continue;
            }

            BlockState state = this.mimesis.level().getBlockState(pos);
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

        float hardness = state.getDestroySpeed(this.mimesis.level(), this.mimesis.blockPosition());
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
        if (this.chaseBreakPos != null && this.mimesis.level() instanceof ServerLevel serverLevel) {
            serverLevel.destroyBlockProgress(this.mimesis.getId(), this.chaseBreakPos, -1);
        }
        this.chaseBreakPos = null;
        this.chaseBreakProgress = 0;
        this.chaseBreakNeededTicks = 0;
    }

    private Player resolveTarget() {
        Player target = null;
        // Priority 1: assigned target player by UUID (natural spawn target)
        if (this.mimesis.isHostile() && this.mimesis.getTargetPlayerUUID() != null) {
            Player p = this.mimesis.level().getPlayerByUUID(this.mimesis.getTargetPlayerUUID());
            if (p != null && p.isAlive()) target = p;
        }

        // Priority 2: explicit entity target set on the mob
        if (target == null && this.mimesis.getTarget() instanceof Player) {
            Player p = (Player) this.mimesis.getTarget();
            if (p != null && p.isAlive()) target = p;
        }

        // Priority 3: nearest player within range but exclude the appearance source (we don't attack the copier)
        if (target == null) {
            double range = this.mimesis.isHostile() ? 96.0D : 32.0D;
            java.util.UUID appearanceUuid = this.mimesis.getAppearancePlayerUUID();
            double bestDist = Double.MAX_VALUE;
            for (Player candidate : this.mimesis.level().players()) {
                if (candidate == null || !candidate.isAlive()) continue;
                if (appearanceUuid != null && appearanceUuid.equals(candidate.getUUID())) continue;
                double d = candidate.distanceToSqr(this.mimesis);
                if (d <= range * range && d < bestDist) {
                    bestDist = d;
                    target = candidate;
                }
            }
        }
        if (target != null) {
            this.mimesis.setTarget(target);
        }
        return target;
    }

    private boolean canBuildBridgeToTarget(Player target) {
        BlockPos from = this.mimesis.blockPosition();
        BlockPos to = target.blockPosition();

        if (from.getY() >= to.getY() - 1 && from.getY() <= to.getY() + 1) {
            return false;
        }

        BlockState below = this.mimesis.level().getBlockState(from.below());
        return !below.isAir();
    }

    private void buildBridgeTowardTarget(Player target) {
        BlockPos base = this.mimesis.blockPosition();
        Vec3 towardTarget = target.position().subtract(this.mimesis.position()).normalize();
        int sx = (int) Math.signum(towardTarget.x);
        int sz = (int) Math.signum(towardTarget.z);

        if (sx == 0 && sz == 0) {
            sx = 1;
        }

        BlockPos place = base.offset(sx, 0, sz);
        if (this.mimesis.level().getBlockState(place).isAir() && !this.mimesis.level().getBlockState(place.below()).isAir()) {
            this.mimesis.level().setBlock(place, Blocks.COBBLESTONE.defaultBlockState(), 3);
            this.mimesis.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (!this.mimesis.level().isClientSide) {
                this.mimesis.level().broadcastEntityEvent(this.mimesis, (byte)4);
            }
        }
    }

    private void equipSword() {
        ItemStack mainHand = this.mimesis.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!mainHand.getItem().toString().toLowerCase().contains("sword") || mainHand.getItem() != Items.DIAMOND_SWORD) {
            this.mimesis.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        }
    }

    private Vec3 computeChasePosition(Player target) {
        double dx = target.getX() - this.mimesis.getX();
        double dz = target.getZ() - this.mimesis.getZ();
        double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
        double backoff = this.mimesis.isHostile() ? 1.0D : 0.6D;
        double x = target.getX() - (dx / length) * backoff;
        double z = target.getZ() - (dz / length) * backoff;
        BlockPos pos = BlockPos.containing(x, target.getY(), z);
        return new Vec3(pos.getX() + 0.5D, target.getY(), pos.getZ() + 0.5D);
    }

    @Override
    public void stop() {
        this.clearChaseBreaking();
        this.blockedChaseTicks = 0;
        if (!this.mimesis.isHostile() && !this.mimesis.isProvokedByPlayer()) {
            this.mimesis.setTarget(null);
        }
        if (!this.mimesis.isHostile()) {
            this.mimesis.clearHeldItems();
        }
    }
}
