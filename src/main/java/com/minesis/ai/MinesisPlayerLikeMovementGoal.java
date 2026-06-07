package com.minesis.ai;

import com.minesis.entity.MinesisEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Human-like passive AI for MinesisEntity.
 *
 * States:
 *   IDLE            – stand still, look around naturally
 *   WANDER          – run/walk like a player, varied head movement
 *   EXPLORE         – stride toward a farther destination
 *   PAUSE           – simulate checking inventory / settings
 *   CHEST_INTERACT  – walk to a nearby chest, open/peek/close
 *   USE_CRAFTING    – walk to a nearby crafting table, simulate crafting
 *   PLACE_STATION   – place a crafting table or furnace, use it, break it
 *   CHOP            – chop a visible log
 *   MINE            – mine a visible ore
 *
 * Speed reference (entity MOVEMENT_SPEED = 0.28 blocks/tick):
 *   modifier 0.90 ≈ 5.0 blocks/s  → brisk player walk
 *   modifier 1.10 ≈ 6.2 blocks/s  → player sprint
 */
public class MinesisPlayerLikeMovementGoal extends Goal {

    private enum State {
        IDLE, WANDER, EXPLORE, PAUSE,
        CHEST_INTERACT, USE_CRAFTING, PLACE_STATION,
        CHOP, MINE
    }

    // ── Speed ────────────────────────────────────────────────────────────────
    private static final double SPEED_WALK   = 0.90D;
    private static final double SPEED_EXPL   = 0.96D;
    private static final double SPEED_TASK   = 0.92D;
    private static final double SPEED_SPRINT = 1.10D;

    // ── Head rotation (deg/tick) ──────────────────────────────────────────────
    private static final float YAW_SNAP = 50.0F;   // decisive glance
    private static final float PIT_SNAP = 42.0F;
    private static final float YAW_IDLE = 20.0F;   // slow scan
    private static final float PIT_IDLE = 16.0F;

    // ── State machine ────────────────────────────────────────────────────────
    private final MinesisEntity minesis;
    private final Deque<Vec3>   recentDests = new ArrayDeque<>();

    private State state      = State.IDLE;
    private int   stateTicks = 0;
    private int   repathTicks = 0;
    private int   lookTimer   = 0;
    private int   stuckTicks  = 0;

    // ── Sprint ───────────────────────────────────────────────────────────────
    private int sprintTicks    = 0;
    private int sprintCooldown = 0;

    // ── Item cycling ─────────────────────────────────────────────────────────
    private int         itemScrollTimer    = 0;
    private ItemStack[] itemTransitionSeq  = null;
    private int         itemTransitionIdx  = 0;
    private int         itemTransitionDelay = 0;

    // ── Player mirroring ─────────────────────────────────────────────────────
    private int mirrorTimer = 0;

    // ── Navigation ───────────────────────────────────────────────────────────
    private Vec3 destination    = null;
    private Vec3 lastPos        = null;
    private Vec3 idleLookTarget = null;

    // ── Block breaking (CHOP / MINE) ─────────────────────────────────────────
    private BlockPos breakingPos           = null;
    private int      breakingProgressTicks = 0;
    private int      breakingNeededTicks   = 0;
    private int      veinBlocksRemaining   = 0;
    private int      scaffoldCooldown      = 0;
    private int      scaffoldCount         = 0;

    // ── Chest interaction ────────────────────────────────────────────────────
    private BlockPos chestTarget    = null;
    private int      chestUseTimer  = 0;  // counts down while "looking inside"
    private boolean  chestOpened    = false;
    private int      chestCooldown  = 0;

    // ── Crafting table use ───────────────────────────────────────────────────
    private BlockPos craftTarget        = null;
    private int      craftUseTimer      = 0;
    private boolean  craftInteractSwung = false;

    // ── Place station (crafting table / furnace) ─────────────────────────────
    private BlockPos stationPlacePos      = null;  // candidate position (before placing)
    private BlockPos stationPos           = null;  // position of the placed block
    private int      stationPhase         = 0;     // 0=navigate 1=place 2=use 3=break
    private int      stationPhaseTimer    = 0;
    private boolean  stationInteractSwung = false;

    // ──────────────────────────────────────────────────────────────────────────

    public MinesisPlayerLikeMovementGoal(MinesisEntity minesis) {
        this.minesis = minesis;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse()           { return !minesis.isHostile() && !minesis.isProvokedByPlayer(); }
    @Override public boolean canContinueToUse() { return !minesis.isHostile() && !minesis.isProvokedByPlayer(); }

    @Override
    public void start() {
        sprintTicks    = 40 + minesis.getRandom().nextInt(60);
        sprintCooldown = 0;
        mirrorTimer    = 80 + minesis.getRandom().nextInt(80);
        chestCooldown  = 200 + minesis.getRandom().nextInt(200);
        enterState(State.IDLE, 30 + minesis.getRandom().nextInt(40));
    }

    @Override
    public void stop() {
        minesis.getNavigation().stop();
        minesis.setSprinting(false);
        minesis.setShiftKeyDown(false);
        clearBreaking();
        minesis.clearHeldItems();
        cleanupStation(true);
        if (chestOpened && chestTarget != null) {
            minesis.level().playSound(null, chestTarget,
                SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        destination = null; lastPos = null; idleLookTarget = null;
        recentDests.clear();
        resetAllTimers();
    }

    @Override
    public void tick() {
        if (minesis.isHostile() || minesis.isProvokedByPlayer()) return;

        stateTicks--;
        repathTicks--;
        lookTimer--;
        sprintCooldown--;
        mirrorTimer--;
        chestCooldown--;

        switch (state) {
            case IDLE            -> tickIdle();
            case WANDER          -> tickWander();
            case EXPLORE         -> tickExplore();
            case PAUSE           -> tickPause();
            case CHEST_INTERACT  -> tickChestInteract();
            case USE_CRAFTING    -> tickUseCrafting();
            case PLACE_STATION   -> tickPlaceStation();
            case CHOP            -> tickChop();
            case MINE            -> tickMine();
        }

        updateStuck();
        tickItemCycling();
        if (stateTicks <= 0) chooseNextState();

        if (mirrorTimer <= 0) {
            mirrorTimer = 80 + minesis.getRandom().nextInt(120);
            mirrorTargetPlayerBehavior();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATES
    // ══════════════════════════════════════════════════════════════════════════

    // ── IDLE ─────────────────────────────────────────────────────────────────

    private void tickIdle() {
        minesis.getNavigation().stop();
        minesis.setSprinting(false);
        minesis.setShiftKeyDown(false);
        if (lookTimer <= 0) {
            lookTimer = 18 + minesis.getRandom().nextInt(30);
            idleLookTarget = pickIdleLookTarget();
        }
        if (idleLookTarget != null)
            minesis.getLookControl().setLookAt(idleLookTarget.x, idleLookTarget.y, idleLookTarget.z, YAW_IDLE, PIT_IDLE);
    }

    private Vec3 pickIdleLookTarget() {
        double eyeY = minesis.getEyeY();
        int roll = minesis.getRandom().nextInt(6);
        double angle = minesis.getRandom().nextDouble() * Math.PI * 2.0D;
        return switch (roll) {
            case 0 -> new Vec3(minesis.getX() + Math.cos(angle) * 28.0D,
                               eyeY + (minesis.getRandom().nextDouble() - 0.4D) * 1.8D,
                               minesis.getZ() + Math.sin(angle) * 28.0D);
            case 1 -> new Vec3(minesis.getX() + (minesis.getRandom().nextDouble() - 0.5D),
                               eyeY - 1.7D,
                               minesis.getZ() + (minesis.getRandom().nextDouble() - 0.5D));
            case 2 -> findInterestingLookPoint();
            case 3 -> new Vec3(minesis.getX() + Math.cos(angle) * 2.0D,
                               eyeY + 2.5D,
                               minesis.getZ() + Math.sin(angle) * 2.0D);
            default -> new Vec3(minesis.getX() + Math.cos(angle) * (4 + minesis.getRandom().nextDouble() * 8),
                                eyeY + (minesis.getRandom().nextDouble() - 0.6D) * 2.5D,
                                minesis.getZ() + Math.sin(angle) * (4 + minesis.getRandom().nextDouble() * 8));
        };
    }

    // ── PAUSE (simulates inventory check) ─────────────────────────────────────

    private void tickPause() {
        minesis.getNavigation().stop();
        minesis.setSprinting(false);
        if (lookTimer <= 0) {
            if (minesis.getRandom().nextInt(4) < 3) {
                lookTimer = 18 + minesis.getRandom().nextInt(30);
                minesis.getLookControl().setLookAt(
                    minesis.getX() + (minesis.getRandom().nextDouble() - 0.5D) * 0.5D,
                    minesis.getY() - 1.3D,
                    minesis.getZ() + (minesis.getRandom().nextDouble() - 0.5D) * 0.5D,
                    YAW_IDLE, PIT_IDLE);
            } else {
                lookTimer = 8 + minesis.getRandom().nextInt(12);
                double a = minesis.getRandom().nextDouble() * Math.PI * 2;
                minesis.getLookControl().setLookAt(
                    minesis.getX() + Math.cos(a) * 8,
                    minesis.getEyeY() + (minesis.getRandom().nextDouble() - 0.3D) * 2,
                    minesis.getZ() + Math.sin(a) * 8,
                    YAW_SNAP, PIT_SNAP);
            }
        }
    }

    // ── WANDER ───────────────────────────────────────────────────────────────

    private void tickWander() {
        if (destination == null || repathTicks <= 0 || minesis.getNavigation().isDone()) {
            destination = findWanderDestination(8.0D, 20.0D);
            repathTicks = 8 + minesis.getRandom().nextInt(10);
        }

        // Sprint is default; short walk pauses break it up
        double speed;
        if (sprintTicks > 0) {
            sprintTicks--;
            speed = SPEED_SPRINT + (minesis.getRandom().nextDouble() - 0.5D) * 0.04D;
            minesis.setSprinting(true);
            if (minesis.onGround() && minesis.getRandom().nextInt(20) == 0)
                minesis.getJumpControl().jump();
            if (sprintTicks <= 0)
                sprintCooldown = 12 + minesis.getRandom().nextInt(30);
        } else if (sprintCooldown > 0) {
            speed = SPEED_WALK + (minesis.getRandom().nextDouble() - 0.5D) * 0.05D;
            minesis.setSprinting(false);
            if (sprintCooldown <= 0)
                sprintTicks = 40 + minesis.getRandom().nextInt(80);
        } else {
            sprintTicks = 40 + minesis.getRandom().nextInt(80);
            speed = SPEED_SPRINT;
            minesis.setSprinting(true);
        }

        minesis.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);

        if (lookTimer <= 0) {
            lookTimer = 4 + minesis.getRandom().nextInt(8);
            applyWalkingLook();
        }

        tryOpenDoorNearSelf();
    }

    /**
     * Realistic walking head movement:
     *   30% ground-check (1.5–3 blocks ahead, foot level)
     *   25% side glance (±30–65°)
     *   20% forward (4–8 blocks, slight vertical variation)
     *   15% distant horizon
     *   8%  interesting nearby block
     *   2%  upward glance (as if hearing something)
     */
    private void applyWalkingLook() {
        Vec3 fwd = destination != null
            ? destination.subtract(minesis.position()).normalize()
            : minesis.getLookAngle();

        int roll = minesis.getRandom().nextInt(100);
        if (roll < 30) {
            // Ground check ahead
            double d = 1.5D + minesis.getRandom().nextDouble() * 1.5D;
            minesis.getLookControl().setLookAt(
                minesis.getX() + fwd.x * d,
                minesis.getY() + (minesis.getRandom().nextDouble() - 0.5D) * 0.2D,
                minesis.getZ() + fwd.z * d,
                YAW_SNAP, PIT_SNAP);
        } else if (roll < 55) {
            // Side glance
            float sideYaw = (float)((minesis.getRandom().nextDouble() - 0.5D) * 1.15D);
            Vec3 side = fwd.yRot(sideYaw);
            minesis.getLookControl().setLookAt(
                minesis.getX() + side.x * 5.0D,
                minesis.getEyeY() + (minesis.getRandom().nextDouble() - 0.5D) * 1.2D,
                minesis.getZ() + side.z * 5.0D,
                YAW_SNAP, PIT_SNAP);
        } else if (roll < 75) {
            // Forward
            double d = 4.0D + minesis.getRandom().nextDouble() * 5.0D;
            minesis.getLookControl().setLookAt(
                minesis.getX() + fwd.x * d,
                minesis.getEyeY() + (minesis.getRandom().nextDouble() - 0.35D) * 1.5D,
                minesis.getZ() + fwd.z * d,
                YAW_SNAP, PIT_SNAP);
        } else if (roll < 90) {
            // Horizon
            float slight = (float)((minesis.getRandom().nextDouble() - 0.5D) * 0.8D);
            Vec3 hor = fwd.yRot(slight);
            double d = 14.0D + minesis.getRandom().nextDouble() * 14.0D;
            minesis.getLookControl().setLookAt(
                minesis.getX() + hor.x * d,
                minesis.getEyeY() + (minesis.getRandom().nextDouble() - 0.4D) * 1.0D,
                minesis.getZ() + hor.z * d,
                YAW_SNAP, PIT_SNAP);
        } else if (roll < 98) {
            Vec3 pt = findInterestingLookPoint();
            minesis.getLookControl().setLookAt(pt.x, pt.y, pt.z, YAW_SNAP, PIT_SNAP);
        } else {
            // Upward glance
            double a = minesis.getRandom().nextDouble() * Math.PI * 2;
            minesis.getLookControl().setLookAt(
                minesis.getX() + Math.cos(a) * 3,
                minesis.getEyeY() + 2.5D,
                minesis.getZ() + Math.sin(a) * 3,
                YAW_SNAP, PIT_SNAP);
        }
    }

    // ── EXPLORE ──────────────────────────────────────────────────────────────

    private void tickExplore() {
        if (destination == null) destination = findWanderDestination(20.0D, 40.0D);
        if (repathTicks <= 0 || minesis.getNavigation().isDone()) {
            minesis.getNavigation().moveTo(destination.x, destination.y, destination.z, SPEED_EXPL);
            repathTicks = 10 + minesis.getRandom().nextInt(14);
        }
        if (minesis.distanceToSqr(destination) < 9.0D) {
            enterState(State.IDLE, 25 + minesis.getRandom().nextInt(40));
            return;
        }
        if (lookTimer <= 0) { lookTimer = 4 + minesis.getRandom().nextInt(8); applyWalkingLook(); }
        tryOpenDoorNearSelf();
    }

    // ── CHEST INTERACT ───────────────────────────────────────────────────────

    private void tickChestInteract() {
        if (chestTarget == null) {
            chestTarget = findNearestContainer(20);
            if (chestTarget == null) { enterState(State.WANDER, 60); return; }
        }

        double distSq = minesis.distanceToSqr(chestTarget.getX() + 0.5, chestTarget.getY() + 0.5, chestTarget.getZ() + 0.5);

        if (distSq > 6.25D) {
            // Navigate toward chest
            if (repathTicks <= 0 || minesis.getNavigation().isDone()) {
                minesis.getNavigation().moveTo(chestTarget.getX() + 0.5, chestTarget.getY(), chestTarget.getZ() + 0.5, SPEED_WALK);
                repathTicks = 8 + minesis.getRandom().nextInt(10);
            }
            if (lookTimer <= 0) {
                lookTimer = 6 + minesis.getRandom().nextInt(8);
                minesis.getLookControl().setLookAt(chestTarget.getX() + 0.5, chestTarget.getY() + 0.5, chestTarget.getZ() + 0.5, YAW_SNAP, PIT_SNAP);
            }
            return;
        }

        // At chest
        minesis.getNavigation().stop();
        minesis.getLookControl().setLookAt(chestTarget.getX() + 0.5, chestTarget.getY() + 0.5, chestTarget.getZ() + 0.5, YAW_SNAP, PIT_SNAP);

        if (!chestOpened) {
            minesis.level().playSound(null, chestTarget, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, 0.9F + minesis.getRandom().nextFloat() * 0.2F);
            chestOpened = true;
            chestUseTimer = 25 + minesis.getRandom().nextInt(40); // 1.25–3.25 s
        }

        if (chestUseTimer > 0) {
            chestUseTimer--;
            // Swing arm occasionally to simulate rummaging
            if (chestUseTimer % 14 == 0) minesis.swing(InteractionHand.MAIN_HAND);
        } else {
            minesis.level().playSound(null, chestTarget, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 0.9F + minesis.getRandom().nextFloat() * 0.2F);
            chestOpened = false;
            chestTarget = null;
            chestCooldown = 400 + minesis.getRandom().nextInt(400);
            enterState(State.WANDER, 60 + minesis.getRandom().nextInt(80));
        }
    }

    // ── USE CRAFTING TABLE ────────────────────────────────────────────────────

    private void tickUseCrafting() {
        if (craftTarget == null) {
            craftTarget = findNearestCraftingTable(16);
            if (craftTarget == null) { enterState(State.WANDER, 60); return; }
        }

        double distSq = minesis.distanceToSqr(craftTarget.getX() + 0.5, craftTarget.getY() + 0.5, craftTarget.getZ() + 0.5);

        if (distSq > 6.25D) {
            if (repathTicks <= 0 || minesis.getNavigation().isDone()) {
                minesis.getNavigation().moveTo(craftTarget.getX() + 0.5, craftTarget.getY(), craftTarget.getZ() + 0.5, SPEED_WALK);
                repathTicks = 8 + minesis.getRandom().nextInt(10);
            }
            if (lookTimer <= 0) {
                lookTimer = 6;
                minesis.getLookControl().setLookAt(craftTarget.getX() + 0.5, craftTarget.getY() + 0.5, craftTarget.getZ() + 0.5, YAW_SNAP, PIT_SNAP);
            }
            return;
        }

        minesis.getNavigation().stop();
        minesis.getLookControl().setLookAt(craftTarget.getX() + 0.5, craftTarget.getY() + 0.2, craftTarget.getZ() + 0.5, YAW_SNAP, PIT_SNAP);

        if (craftUseTimer <= 0) {
            craftTarget = null;
            minesis.clearHeldItems();
            enterState(State.WANDER, 60 + minesis.getRandom().nextInt(80));
            return;
        }

        craftUseTimer--;
        if (!craftInteractSwung) {
            minesis.swing(InteractionHand.MAIN_HAND);
            craftInteractSwung = true;
        }
    }

    // ── PLACE STATION ─────────────────────────────────────────────────────────
    //  Phases: 0=find+navigate  1=place  2=use  3=break

    private void tickPlaceStation() {
        stationPhaseTimer--;

        switch (stationPhase) {

            case 0 -> { // Find a placement position and walk toward it
                if (stationPlacePos == null) {
                    stationPlacePos = findPlacementPos();
                    if (stationPlacePos == null) { enterState(State.WANDER, 60); return; }
                }
                double d = minesis.distanceToSqr(stationPlacePos.getX() + 0.5, stationPlacePos.getY(), stationPlacePos.getZ() + 0.5);
                if (d > 4.0D) {
                    if (repathTicks <= 0) {
                        minesis.getNavigation().moveTo(stationPlacePos.getX() + 0.5, stationPlacePos.getY(), stationPlacePos.getZ() + 0.5, SPEED_WALK);
                        repathTicks = 8;
                    }
                    if (lookTimer <= 0) {
                        lookTimer = 6;
                        minesis.getLookControl().setLookAt(stationPlacePos.getX() + 0.5, stationPlacePos.getY() + 0.5, stationPlacePos.getZ() + 0.5, YAW_SNAP, PIT_SNAP);
                    }
                } else {
                    minesis.getNavigation().stop();
                    minesis.getLookControl().setLookAt(stationPlacePos.getX() + 0.5, stationPlacePos.getY() + 0.5, stationPlacePos.getZ() + 0.5, YAW_SNAP, PIT_SNAP);
                    stationPhase = 1;
                    stationPhaseTimer = 8;
                }
            }

            case 1 -> { // Place block
                minesis.getNavigation().stop();
                if (stationPhaseTimer <= 0) {
                    if (stationPlacePos == null || !minesis.level().getBlockState(stationPlacePos).isAir()) {
                        stationPlacePos = null; stationPhase = 0; enterState(State.WANDER, 60); return;
                    }
                    BlockState placed = minesis.getRandom().nextInt(5) < 3
                        ? Blocks.CRAFTING_TABLE.defaultBlockState()
                        : Blocks.FURNACE.defaultBlockState()
                              .setValue(FurnaceBlock.FACING, facingToward(stationPlacePos, minesis.blockPosition()))
                              .setValue(FurnaceBlock.LIT, true);
                    minesis.level().setBlock(stationPlacePos, placed, 3);
                    minesis.swing(InteractionHand.MAIN_HAND);
                    stationPos = stationPlacePos;
                    minesis.trackStation(stationPos);
                    stationPhase = 2;
                    stationPhaseTimer = 100 + minesis.getRandom().nextInt(120); // 5–11 s
                }
            }

            case 2 -> { // Use the station (navigate + simulate use)
                if (stationPos == null || minesis.level().getBlockState(stationPos).isAir()) {
                    cleanupStation(false); enterState(State.WANDER, 60); return;
                }
                double d = minesis.distanceToSqr(stationPos.getX() + 0.5, stationPos.getY() + 0.5, stationPos.getZ() + 0.5);
                if (d > 6.25D) {
                    if (repathTicks <= 0) {
                        minesis.getNavigation().moveTo(stationPos.getX() + 0.5, stationPos.getY(), stationPos.getZ() + 0.5, SPEED_WALK);
                        repathTicks = 8;
                    }
                } else {
                    minesis.getNavigation().stop();
                    minesis.getLookControl().setLookAt(stationPos.getX() + 0.5, stationPos.getY() + 0.3, stationPos.getZ() + 0.5, YAW_SNAP, PIT_SNAP);
                    if (!stationInteractSwung) {
                        minesis.swing(InteractionHand.MAIN_HAND);
                        stationInteractSwung = true;
                    }
                }
                if (stationPhaseTimer <= 0) {
                    stationPhase = 3;
                    stationPhaseTimer = 0;
                }
            }

            case 3 -> { // Break station progressively (survival-like)
                if (stationPos == null) { cleanupStation(false); enterState(State.WANDER, 60); return; }

                // Initialise breaking on first tick of phase 3
                if (breakingNeededTicks == 0) {
                    BlockState toBreak = minesis.level().getBlockState(stationPos);
                    if (toBreak.is(Blocks.CRAFTING_TABLE)) {
                        ItemStack axe = minesis.findAxeInHotbar();
                        minesis.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                                axe.isEmpty() ? new ItemStack(Items.IRON_AXE) : axe);
                        breakingNeededTicks = 12 + minesis.getRandom().nextInt(8);
                    } else if (toBreak.is(Blocks.FURNACE)) {
                        ItemStack pick = minesis.findPickaxeInHotbar();
                        minesis.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                                pick.isEmpty() ? new ItemStack(Items.IRON_PICKAXE) : pick);
                        breakingNeededTicks = 18 + minesis.getRandom().nextInt(10);
                    } else {
                        // Already broken externally
                        clearBreaking();
                        cleanupStation(false);
                        enterState(State.WANDER, 60 + minesis.getRandom().nextInt(60));
                        return;
                    }
                    breakingPos = stationPos;
                    breakingProgressTicks = 0;
                }

                // Block was broken by someone else mid-animation
                if (minesis.level().getBlockState(stationPos).isAir()) {
                    clearBreaking();
                    cleanupStation(false);
                    enterState(State.WANDER, 60 + minesis.getRandom().nextInt(60));
                    return;
                }

                minesis.getLookControl().setLookAt(stationPos.getX() + 0.5, stationPos.getY() + 0.5, stationPos.getZ() + 0.5, YAW_SNAP, PIT_SNAP);
                minesis.getNavigation().stop();
                breakingProgressTicks++;
                int stage = Math.min(9, (breakingProgressTicks * 10) / Math.max(1, breakingNeededTicks));
                minesis.level().destroyBlockProgress(minesis.getId(), stationPos, stage);
                if (breakingProgressTicks % 4 == 1) minesis.swing(InteractionHand.MAIN_HAND);
                if (breakingProgressTicks >= breakingNeededTicks) {
                    minesis.level().destroyBlock(stationPos, false);
                    clearBreaking();
                    cleanupStation(false);
                    enterState(State.WANDER, 60 + minesis.getRandom().nextInt(60));
                }
            }
        }
    }

    private void cleanupStation(boolean breakIt) {
        if (breakIt && stationPos != null && !minesis.level().getBlockState(stationPos).isAir()) {
            minesis.level().destroyBlock(stationPos, false);
        }
        if (stationPos != null) minesis.untrackStation(stationPos);
        stationPos = null;
        stationPlacePos = null;
        stationPhase = 0;
        stationPhaseTimer = 0;
        stationInteractSwung = false;
    }

    // ── CHOP ─────────────────────────────────────────────────────────────────

    private void tickChop() {
        if (breakingPos == null) {
            BlockPos log = findNearestLog(12);
            if (log == null) { enterState(State.WANDER, 60 + minesis.getRandom().nextInt(80)); return; }
            breakingPos = log;
            breakingNeededTicks = 25 + minesis.getRandom().nextInt(20);
            breakingProgressTicks = 0;
            veinBlocksRemaining = 4 + minesis.getRandom().nextInt(5);
            ItemStack axe = minesis.findAxeInHotbar();
            if (axe.isEmpty()) axe = new ItemStack(Items.IRON_AXE);
            beginToolTransition(axe);
        }
        if (!isLogBlock(minesis.level().getBlockState(breakingPos))) {
            clearBreaking(); minesis.clearHeldItems();
            enterState(State.WANDER, 40 + minesis.getRandom().nextInt(60));
            return;
        }
        double d = minesis.distanceToSqr(breakingPos.getX() + 0.5, breakingPos.getY() + 0.5, breakingPos.getZ() + 0.5);
        if (d > 9.0D) {
            minesis.setShiftKeyDown(false);
            if (repathTicks <= 0 || minesis.getNavigation().isDone()) {
                minesis.getNavigation().moveTo(breakingPos.getX() + 0.5, breakingPos.getY(), breakingPos.getZ() + 0.5, SPEED_TASK);
                repathTicks = 8 + minesis.getRandom().nextInt(8);
            }
        } else {
            minesis.getNavigation().stop();
            minesis.setShiftKeyDown(true);
            minesis.getLookControl().setLookAt(breakingPos.getX() + 0.5, breakingPos.getY() + 0.5, breakingPos.getZ() + 0.5, YAW_SNAP, PIT_SNAP);
            breakingProgressTicks++;
            int stage = Math.min(9, (breakingProgressTicks * 10) / Math.max(1, breakingNeededTicks));
            minesis.level().destroyBlockProgress(minesis.getId(), breakingPos, stage);
            if (breakingProgressTicks % 5 == 0) minesis.swing(InteractionHand.MAIN_HAND);
            if (breakingProgressTicks >= breakingNeededTicks) {
                BlockPos broken = breakingPos;
                minesis.level().destroyBlock(broken, true);
                minesis.collectNearbyItems(broken);
                minesis.level().destroyBlockProgress(minesis.getId(), broken, -1);
                clearBreaking();

                veinBlocksRemaining--;
                BlockPos next = veinBlocksRemaining > 0 ? findAdjacentLog(broken) : null;
                if (next == null) next = veinBlocksRemaining > 0 ? findNearestLog(8) : null;
                if (next != null) {
                    breakingPos = next;
                    breakingNeededTicks = 25 + minesis.getRandom().nextInt(20);
                    breakingProgressTicks = 0;
                } else {
                    minesis.clearHeldItems();
                    enterState(State.WANDER, 40 + minesis.getRandom().nextInt(80));
                }
            }
        }
    }

    // ── MINE ─────────────────────────────────────────────────────────────────

    private void tickMine() {
        if (breakingPos == null) {
            BlockPos ore = findNearestOre(12);
            if (ore == null) { enterState(State.WANDER, 60 + minesis.getRandom().nextInt(80)); return; }
            breakingPos = ore;
            breakingNeededTicks = 40 + minesis.getRandom().nextInt(40);
            breakingProgressTicks = 0;
            veinBlocksRemaining = 8 + minesis.getRandom().nextInt(8);
            ItemStack pick = minesis.findPickaxeInHotbar();
            if (pick.isEmpty()) pick = new ItemStack(Items.IRON_PICKAXE);
            beginToolTransition(pick);
        }
        if (!isOreBlock(minesis.level().getBlockState(breakingPos))) {
            clearBreaking(); minesis.clearHeldItems();
            enterState(State.WANDER, 40 + minesis.getRandom().nextInt(60));
            return;
        }
        double d = minesis.distanceToSqr(breakingPos.getX() + 0.5, breakingPos.getY() + 0.5, breakingPos.getZ() + 0.5);
        if (d > 9.0D) {
            minesis.setShiftKeyDown(false);
            if (scaffoldCooldown > 0) {
                scaffoldCooldown--;
            } else if (repathTicks <= 0 && minesis.getNavigation().isDone() && scaffoldCount < 6) {
                if (tryPlaceCobblestoneToward(breakingPos)) {
                    scaffoldCount++;
                    scaffoldCooldown = 20;
                    stuckTicks = 0;
                }
            }
            if (repathTicks <= 0 || minesis.getNavigation().isDone()) {
                minesis.getNavigation().moveTo(breakingPos.getX() + 0.5, breakingPos.getY(), breakingPos.getZ() + 0.5, SPEED_TASK);
                repathTicks = 8 + minesis.getRandom().nextInt(8);
            }
        } else {
            minesis.getNavigation().stop();
            minesis.setShiftKeyDown(true);
            minesis.getLookControl().setLookAt(breakingPos.getX() + 0.5, breakingPos.getY() + 0.5, breakingPos.getZ() + 0.5, YAW_SNAP, PIT_SNAP);
            breakingProgressTicks++;
            int stage = Math.min(9, (breakingProgressTicks * 10) / Math.max(1, breakingNeededTicks));
            minesis.level().destroyBlockProgress(minesis.getId(), breakingPos, stage);
            if (breakingProgressTicks % 5 == 0) minesis.swing(InteractionHand.MAIN_HAND);
            if (breakingProgressTicks >= breakingNeededTicks) {
                BlockPos broken = breakingPos;
                minesis.level().destroyBlock(broken, true);
                minesis.collectNearbyItems(broken);
                minesis.level().destroyBlockProgress(minesis.getId(), broken, -1);
                clearBreaking();

                veinBlocksRemaining--;
                BlockPos next = veinBlocksRemaining > 0 ? findAdjacentOre(broken) : null;
                if (next == null) next = veinBlocksRemaining > 0 ? findNearestOre(8) : null;
                if (next != null) {
                    breakingPos = next;
                    breakingNeededTicks = 40 + minesis.getRandom().nextInt(40);
                    breakingProgressTicks = 0;
                } else {
                    minesis.clearHeldItems();
                    enterState(State.WANDER, 40 + minesis.getRandom().nextInt(80));
                }
            }
        }
        if (minesis.level().getBrightness(LightLayer.BLOCK, minesis.blockPosition()) <= 2)
            placeTorchOccasionally();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATE TRANSITIONS
    // ══════════════════════════════════════════════════════════════════════════

    private void chooseNextState() {
        BlockPos ore   = findNearestOre(12);
        BlockPos log   = findNearestLog(12);
        BlockPos chest = chestCooldown <= 0 ? findNearestContainer(20) : null;
        BlockPos table = findNearestCraftingTable(16);
        int roll = minesis.getRandom().nextInt(100);

        // Tâches en priorité si un bloc est visible
        if      (roll < 40 && ore != null)   { enterState(State.MINE,  300 + minesis.getRandom().nextInt(200)); }
        else if (roll < 65 && log != null)   { enterState(State.CHOP,  250 + minesis.getRandom().nextInt(150)); }
        else if (roll < 67 && chest != null) { chestTarget = chest; enterState(State.CHEST_INTERACT, 300); }
        else if (roll < 69 && table != null) { craftTarget = table; craftUseTimer = 100 + minesis.getRandom().nextInt(120); enterState(State.USE_CRAFTING, 300); }
        else if (roll < 71)                  { enterState(State.PLACE_STATION, 600); }
        else if (roll < 85)                  { enterState(State.WANDER,  60 + minesis.getRandom().nextInt(100)); }
        else if (roll < 91) { destination = findWanderDestination(20, 40); enterState(State.EXPLORE, 150 + minesis.getRandom().nextInt(100)); }
        else if (roll < 96)                  { enterState(State.IDLE,   25 + minesis.getRandom().nextInt(50)); }
        else                                 { enterState(State.PAUSE,  40 + minesis.getRandom().nextInt(80)); }
    }

    private void enterState(State next, int duration) {
        state       = next;
        stateTicks  = duration;
        destination = null;
        repathTicks = 0;
        lookTimer   = 0;
        idleLookTarget = null;
        if (next != State.CHOP && next != State.MINE) {
            clearBreaking();
            minesis.clearHeldItems();
            clearItemTransition();
            minesis.setShiftKeyDown(false);
            veinBlocksRemaining = 0;
        }
        if (next == State.IDLE || next == State.PAUSE) {
            sprintTicks = 0;
            minesis.setSprinting(false);
        }
        if (next != State.CHEST_INTERACT) { chestOpened = false; chestTarget = null; }
        if (next != State.USE_CRAFTING)   { craftTarget = null; craftUseTimer = 0; craftInteractSwung = false; }
        if (next == State.PLACE_STATION)  { stationPhase = 0; stationPlacePos = null; }
        minesis.setAtCraftingStation(next == State.USE_CRAFTING || next == State.PLACE_STATION);

        minesis.setVoiceContext(switch (next) {
            case MINE, CHOP                        -> com.minesis.voice.VoiceContext.MINING;
            case USE_CRAFTING, PLACE_STATION,
                 CHEST_INTERACT                    -> com.minesis.voice.VoiceContext.CRAFTING;
            case WANDER, EXPLORE                   -> com.minesis.voice.VoiceContext.WALKING;
            default                                -> com.minesis.voice.VoiceContext.IDLE;
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PLAYER MIRRORING
    // ══════════════════════════════════════════════════════════════════════════

    private void mirrorTargetPlayerBehavior() {
        UUID targetUUID = minesis.getTargetPlayerUUID();
        if (targetUUID == null) return;
        Player target = minesis.level().getPlayerByUUID(targetUUID);
        if (target == null || !target.isAlive() || minesis.distanceTo(target) > 40.0D) return;

        if (target.isSprinting() && sprintTicks <= 0 && sprintCooldown <= 0
                && minesis.getRandom().nextInt(3) != 0) {
            sprintTicks = 15 + minesis.getRandom().nextInt(25);
            sprintCooldown = 50;
        }
        if (target.isShiftKeyDown() && !minesis.isCrouching()
                && minesis.getRandom().nextInt(3) == 0) {
            minesis.startCrouch(15 + minesis.getRandom().nextInt(20));
        }
        if (!target.onGround() && minesis.onGround()
                && minesis.getRandom().nextInt(5) == 0) {
            minesis.getJumpControl().jump();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  NAVIGATION HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private Vec3 findWanderDestination(double minR, double maxR) {
        for (int i = 0; i < 12; i++) {
            double a = minesis.getRandom().nextDouble() * Math.PI * 2;
            double r = minR + minesis.getRandom().nextDouble() * (maxR - minR);
            Vec3 p = snapToGround(minesis.getX() + Math.cos(a) * r, minesis.getZ() + Math.sin(a) * r, minesis.getY());
            if (!isRecentlyVisited(p)) return p;
        }
        double a = minesis.getRandom().nextDouble() * Math.PI * 2;
        double r = minR + minesis.getRandom().nextDouble() * (maxR - minR);
        return snapToGround(minesis.getX() + Math.cos(a) * r, minesis.getZ() + Math.sin(a) * r, minesis.getY());
    }

    private Vec3 snapToGround(double x, double z, double refY) {
        int y = minesis.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            BlockPos.containing(x, refY, z).getX(), BlockPos.containing(x, refY, z).getZ());
        return new Vec3(x, y, z);
    }

    private boolean isRecentlyVisited(Vec3 p) {
        for (Vec3 old : recentDests) if (old.distanceToSqr(p) < 16) return true;
        recentDests.addLast(p);
        if (recentDests.size() > 12) recentDests.removeFirst();
        return false;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOOK HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private Vec3 findInterestingLookPoint() {
        BlockPos center = minesis.blockPosition();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    if (minesis.getRandom().nextInt(30) != 0) continue;
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (isInterestingBlock(minesis.level().getBlockState(pos)))
                        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                }
            }
        }
        double a = minesis.getRandom().nextDouble() * Math.PI * 2;
        return new Vec3(minesis.getX() + Math.cos(a) * 8, minesis.getEyeY(), minesis.getZ() + Math.sin(a) * 8);
    }

    private boolean isInterestingBlock(BlockState s) {
        Block b = s.getBlock();
        return isLogBlock(s) || isOreBlock(s)
            || b == Blocks.WATER || b == Blocks.LAVA
            || b == Blocks.CHEST || b == Blocks.BARREL || b == Blocks.TRAPPED_CHEST
            || b == Blocks.FURNACE || b == Blocks.CRAFTING_TABLE || b == Blocks.BOOKSHELF
            || b == Blocks.DIAMOND_BLOCK || b == Blocks.GOLD_BLOCK || b == Blocks.IRON_BLOCK;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STUCK RECOVERY
    // ══════════════════════════════════════════════════════════════════════════

    private void updateStuck() {
        Vec3 now = minesis.position();
        if (state == State.IDLE || state == State.PAUSE) { stuckTicks = 0; lastPos = now; return; }
        if (lastPos != null && now.distanceTo(lastPos) < 0.07D) stuckTicks++;
        else stuckTicks = 0;
        lastPos = now;
        if (stuckTicks < 8) return;
        if (!tryOpenDoorNearSelf() && !breakSoftObstacleInFront()) {
            if (stuckTicks >= 16) {
                destination = findWanderDestination(4, 12);
                repathTicks = 0; stuckTicks = 0;
                if (state == State.CHOP || state == State.MINE)
                    enterState(State.WANDER, 60 + minesis.getRandom().nextInt(60));
            }
        } else stuckTicks = 0;
    }

    private boolean breakSoftObstacleInFront() {
        Vec3 look = minesis.getLookAngle();
        int sx = (int) Math.signum(look.x), sz = (int) Math.signum(look.z);
        if (sx == 0 && sz == 0) sx = 1;
        if (sx != 0 && sz != 0) { if (minesis.getRandom().nextBoolean()) sz = 0; else sx = 0; }
        BlockPos base = minesis.blockPosition();
        for (int i = 1; i <= 2; i++) {
            BlockPos p = base.offset(sx * i, 0, sz * i);
            BlockState s = minesis.level().getBlockState(p);
            if (!s.isAir() && isSoftBreakable(s)) { minesis.level().destroyBlock(p, true); return true; }
        }
        return false;
    }

    private boolean isSoftBreakable(BlockState s) {
        if (s.getDestroySpeed(minesis.level(), minesis.blockPosition()) < 0) return false;
        Block b = s.getBlock();
        return b == Blocks.DIRT || b == Blocks.GRASS_BLOCK || b == Blocks.SAND || b == Blocks.GRAVEL
            || b == Blocks.COBBLESTONE || b == Blocks.OAK_PLANKS || b == Blocks.SPRUCE_PLANKS
            || b == Blocks.BIRCH_PLANKS || b == Blocks.OAK_LOG || b == Blocks.SPRUCE_LOG
            || b == Blocks.BIRCH_LOG || b == Blocks.OAK_LEAVES || b == Blocks.SPRUCE_LEAVES
            || b == Blocks.BIRCH_LEAVES || b == Blocks.COBWEB || b == Blocks.VINE
            || b == Blocks.TALL_GRASS || b == Blocks.SNOW;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DOOR INTERACTION
    // ══════════════════════════════════════════════════════════════════════════

    private boolean tryOpenDoorNearSelf() {
        BlockPos base = minesis.blockPosition();
        return tryOpenDoor(base) || tryOpenDoor(base.north()) || tryOpenDoor(base.south())
            || tryOpenDoor(base.east()) || tryOpenDoor(base.west());
    }

    private boolean tryOpenDoor(BlockPos pos) {
        BlockState s = minesis.level().getBlockState(pos);
        if (!(s.getBlock() instanceof DoorBlock) || !s.hasProperty(DoorBlock.OPEN)) return false;
        BlockPos lower = pos;
        if (s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            lower = pos.below(); s = minesis.level().getBlockState(lower);
        }
        if (!(s.getBlock() instanceof DoorBlock) || !s.hasProperty(DoorBlock.OPEN)) return false;
        boolean open = s.getValue(DoorBlock.OPEN);
        boolean next = !open || minesis.getRandom().nextInt(100) >= 28;
        if (next == open) return false;
        minesis.level().setBlock(lower, s.setValue(DoorBlock.OPEN, next), 10);
        BlockPos upper = lower.above();
        BlockState us = minesis.level().getBlockState(upper);
        if (us.getBlock() instanceof DoorBlock && us.hasProperty(DoorBlock.OPEN))
            minesis.level().setBlock(upper, us.setValue(DoorBlock.OPEN, next), 10);
        minesis.level().playSound(null, lower,
            next ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE,
            SoundSource.BLOCKS, 0.65F, 1.0F);
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BLOCK SCANNING
    // ══════════════════════════════════════════════════════════════════════════

    private BlockPos findNearestContainer(int radius) {
        BlockPos center = minesis.blockPosition();
        BlockPos best = null; double best2 = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = center.offset(x, y, z);
                    Block b = minesis.level().getBlockState(p).getBlock();
                    if (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.BARREL) {
                        double d = minesis.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
                        if (d < best2) { best2 = d; best = p; }
                    }
                }
            }
        }
        return best;
    }

    private BlockPos findNearestCraftingTable(int radius) {
        BlockPos center = minesis.blockPosition();
        BlockPos best = null; double best2 = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = center.offset(x, y, z);
                    if (minesis.level().getBlockState(p).getBlock() == Blocks.CRAFTING_TABLE) {
                        double d = minesis.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
                        if (d < best2) { best2 = d; best = p; }
                    }
                }
            }
        }
        return best;
    }

    private BlockPos findNearestLog(int radius) {
        BlockPos center = minesis.blockPosition();
        BlockPos best = null; double best2 = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 4; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = center.offset(x, y, z);
                    if (isLogBlock(minesis.level().getBlockState(p)) && hasLos(p)) {
                        double d = minesis.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
                        if (d < best2) { best2 = d; best = p; }
                    }
                }
            }
        }
        return best;
    }

    private BlockPos findNearestOre(int radius) {
        BlockPos center = minesis.blockPosition();
        BlockPos best = null; double best2 = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = center.offset(x, y, z);
                    if (isOreBlock(minesis.level().getBlockState(p)) && hasLos(p)) {
                        double d = minesis.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
                        if (d < best2) { best2 = d; best = p; }
                    }
                }
            }
        }
        return best;
    }

    private BlockPos findAdjacentOre(BlockPos center) {
        for (Direction dir : Direction.values()) {
            BlockPos adj = center.relative(dir);
            if (isOreBlock(minesis.level().getBlockState(adj))) return adj;
        }
        return null;
    }

    private BlockPos findAdjacentLog(BlockPos center) {
        // Prioritize upward (trunk grows up)
        BlockPos above = center.above();
        if (isLogBlock(minesis.level().getBlockState(above))) return above;
        for (Direction dir : Direction.values()) {
            BlockPos adj = center.relative(dir);
            if (isLogBlock(minesis.level().getBlockState(adj))) return adj;
        }
        return null;
    }

    /** Find an adjacent air position with a solid floor — valid station placement spot. */
    private BlockPos findPlacementPos() {
        BlockPos base = minesis.blockPosition();
        BlockPos[] candidates = {
            base.north(), base.south(), base.east(), base.west(),
            base.north().east(), base.north().west(), base.south().east(), base.south().west()
        };
        for (BlockPos pos : candidates) {
            if (minesis.level().getBlockState(pos).isAir()
                    && !minesis.level().getBlockState(pos.below()).isAir()
                    && minesis.level().getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }
        return null;
    }

    private boolean isLogBlock(BlockState s) {
        Block b = s.getBlock();
        return b == Blocks.OAK_LOG || b == Blocks.SPRUCE_LOG || b == Blocks.BIRCH_LOG
            || b == Blocks.JUNGLE_LOG || b == Blocks.ACACIA_LOG || b == Blocks.DARK_OAK_LOG
            || b == Blocks.MANGROVE_LOG;
    }

    private boolean isOreBlock(BlockState s) {
        Block b = s.getBlock();
        return b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE
            || b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE
            || b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE
            || b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE
            || b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE
            || b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE
            || b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE
            || b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE;
    }

    private boolean hasLos(BlockPos pos) {
        BlockHitResult hit = minesis.level().clip(new ClipContext(
            minesis.getEyePosition(), Vec3.atCenterOf(pos),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minesis));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    /** Cardinal direction from a source pos toward a target pos. */
    private Direction facingToward(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        if (Math.abs(dx) >= Math.abs(dz)) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BREAKING / TORCH
    // ══════════════════════════════════════════════════════════════════════════

    private void clearBreaking() {
        if (breakingPos != null)
            minesis.level().destroyBlockProgress(minesis.getId(), breakingPos, -1);
        breakingPos = null; breakingProgressTicks = 0; breakingNeededTicks = 0;
        scaffoldCooldown = 0; scaffoldCount = 0;
    }

    private boolean tryPlaceCobblestoneToward(BlockPos target) {
        BlockPos mob = minesis.blockPosition();
        int dx = target.getX() - mob.getX();
        int dy = target.getY() - mob.getY();
        int dz = target.getZ() - mob.getZ();
        int sx = Integer.signum(dx);
        int sz = Integer.signum(dz);

        BlockPos[] candidates;
        if (sx != 0 && sz != 0) {
            if (dy > 0)
                candidates = new BlockPos[]{ mob.offset(sx, 1, sz), mob.offset(sx, 0, sz),
                                             mob.offset(sx, 1, 0), mob.offset(0, 1, sz) };
            else
                candidates = new BlockPos[]{ mob.offset(sx, 0, sz), mob.offset(sx, 1, sz),
                                             mob.offset(sx, 0, 0), mob.offset(0, 0, sz) };
        } else if (sx != 0) {
            candidates = dy > 0
                ? new BlockPos[]{ mob.offset(sx, 1, 0), mob.offset(sx, 0, 0) }
                : new BlockPos[]{ mob.offset(sx, 0, 0), mob.offset(sx, 1, 0) };
        } else if (sz != 0) {
            candidates = dy > 0
                ? new BlockPos[]{ mob.offset(0, 1, sz), mob.offset(0, 0, sz) }
                : new BlockPos[]{ mob.offset(0, 0, sz), mob.offset(0, 1, sz) };
        } else if (dy > 0) {
            candidates = new BlockPos[]{ mob.north().above(), mob.south().above(),
                                         mob.east().above(), mob.west().above() };
        } else {
            return false;
        }

        for (BlockPos candidate : candidates) {
            if (candidate.equals(target)) continue;
            if (!minesis.level().getBlockState(candidate).isAir()) continue;
            if (!hasAdjacentSolid(candidate)) continue;
            minesis.level().setBlock(candidate, Blocks.COBBLESTONE.defaultBlockState(), 3);
            minesis.getLookControl().setLookAt(candidate.getX() + 0.5, candidate.getY() + 0.5,
                    candidate.getZ() + 0.5, YAW_SNAP, PIT_SNAP);
            minesis.swing(InteractionHand.MAIN_HAND);
            return true;
        }
        return false;
    }

    private boolean hasAdjacentSolid(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.relative(dir);
            BlockState adjState = minesis.level().getBlockState(adj);
            if (!adjState.isAir() && adjState.isFaceSturdy(minesis.level(), adj, dir.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    private void placeTorchOccasionally() {
        if (minesis.getRandom().nextInt(100) >= 15) return;
        BlockPos base = minesis.blockPosition();
        for (BlockPos pos : new BlockPos[]{ base.north(), base.south(), base.east(), base.west(), base.above() }) {
            if (!minesis.level().getBlockState(pos).isAir()) continue;
            BlockState below = minesis.level().getBlockState(pos.below());
            if (below.isAir() || !below.getFluidState().isEmpty()) continue;
            if (!below.isFaceSturdy(minesis.level(), pos.below(), Direction.UP)) continue;
            // Look at the torch position before placing
            minesis.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, YAW_SNAP, PIT_SNAP);
            minesis.level().setBlock(pos, Blocks.TORCH.defaultBlockState(), 3);
            minesis.swing(InteractionHand.MAIN_HAND);
            return;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private void resetAllTimers() {
        state = State.IDLE; stateTicks = 0; repathTicks = 0; lookTimer = 0;
        stuckTicks = 0; sprintTicks = 0; sprintCooldown = 0;
        mirrorTimer = 0; chestCooldown = 0; chestUseTimer = 0;
        craftUseTimer = 0; stationPhase = 0; stationPhaseTimer = 0;
        itemScrollTimer = 0;
        clearItemTransition();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ITEM CYCLING
    // ══════════════════════════════════════════════════════════════════════════

    private void tickItemCycling() {
        if (itemTransitionDelay > 0) { itemTransitionDelay--; return; }

        if (itemTransitionSeq != null) {
            if (itemTransitionIdx < itemTransitionSeq.length) {
                minesis.setItemInHand(InteractionHand.MAIN_HAND,
                        itemTransitionSeq[itemTransitionIdx].copy());
                itemTransitionIdx++;
                if (itemTransitionIdx < itemTransitionSeq.length)
                    itemTransitionDelay = 5 + minesis.getRandom().nextInt(8);
                else
                    itemTransitionSeq = null;
            } else {
                itemTransitionSeq = null;
            }
            return;
        }

        if (itemScrollTimer > 0) { itemScrollTimer--; return; }

        if (minesis.hasHotbarItems()) {
            switch (state) {
                case IDLE: case WANDER: case PAUSE: case EXPLORE:
                    beginRandomScroll(1 + minesis.getRandom().nextInt(3));
                    itemScrollTimer = 60 + minesis.getRandom().nextInt(140);
                    break;
                default: break;
            }
        }
    }

    private void beginRandomScroll(int steps) {
        ItemStack[] seq = new ItemStack[steps];
        for (int i = 0; i < steps; i++) seq[i] = minesis.pickRandomHotbarItem();
        beginItemTransition(seq);
    }

    private void beginToolTransition(ItemStack target) {
        int randSteps = 1 + minesis.getRandom().nextInt(3);
        ItemStack[] seq = new ItemStack[randSteps + 1];
        for (int i = 0; i < randSteps; i++) {
            ItemStack pick = minesis.pickRandomHotbarItem();
            seq[i] = pick.isEmpty() ? target : pick;
        }
        seq[randSteps] = target;
        beginItemTransition(seq);
    }

    private void beginItemTransition(ItemStack[] seq) {
        itemTransitionSeq   = seq;
        itemTransitionIdx   = 0;
        itemTransitionDelay = 2 + minesis.getRandom().nextInt(4);
    }

    private void clearItemTransition() {
        itemTransitionSeq   = null;
        itemTransitionIdx   = 0;
        itemTransitionDelay = 0;
    }
}
