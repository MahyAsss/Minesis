package com.minesis.entity;
 
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import org.joml.Vector3f;
 
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import com.minesis.voice.VoiceContext;
import com.minesis.voice.VoiceStorage;
import com.minesis.voice.VoicePlaybackService;
import com.minesis.ai.MinesisAttackGoal;
import com.minesis.ai.MinesisMobCombatGoal;
import com.minesis.ai.MinesisPlayerLikeMovementGoal;
import com.minesis.ai.MinesisBlockBreakGoal;
import com.minesis.ai.MinesisDanceGoal;
import com.minesis.ai.HumanPlayerLookGoal;
import com.minesis.ai.RealisticHeadLookGoal;
import com.minesis.MinesisSounds;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.damagesource.DamageSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nullable;
 
public class MinesisEntity extends Monster {
 
    private static final int MIN_HOSTILE_DELAY_TICKS = 20 * 60;  // 1 minute
    private static final int MAX_HOSTILE_DELAY_TICKS = 20 * 180; // 3 minutes
    private static final Logger LOGGER = LogManager.getLogger();
 
    // ── Données synchronisées ─────────────────────────────────────────────
    private static final EntityDataAccessor<String>  TARGET_PLAYER_UUID =
            SynchedEntityData.defineId(MinesisEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String>  TARGET_PLAYER_NAME =
            SynchedEntityData.defineId(MinesisEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String>  APPEARANCE_PLAYER_UUID =
            SynchedEntityData.defineId(MinesisEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> BEHAVIOR_TIMER =
            SynchedEntityData.defineId(MinesisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HOSTILE_MODE =
            SynchedEntityData.defineId(MinesisEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String>  SKIN_TEXTURE_PROPERTIES =
            SynchedEntityData.defineId(MinesisEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String>  SKIN_MODEL =
            SynchedEntityData.defineId(MinesisEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SPEAKING =
            SynchedEntityData.defineId(MinesisEntity.class, EntityDataSerializers.BOOLEAN);
 
    // ── Champs internes ───────────────────────────────────────────────────
    private UUID   targetPlayerUUID;
    private UUID   appearancePlayerUUID;
    private String targetPlayerName;
    private boolean allowVoicePlayback   = true;
    private boolean allowInventoryCopy   = true;
    private int  timeSinceSpawn          = 0;
    private int  hostileActivationTime   = 300;
    private boolean isHostile            = false;
    private int  voicePlaybackCooldown   = 0;
 
    private static final int VOICE_REPLAY_INTERVAL_MIN = 400;
    private static final int VOICE_REPLAY_INTERVAL_MAX = 900;
 
    private int  speakingIconTimer           = 0;
    private int  armorSyncTimer             = 0;
    private int  hostileMobAggroTimer       = 0;
    private int  passiveItemCollectionTimer = 0;
    private NonNullList<ItemStack> carriedItems = NonNullList.withSize(9, ItemStack.EMPTY);
    // Copie du hotbar (slots 0-8) du joueur copié — mise à jour toutes les 20 ticks
    private final NonNullList<ItemStack> hotbarItems = NonNullList.withSize(9, ItemStack.EMPTY);
    private BlockPos cookingStationPos;
 
    private boolean provokedByPlayer      = false;
    private int     provokedByPlayerTimer = 0;
    private boolean provokedByMob         = false;
    private int     provokedByMobTimer    = 0;
    private boolean hostilityTransformPlayed = false;
    private boolean playCustomHurtSound   = false;
    private boolean singingLoopActive     = false;
    private int     singingLoopTimer      = 0;
    private static final int SINGING_LOOP_INTERVAL = 20 * 6;

    private int          transformationFreezeTimer = 0;
    private int          leashCheckTimer          = 0;
    private VoiceContext voiceContext              = VoiceContext.IDLE;

    private boolean atCraftingStation     = false;
    private boolean fakingHurt            = false;

    // Replaces the iron-sword weapon bonus (+5 = Tiers.IRON bonus 2 + SwordItem base 3)
    private static final java.util.UUID HOSTILE_DAMAGE_UUID =
            java.util.UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final AttributeModifier HOSTILE_DAMAGE_BONUS = new AttributeModifier(
            HOSTILE_DAMAGE_UUID, "minesis.hostile_damage", 5.0D, AttributeModifier.Operation.ADDITION);
 
    // ── Comportements humains ─────────────────────────────────────────────
    private int humanBehaviorTimer = 0;
    private int crouchTimer = 0;

    // ── Blocs temporaires posés par le mob (table de craft, four) ─────────
    // Nettoyés à la mort, au despawn, et au passage en mode hostile.
    private final Set<BlockPos> temporaryStations = new HashSet<>();

    // ─────────────────────────────────────────────────────────────────────
 
    public MinesisEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 50;
        this.setHealth(20.0F);
    }
 
    public boolean isProvokedByPlayer()      { return this.provokedByPlayer; }
    public boolean isProvokedByMob()         { return this.provokedByMob;    }
    public boolean isTransformationFreezing(){ return this.transformationFreezeTimer > 0; }

    public void setVoiceContext(VoiceContext ctx) { this.voiceContext = ctx; }

    private VoiceContext computePlaybackContext() {
        if (this.isHostileModeActive())  return VoiceContext.COMBAT;
        if (this.isSwimming() || this.isUnderWater()) return VoiceContext.SWIMMING;
        return this.voiceContext; // set by the movement goal at each state transition
    }

    public void startCrouch(int ticks) {
        if (this.crouchTimer > 0) return;
        this.setPose(Pose.CROUCHING);
        this.setShiftKeyDown(true);
        this.crouchTimer = ticks;
    }

    public void setAtCraftingStation(boolean at) { this.atCraftingStation = at; }

    // ── Blocs temporaires ────────────────────────────────────────────────

    public void trackStation(BlockPos pos)   { this.temporaryStations.add(pos.immutable()); }
    public void untrackStation(BlockPos pos) { this.temporaryStations.remove(pos); }

    public void removeAllStations() {
        if (this.level().isClientSide) return;
        for (BlockPos pos : this.temporaryStations) {
            net.minecraft.world.level.block.Block b = this.level().getBlockState(pos).getBlock();
            if (b == Blocks.CRAFTING_TABLE || b == Blocks.FURNACE) {
                this.level().removeBlock(pos, false);
            }
        }
        this.temporaryStations.clear();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) removeAllStations();
        super.remove(reason);
    }
 
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,     20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE,  4.0D)
                .add(Attributes.FOLLOW_RANGE,   64.0D);
    }
 
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TARGET_PLAYER_UUID,      "");
        this.entityData.define(TARGET_PLAYER_NAME,      "");
        this.entityData.define(APPEARANCE_PLAYER_UUID,  "");
        this.entityData.define(BEHAVIOR_TIMER,          0);
        this.entityData.define(HOSTILE_MODE,            false);
        this.entityData.define(SKIN_TEXTURE_PROPERTIES, "");
        this.entityData.define(SKIN_MODEL,              "");
        this.entityData.define(SPEAKING,                false);
    }
 
    // ── Enregistrement des Goals ──────────────────────────────────────────
 
    @Override
    protected void registerGoals() {
        // 0 : flotter dans l'eau (toujours en premier)
        this.goalSelector.addGoal(0, new FloatGoal(this));
 
        // 1 : combat contre mobs hostiles proches
        this.goalSelector.addGoal(1, new MinesisMobCombatGoal(this, 1.15D));
 
        // 2 : attaque du joueur (mode hostile)
        this.goalSelector.addGoal(2, new MinesisAttackGoal(this));
 
        // 3 : machine à états principale (comportement joueur)
        this.goalSelector.addGoal(3, new MinesisPlayerLikeMovementGoal(this));
 
        // 4 : minage de minerais visibles (complément de la machine à états)
        this.goalSelector.addGoal(4, new MinesisBlockBreakGoal(this));
 
        // 5 : accroupissement / danse aléatoire
        this.goalSelector.addGoal(5, new MinesisDanceGoal(this));
 
        // 6 : regard réaliste pendant les pauses
        this.goalSelector.addGoal(6, new RealisticHeadLookGoal(this));
 
        // 7 : regarder le joueur proche (regard humain avec dérive)
        this.goalSelector.addGoal(7, new HumanPlayerLookGoal(this, 8.0D));
 
        // 8 : rotation aléatoire de tête (fallback)
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
 
        // Navigation : ouvre les portes en mode passif (hostile = brise)
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanPassDoors(true);
        }

        // ── Cibles ──
        // Réagit si attaqué
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Attaque les mobs hostiles proches (rayon max 10 blocs)
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Monster.class, 10, true, false,
                (living) -> this.distanceTo(living) <= 10.0D));
    }
 
    // ── Tick principal ────────────────────────────────────────────────────
 
    @Override
    public void tick() {
        super.tick();
 
        if (!this.level().isClientSide) {
            this.timeSinceSpawn++;

            // Maintenir le nametag visible — SynchedEntityData.set() n'envoie un
            // paquet que si la valeur change, donc ceci ne génère aucun trafic réseau
            // inutile une fois les données stables.
            if (this.targetPlayerName != null && !this.targetPlayerName.isEmpty()) {
                if (!this.isCustomNameVisible()) this.setCustomNameVisible(true);
                if (!this.hasCustomName()) this.setCustomName(
                    net.minecraft.network.chat.Component.literal(this.targetPlayerName));
            }

            // Pause dramatique lors de la transformation
            if (this.transformationFreezeTimer > 0) {
                this.transformationFreezeTimer--;
                if (this.transformationFreezeTimer == 0) {
                    // Fin du freeze : jumpscare physique
                    net.minecraft.world.entity.LivingEntity jumpTarget = this.getTarget();
                    if (jumpTarget == null) {
                        jumpTarget = this.level().getNearestPlayer(
                                this.getX(), this.getY(), this.getZ(), 64.0, false);
                    }
                    if (jumpTarget != null) {
                        Vec3 dir = jumpTarget.position().subtract(this.position()).normalize();
                        this.setDeltaMovement(dir.x * 1.5, 0.5, dir.z * 1.5);
                        this.hasImpulse = true;
                    }
                } else {
                    this.getNavigation().stop();
                    this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
                }
            }

            // Passage en mode hostile (corrigé : vérifie le timer avant tout)
            if (!this.isHostile && this.shouldEnterHostileMode()) {
                this.setHostileMode(true);
            }
 
            // Timers de provocation
            if (this.provokedByPlayerTimer > 0) {
                this.provokedByPlayerTimer--;
                if (this.provokedByPlayerTimer <= 0) this.provokedByPlayer = false;
            }
            if (this.provokedByMobTimer > 0) {
                this.provokedByMobTimer--;
                if (this.provokedByMobTimer <= 0) this.provokedByMob = false;
            }
 
            // Répondre si la proie vient de parler (fenêtre 2 s → réponse en 1-2 s)
            if (!this.isHostileModeActive() && this.voicePlaybackCooldown > 40) {
                UUID tpUUID2 = this.getTargetPlayerUUID();
                if (tpUUID2 != null && com.minesis.voice.PlayerActivityTracker.hasRecentlySpoken(tpUUID2, 2000L)) {
                    this.voicePlaybackCooldown = 20 + this.random.nextInt(20);
                }
            }

            // Icône SVC : décrémenter le timer, éteindre quand expiré
            if (this.speakingIconTimer > 0) {
                this.speakingIconTimer--;
                if (this.speakingIconTimer == 0) this.setSpeaking(false);
            }

            // Playback voix + orientation du regard
            if (this.voicePlaybackCooldown <= 0 && this.isAlive()) {
                this.setSpeaking(true);
                this.speakingIconTimer = 60; // icône visible 3 s
                this.attemptVoicePlayback();
                // Orienter la tête vers le joueur cible au moment de la lecture
                UUID tpUUID = this.getTargetPlayerUUID();
                if (tpUUID != null) {
                    Player tp = this.level().getPlayerByUUID(tpUUID);
                    if (tp != null) {
                        double faceY = tp.getY() + tp.getEyeHeight() * 0.82
                                + (this.random.nextDouble() - 0.5) * 0.4;
                        this.getLookControl().setLookAt(
                                tp.getX() + (this.random.nextDouble() - 0.5) * 0.3,
                                faceY,
                                tp.getZ() + (this.random.nextDouble() - 0.5) * 0.3,
                                14.0F, 14.0F);
                    }
                }
                this.voicePlaybackCooldown = VOICE_REPLAY_INTERVAL_MIN
                        + this.random.nextInt(VOICE_REPLAY_INTERVAL_MAX - VOICE_REPLAY_INTERVAL_MIN + 1);
            } else {
                this.voicePlaybackCooldown--;
            }
 
            // Boucle de chant en mode hostile
            if (this.singingLoopActive) {
                this.singingLoopTimer++;
                if (this.singingLoopTimer >= SINGING_LOOP_INTERVAL) {
                    this.playHostileSinging();
                    this.singingLoopTimer = 0;
                }
            }
 
            // Sync armure toutes les 20 ticks
            this.armorSyncTimer++;
            if (this.armorSyncTimer >= 20) {
                this.syncArmor();
                this.armorSyncTimer = 0;
            }
 
            // Attirer l'agro des mobs hostiles proches (hors mode hostile)
            if (!this.isHostileModeActive()) {
                this.hostileMobAggroTimer++;
                if (this.hostileMobAggroTimer >= 40) {
                    this.pullHostileMobsAggro();
                    this.hostileMobAggroTimer = 0;
                }
            } else {
                this.hostileMobAggroTimer = 0;
            }
 
            // Ramassage passif d'items
            this.passiveItemCollectionTimer++;
            if (this.passiveItemCollectionTimer >= 5) {
                this.collectItemsPassively();
                this.passiveItemCollectionTimer = 0;
            }
 
            // ── Comportements humains aléatoires ─────────────────────────
            // Remplace l'ancien TickTask : le crouchTimer gère l'accroupissement
            if (this.crouchTimer > 0) {
                this.crouchTimer--;
                if (this.crouchTimer == 0) {
                    // Fin de l'accroupissement → se relever
                    if (this.getPose() == Pose.CROUCHING) {
                        this.setPose(Pose.STANDING);
                    }
                }
            }
 
            // Déclenche une action humaine aléatoire toutes les ~2 secondes
            if (!this.isHostileModeActive()) {
                this.humanBehaviorTimer++;
                if (this.humanBehaviorTimer >= 40) {
                    this.humanBehaviorTimer = 0;
                    this.doRandomHumanBehavior();
                }
            }
 
            // Vérification de laisse : retour au joueur si trop loin
            this.leashCheckTimer++;
            if (this.leashCheckTimer >= 40) {
                this.leashCheckTimer = 0;
                this.checkAndLeashToPlayer();
            }

            this.entityData.set(BEHAVIOR_TIMER, this.timeSinceSpawn);
        }
    }
 
    // ── Comportements humains aléatoires ──────────────────────────────────
 
    /**
     * Actions aléatoires « humaines » pendant les phases non-hostiles.
     * N'utilise PAS TickTask (inaccessible depuis les mods Forge) — utilise crouchTimer.
     */
    private void doRandomHumanBehavior() {
        // Ne rien faire si déjà accroupi
        if (this.crouchTimer > 0) return;
 
        int roll = this.random.nextInt(100);

        if (roll < 12 && !this.atCraftingStation) {
            // Saut d'impatience
            if (this.onGround()) {
                this.setDeltaMovement(getDeltaMovement().add(0, 0.35, 0));
            }

        } else if (roll < 25) {
            // Accroupissement bref (~1 seconde) — géré par crouchTimer dans tick()
            this.setPose(Pose.CROUCHING);
            this.crouchTimer = 15 + this.random.nextInt(15);

        } else if (roll < 38) {
            // Poser une torche si zone sombre
            if (this.level().getBrightness(LightLayer.BLOCK, this.blockPosition()) <= 3) {
                this.tryPlaceTorch();
            }
        }
        // ~62 % restants → pause naturelle (navigation gérée par le goal)
    }
 
    /**
     * Pose une torche sur un bloc adjacent libre si le sol est solide.
     */
    private void tryPlaceTorch() {
        BlockPos base = this.blockPosition();
        BlockPos[] candidates = {
            base.north(), base.south(), base.east(), base.west(), base.above()
        };
        for (BlockPos pos : candidates) {
            BlockState state = this.level().getBlockState(pos);
            if (!state.isAir()) continue;
            BlockState below = this.level().getBlockState(pos.below());
            if (below.isAir() || !below.getFluidState().isEmpty()) continue;
            if (!below.isFaceSturdy(this.level(), pos.below(), Direction.UP)) continue;
            // Regarder l'emplacement avant de poser
            this.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 40.0F, 40.0F);
            this.level().setBlock(pos, Blocks.TORCH.defaultBlockState(), 3);
            this.swing(InteractionHand.MAIN_HAND);
            return;
        }
    }
 
    // ── Mode hostile ──────────────────────────────────────────────────────
 
    private boolean shouldEnterHostileMode() {
        if (this.getTargetPlayerName().isEmpty()) return false;
        if (this.timeSinceSpawn < this.hostileActivationTime) return false;
        Player nearestPlayer = this.level().getNearestPlayer(
                this.getX(), this.getY(), this.getZ(), 4.0D, false);
        return nearestPlayer != null;
    }
 
    public void setHostileMode(boolean hostile) {
        this.isHostile = hostile;
        this.entityData.set(HOSTILE_MODE, hostile);
        if (hostile) {
            if (!this.level().isClientSide) this.removeAllStations();
            this.clearNearbyAggro();
            if (!this.singingLoopActive && !this.level().isClientSide) {
                this.singingLoopActive = true;
                this.singingLoopTimer  = 0;
                this.playHostileSinging();
            }
            AttributeInstance atk = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null && atk.getModifier(HOSTILE_DAMAGE_UUID) == null) {
                atk.addTransientModifier(HOSTILE_DAMAGE_BONUS);
            }
            this.clearArmorSlots();
        } else {
            this.singingLoopActive = false;
            this.singingLoopTimer  = 0;
            AttributeInstance atk = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) atk.removeModifier(HOSTILE_DAMAGE_UUID);
        }
        if (hostile && !this.hostilityTransformPlayed) {
            this.transformationFreezeTimer = 60; // pause dramatique 3s avant de charger
            this.playTransformationEffects();
            this.hostilityTransformPlayed = true;
        }
    }
 
    // ── Setters de cibles / apparence ─────────────────────────────────────
 
    public void setTargetPlayer(Player player) {
        this.targetPlayerUUID   = player.getUUID();
        this.targetPlayerName   = player.getName().getString();
        this.allowVoicePlayback = true;
        this.allowInventoryCopy = true;
        this.entityData.set(TARGET_PLAYER_UUID, targetPlayerUUID.toString());
        this.entityData.set(TARGET_PLAYER_NAME, targetPlayerName);
        this.timeSinceSpawn    = 0;
        this.hostileActivationTime = MIN_HOSTILE_DELAY_TICKS
                + this.random.nextInt(MAX_HOSTILE_DELAY_TICKS - MIN_HOSTILE_DELAY_TICKS + 1);
        this.hostilityTransformPlayed = false;
        this.appearancePlayerUUID = player.getUUID();
        this.entityData.set(APPEARANCE_PLAYER_UUID, this.appearancePlayerUUID.toString());
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            String tp = extractTextureProperties(sp.getGameProfile());
            if (tp != null && !tp.isEmpty()) {
                this.entityData.set(SKIN_TEXTURE_PROPERTIES, tp);
                String model = com.minesis.client.SkinTextureLoader.extractSkinModelFromProperties(tp);
                this.entityData.set(SKIN_MODEL, model != null ? model : "");
            }
        }
        this.setHostileMode(false);
        this.setCustomName(player.getName());
        this.setCustomNameVisible(true);
        this.copyPlayerInventory(player);
        this.clearHeldItems();
    }
 
    public void setAnonymousTarget(String playerName) {
        this.targetPlayerUUID     = null;
        this.appearancePlayerUUID = null;
        this.targetPlayerName     = playerName;
        this.allowVoicePlayback   = false;
        this.allowInventoryCopy   = false;
        this.entityData.set(TARGET_PLAYER_UUID,     "");
        this.entityData.set(TARGET_PLAYER_NAME,     playerName);
        this.entityData.set(APPEARANCE_PLAYER_UUID, "");
        this.timeSinceSpawn = 0;
        this.hostileActivationTime = MIN_HOSTILE_DELAY_TICKS
                + this.random.nextInt(MAX_HOSTILE_DELAY_TICKS - MIN_HOSTILE_DELAY_TICKS + 1);
        this.hostilityTransformPlayed = false;
        this.setHostileMode(false);
        this.setCustomName(net.minecraft.network.chat.Component.literal(playerName));
        this.setCustomNameVisible(true);
        this.clearHeldItems();
    }
 
    public void setAppearanceFromPlayer(Player player) {
        if (player == null) return;
        this.appearancePlayerUUID = player.getUUID();
        this.entityData.set(APPEARANCE_PLAYER_UUID, this.appearancePlayerUUID.toString());
        this.copyPlayerArmor(player);
        this.setCustomName(player.getDisplayName());
        this.setCustomNameVisible(true);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            String tp = extractTextureProperties(serverPlayer.getGameProfile());
            if (tp != null && !tp.isEmpty()) {
                this.entityData.set(SKIN_TEXTURE_PROPERTIES, tp);
                String model = com.minesis.client.SkinTextureLoader.extractSkinModelFromProperties(tp);
                if (model != null) this.entityData.set(SKIN_MODEL, model);
            }
        }
    }
 
    private String extractTextureProperties(com.mojang.authlib.GameProfile gameProfile) {
        try {
            com.mojang.authlib.properties.PropertyMap properties = gameProfile.getProperties();
            if (properties != null) {
                java.util.Collection<com.mojang.authlib.properties.Property> textures =
                        properties.get("textures");
                if (!textures.isEmpty()) return textures.iterator().next().getValue();
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to extract texture properties: " + e.getMessage());
        }
        return "";
    }
 
    public String getSkinTextureProperties() { return this.entityData.get(SKIN_TEXTURE_PROPERTIES); }
    public String getSkinModel()              { return this.entityData.get(SKIN_MODEL); }
 
    public void setAppearanceFromNameAndProperties(String playerName, UUID uuid, String textureProperties) {
        this.appearancePlayerUUID = uuid;
        this.entityData.set(APPEARANCE_PLAYER_UUID, uuid.toString());
        this.setCustomName(net.minecraft.network.chat.Component.literal(playerName));
        this.setCustomNameVisible(true);
        if (textureProperties != null && !textureProperties.isEmpty()) {
            this.entityData.set(SKIN_TEXTURE_PROPERTIES, textureProperties);
            String model = com.minesis.client.SkinTextureLoader.extractSkinModelFromProperties(textureProperties);
            if (model != null) this.entityData.set(SKIN_MODEL, model);
        }
    }
 
    public void setNaturalTarget(Player targetPlayer, @Nullable Player appearanceSource) {
        this.targetPlayerUUID   = targetPlayer.getUUID();
        this.targetPlayerName   = targetPlayer.getName().getString();
        this.allowVoicePlayback = true;
        this.allowInventoryCopy = true;
        this.entityData.set(TARGET_PLAYER_UUID,  this.targetPlayerUUID.toString());
        this.entityData.set(TARGET_PLAYER_NAME,  this.targetPlayerName);
        this.timeSinceSpawn    = 0;
        this.hostileActivationTime = MIN_HOSTILE_DELAY_TICKS
                + this.random.nextInt(MAX_HOSTILE_DELAY_TICKS - MIN_HOSTILE_DELAY_TICKS + 1);
        this.hostilityTransformPlayed = false;
        this.setCustomNameVisible(false);
        this.appearancePlayerUUID = appearanceSource == null ? null : appearanceSource.getUUID();
        this.entityData.set(APPEARANCE_PLAYER_UUID,
                this.appearancePlayerUUID == null ? "" : this.appearancePlayerUUID.toString());
        Player skinSource = appearanceSource != null ? appearanceSource : targetPlayer;
        if (skinSource instanceof net.minecraft.server.level.ServerPlayer sp) {
            String tp = extractTextureProperties(sp.getGameProfile());
            if (tp != null && !tp.isEmpty()) {
                this.entityData.set(SKIN_TEXTURE_PROPERTIES, tp);
                String model = com.minesis.client.SkinTextureLoader.extractSkinModelFromProperties(tp);
                this.entityData.set(SKIN_MODEL, model != null ? model : "");
            }
        }
        this.setHostileMode(false);
        this.clearHeldItems();
        String displayName = skinSource.getName().getString();
        this.setCustomName(net.minecraft.network.chat.Component.literal(displayName));
        this.setCustomNameVisible(true);
    }
 
    // ── Inventaire & armure ───────────────────────────────────────────────
 
    private void copyPlayerInventory(Player player) {
        if (!this.allowInventoryCopy) return;
        this.setItemSlot(EquipmentSlot.HEAD,  player.getItemBySlot(EquipmentSlot.HEAD).copy());
        this.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST).copy());
        this.setItemSlot(EquipmentSlot.LEGS,  player.getItemBySlot(EquipmentSlot.LEGS).copy());
        this.setItemSlot(EquipmentSlot.FEET,  player.getItemBySlot(EquipmentSlot.FEET).copy());
        this.setDropChance(EquipmentSlot.HEAD,     0.0F);
        this.setDropChance(EquipmentSlot.CHEST,    0.0F);
        this.setDropChance(EquipmentSlot.LEGS,     0.0F);
        this.setDropChance(EquipmentSlot.FEET,     0.0F);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.OFFHAND,  0.0F);
    }
 
    public void clearHeldItems() {
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.OFFHAND,  ItemStack.EMPTY);
    }

    public void clearArmorSlots() {
        this.setItemSlot(EquipmentSlot.HEAD,  ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.LEGS,  ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.FEET,  ItemStack.EMPTY);
    }
 
    public void copyPlayerArmor(Player player) {
        this.setItemSlot(EquipmentSlot.HEAD,  player.getItemBySlot(EquipmentSlot.HEAD).copy());
        this.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST).copy());
        this.setItemSlot(EquipmentSlot.LEGS,  player.getItemBySlot(EquipmentSlot.LEGS).copy());
        this.setItemSlot(EquipmentSlot.FEET,  player.getItemBySlot(EquipmentSlot.FEET).copy());
        this.setDropChance(EquipmentSlot.HEAD,  0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        this.setDropChance(EquipmentSlot.LEGS,  0.0F);
        this.setDropChance(EquipmentSlot.FEET,  0.0F);
    }
 
    public void addToCarriedItems(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        for (int i = 0; i < this.carriedItems.size(); i++) {
            ItemStack s = this.carriedItems.get(i);
            if (s.isEmpty()) { this.carriedItems.set(i, stack.copy()); return; }
            if (ItemStack.isSameItemSameTags(s, stack) && s.getCount() < s.getMaxStackSize()) {
                int move = Math.min(s.getMaxStackSize() - s.getCount(), stack.getCount());
                s.grow(move);
                stack.shrink(move);
                if (stack.isEmpty()) return;
            }
        }
        if (!this.level().isClientSide) {
            while (!stack.isEmpty()) {
                this.spawnAtLocation(stack.split(Math.min(stack.getCount(), stack.getMaxStackSize())));
            }
        }
    }
 
    public void collectNearbyItems(BlockPos pos) {
        if (this.level().isClientSide) return;
        if (!(this.level() instanceof ServerLevel sl)) return;
        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).inflate(1.5D);
        for (ItemEntity item : sl.getEntitiesOfClass(ItemEntity.class, box)) {
            if (item != null && !item.isRemoved()) {
                ItemStack s = item.getItem();
                if (!s.isEmpty()) this.addToCarriedItems(s.copy());
                item.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        }
    }
 
    private void collectItemsPassively() {
        if (this.level().isClientSide) return;
        if (!(this.level() instanceof ServerLevel sl)) return;
        AABB box = this.getBoundingBox().inflate(0.5D);
        for (ItemEntity item : sl.getEntitiesOfClass(ItemEntity.class, box)) {
            if (item != null && !item.isRemoved() && !item.hasPickUpDelay()) {
                ItemStack s = item.getItem();
                if (!s.isEmpty()) {
                    this.addToCarriedItems(s.copy());
                    item.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                }
            }
        }
    }
 
    // ── Sync armure ───────────────────────────────────────────────────────
 
    // ── Hotbar du joueur copié ─────────────────────────────────────────────

    private void copyPlayerHotbar(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            this.hotbarItems.set(i, item.isEmpty() ? ItemStack.EMPTY : item.copy());
        }
    }

    public boolean hasHotbarItems() {
        for (ItemStack s : this.hotbarItems) if (!s.isEmpty()) return true;
        return false;
    }

    public ItemStack pickRandomHotbarItem() {
        java.util.List<ItemStack> pool = new java.util.ArrayList<>();
        for (ItemStack s : this.hotbarItems) if (!s.isEmpty()) pool.add(s);
        if (pool.isEmpty()) return ItemStack.EMPTY;
        return pool.get(this.random.nextInt(pool.size())).copy();
    }

    public void switchToRandomHotbarItem() {
        if (!this.allowInventoryCopy) return;
        this.setItemInHand(InteractionHand.MAIN_HAND, pickRandomHotbarItem());
    }

    public NonNullList<ItemStack> getHotbarItems() { return this.hotbarItems; }

    public ItemStack findAxeInHotbar() {
        for (ItemStack s : this.hotbarItems)
            if (!s.isEmpty() && s.getItem() instanceof AxeItem) return s.copy();
        return ItemStack.EMPTY;
    }

    public ItemStack findPickaxeInHotbar() {
        for (ItemStack s : this.hotbarItems)
            if (!s.isEmpty() && s.getItem() instanceof PickaxeItem) return s.copy();
        return ItemStack.EMPTY;
    }

    public ItemStack findSwordInHotbar() {
        for (ItemStack s : this.hotbarItems)
            if (!s.isEmpty() && s.getItem() instanceof SwordItem) return s.copy();
        return ItemStack.EMPTY;
    }

    private void syncArmor() {
        if (this.isHostile) return;
        if (this.allowInventoryCopy) {
            UUID syncUUID = this.appearancePlayerUUID != null ? this.appearancePlayerUUID : this.targetPlayerUUID;
            if (syncUUID != null) {
                Player tp = this.level().getPlayerByUUID(syncUUID);
                if (tp != null) {
                    syncArmorSlot(tp, EquipmentSlot.HEAD);
                    syncArmorSlot(tp, EquipmentSlot.CHEST);
                    syncArmorSlot(tp, EquipmentSlot.LEGS);
                    syncArmorSlot(tp, EquipmentSlot.FEET);
                    copyPlayerHotbar(tp);
                }
            }
        }
    }
 
    private void syncArmorSlot(Player targetPlayer, EquipmentSlot slot) {
        ItemStack current = this.getItemBySlot(slot);
        ItemStack desired = targetPlayer.getItemBySlot(slot);
        if (!isSameArmorIgnoringDurability(current, desired))
            this.setItemSlot(slot, desired.copy());
    }
 
    private boolean isSameArmorIgnoringDurability(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() != b.isEmpty()) return false;
        ItemStack aa = a.copy(); aa.removeTagKey("Damage");
        ItemStack bb = b.copy(); bb.removeTagKey("Damage");
        return ItemStack.isSameItemSameTags(aa, bb);
    }
 
    // ── Gestion des mobs ──────────────────────────────────────────────────
 
    private void pullHostileMobsAggro() {
        if (this.level().isClientSide) return;
        for (Mob mob : this.level().getEntitiesOfClass(
                Mob.class, this.getBoundingBox().inflate(24.0D))) {
            if (mob == this || !mob.isAlive() || mob instanceof MinesisEntity) continue;
            if (mob instanceof Monster) mob.setTarget(this);
        }
    }
 
    private void clearNearbyAggro() {
        if (this.level().isClientSide) return;
        for (Mob mob : this.level().getEntitiesOfClass(
                Mob.class, this.getBoundingBox().inflate(24.0D))) {
            if (mob.getTarget() == this) mob.setTarget(null);
        }
    }
 
    @Override
    public void setTarget(@Nullable net.minecraft.world.entity.LivingEntity target) {
        if (target instanceof Mob && this.isHostileModeActive()) return;
        super.setTarget(target);
    }
 
    // ── Voix ──────────────────────────────────────────────────────────────
 
    private void attemptVoicePlayback() {
        UUID voiceUUID = this.appearancePlayerUUID != null ? this.appearancePlayerUUID : this.getTargetPlayerUUID();
        if (this.allowVoicePlayback && voiceUUID != null) {
            try {
                de.maxhenkel.voicechat.api.VoicechatServerApi api =
                        com.minesis.voice.MinesisVoiceChatPlugin.getVoicechatApi();
                if (api != null)
                    VoicePlaybackService.playVoiceClip(this, voiceUUID, api, computePlaybackContext());
            } catch (Exception ignored) {}
        }
    }
 
    // ── Effets visuels / sonores ──────────────────────────────────────────
 
    private void playTransformationEffects() {
        if (this.level().isClientSide) return;
        if (!(this.level() instanceof ServerLevel sl)) return;

        double x = this.getX(), y = this.getY(), z = this.getZ();

        // Effets sur les joueurs proches
        for (Player player : this.level().getEntitiesOfClass(
                Player.class, this.getBoundingBox().inflate(20.0D))) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,  40, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,  160, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,  80, 1, false, false, true));
        }

        // Son de transformation
        this.level().playSound(null, this.blockPosition(),
                MinesisSounds.MIMESIS_TRANSFORM.get(), SoundSource.HOSTILE, 3.0F, 1.0F);

        // Foudre cosmétique (aucun dégât, juste l'éclair)
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(this.level());
        if (bolt != null) {
            bolt.moveTo(x, y, z);
            bolt.setVisualOnly(true);
            sl.addFreshEntity(bolt);
        }

        // Grande explosion de particules rouges
        sl.sendParticles(new DustParticleOptions(new Vector3f(0.9F, 0.0F, 0.0F), 2.5F),
                x, y + 1.0D, z, 120, 0.9D, 1.5D, 0.9D, 0.3D);

        // Anneau de choc au sol
        for (int i = 0; i < 24; i++) {
            double angle = (i / 24.0) * Math.PI * 2;
            sl.sendParticles(new DustParticleOptions(new Vector3f(0.7F, 0.0F, 0.0F), 1.8F),
                    x + Math.cos(angle) * 2.5, y + 0.05D, z + Math.sin(angle) * 2.5,
                    2, 0.05D, 0.02D, 0.05D, 0.0D);
        }

        // Nuage de fumée
        sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                x, y + 1.0D, z, 25, 0.4D, 1.0D, 0.4D, 0.04D);

        // Jumpscare côté client : flash rouge + shake caméra
        for (Player player : this.level().getEntitiesOfClass(
                Player.class, this.getBoundingBox().inflate(20.0D))) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                com.minesis.network.VoiceNetworking.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                        new com.minesis.network.JumpscarePacket());
            }
        }
    }
 
    private void playHostileSinging() {
        if (this.level().isClientSide) return;
        this.level().playSound(null, this.blockPosition(),
                MinesisSounds.MIMESIS_SINGING.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }
 
    // ── Dégâts ────────────────────────────────────────────────────────────
 
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Flèche de mob (squelette…) : absorber sans dégâts ni rebond
        net.minecraft.world.entity.Entity direct = source.getDirectEntity();
        if (direct instanceof net.minecraft.world.entity.projectile.AbstractArrow
                && !(source.getEntity() instanceof Player)) {
            direct.discard(); // supprime la flèche avant qu'elle puisse rebondir
            this.provokedByMob      = true;
            this.provokedByMobTimer = 200;
            if (source.getEntity() instanceof Mob mob) {
                this.setTarget(mob);
                this.setLastHurtByMob(mob);
            }
            return false;
        }

        if (source.getEntity() instanceof Player) {
            this.playCustomHurtSound = this.isHostileModeActive();
            this.provokedByPlayer      = true;
            this.provokedByPlayerTimer = 200;
            this.setTarget((net.minecraft.world.entity.LivingEntity) source.getEntity());
            boolean tookDamage = super.hurt(source, amount);
            if (tookDamage && !this.level().isClientSide) {
                this.setHostileMode(true);
                if (this.transformationFreezeTimer == 0) {
                    this.teleportNearTargetLikeEnderman();
                }
            }
            this.playCustomHurtSound = false;
            return tookDamage;
        } else if (source.getEntity() instanceof Mob) {
            this.provokedByMob      = true;
            this.provokedByMobTimer = 200;
            this.setTarget((net.minecraft.world.entity.LivingEntity) source.getEntity());
            float healthBefore = this.getHealth();
            this.fakingHurt = true;
            boolean tookDamage = super.hurt(source, amount);
            this.fakingHurt = false;
            if (tookDamage) this.setHealth(healthBefore);
            return tookDamage;
        }
        this.playCustomHurtSound = this.isHostileModeActive();
        boolean tookDamage = super.hurt(source, amount);
        if (tookDamage && !this.level().isClientSide && this.isHostileModeActive() && this.transformationFreezeTimer == 0)
            this.teleportNearTargetLikeEnderman();
        this.playCustomHurtSound = false;
        return tookDamage;
    }
 
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        if (this.playCustomHurtSound) return MinesisSounds.MIMESIS_HURT.get();
        return super.getHurtSound(damageSource);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isHostileModeActive()) return MinesisSounds.MIMESIS_AMBIENT.get();
        return super.getAmbientSound();
    }

    @Override
    public int getAmbientSoundInterval() {
        return this.isHostileModeActive() ? 80 : super.getAmbientSoundInterval();
    }
 
    // ── Téléportation style Enderman ──────────────────────────────────────
 
    private void teleportNearTargetLikeEnderman() {
        net.minecraft.world.entity.LivingEntity target = this.getTarget();
        if (target == null) return;
        for (int i = 0; i < 16; i++) {
            double x = target.getX() + (this.random.nextDouble() * 20.0D - 10.0D);
            double y = target.getY() + (this.random.nextDouble() * 20.0D - 10.0D);
            double z = target.getZ() + (this.random.nextDouble() * 20.0D - 10.0D);
            if (this.randomTeleport(x, y, z, true)) {
                this.getNavigation().stop();
                this.setTarget(target);
                return;
            }
        }
    }
 
    public void teleportNearCurrentTargetLikeEnderman() {
        if (!this.level().isClientSide) this.teleportNearTargetLikeEnderman();
    }

    private void checkAndLeashToPlayer() {
        Player anchor = null;
        if (this.targetPlayerUUID != null) {
            anchor = this.level().getPlayerByUUID(this.targetPlayerUUID);
            if (anchor != null && !anchor.isAlive()) anchor = null;
        }
        if (anchor == null) {
            anchor = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 2048.0, false);
        }
        if (anchor == null) return;

        if (this.distanceToSqr(anchor) <= 40.0 * 40.0) return;

        double speed = this.isHostile ? 1.35D : 1.05D;
        this.getNavigation().moveTo(anchor.getX(), anchor.getY(), anchor.getZ(), speed);
        this.setSprinting(this.distanceToSqr(anchor) > 60.0 * 60.0);
    }

    // ── Station de cuisson ────────────────────────────────────────────────
 
    public void setCookingStation(BlockPos pos)  { this.cookingStationPos = pos == null ? null : pos.immutable(); }
    public void clearCookingStation()            { this.cookingStationPos = null; }
    public BlockPos getCookingStationPos()        { return this.cookingStationPos; }
 
    // ── Getters ───────────────────────────────────────────────────────────
 
    public UUID getTargetPlayerUUID() {
        String uuid = this.entityData.get(TARGET_PLAYER_UUID);
        if (uuid.isEmpty()) return null;
        try { return UUID.fromString(uuid); } catch (IllegalArgumentException e) { return null; }
    }
 
    public String getTargetPlayerName() { return this.entityData.get(TARGET_PLAYER_NAME); }
 
    public UUID getAppearancePlayerUUID() {
        String uuid = this.entityData.get(APPEARANCE_PLAYER_UUID);
        if (uuid.isEmpty()) return null;
        try { return UUID.fromString(uuid); } catch (IllegalArgumentException e) { return null; }
    }
 
    public boolean isHostile()           { return this.isHostile; }
    public boolean isHostileModeActive() { return this.entityData.get(HOSTILE_MODE); }
    public boolean isSpeaking()          { return this.entityData.get(SPEAKING); }
    public void setSpeaking(boolean b)   { this.entityData.set(SPEAKING, b); }
 
    // ── Mort / drops ──────────────────────────────────────────────────────
 
    @Override
    public void die(DamageSource damageSource) {
        if (this.fakingHurt) return; // bloque la mort pendant le flash rouge simulé
        this.singingLoopActive = false;
        this.singingLoopTimer  = 0;
        super.die(damageSource);
    }
 
    @Override
    public void dropAllDeathLoot(DamageSource damageSource) {
        if (!this.level().isClientSide) {
            for (ItemStack s : this.carriedItems) {
                if (s != null && !s.isEmpty()) this.spawnAtLocation(s.copy());
            }
        }
        super.dropAllDeathLoot(damageSource);
    }
 
    @Override
    protected void dropExperience() { /* pas d'XP */ }
 
    @Override
    public boolean removeWhenFarAway(double distanceToFurthestPlayer) { return false; }
 
    // ── NBT ───────────────────────────────────────────────────────────────
 
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.targetPlayerUUID != null)
            tag.putUUID("TargetPlayerUUID", this.targetPlayerUUID);
        if (this.targetPlayerName != null && !this.targetPlayerName.isEmpty())
            tag.putString("TargetPlayerName", this.targetPlayerName);
        tag.putInt("TimeSinceSpawn",               this.timeSinceSpawn);
        tag.putInt("HostileActivationTime",        this.hostileActivationTime);
        tag.putBoolean("IsHostile",                this.isHostile);
        tag.putBoolean("HostilityTransformPlayed", this.hostilityTransformPlayed);
        if (this.appearancePlayerUUID != null)
            tag.putUUID("AppearancePlayerUUID", this.appearancePlayerUUID);
        tag.putBoolean("AllowVoicePlayback", this.allowVoicePlayback);
        tag.putBoolean("AllowInventoryCopy", this.allowInventoryCopy);
        ListTag list = new ListTag();
        for (ItemStack s : this.carriedItems) {
            CompoundTag itemTag = new CompoundTag();
            if (s != null) s.save(itemTag);
            list.add(itemTag);
        }
        tag.put("CarriedItems", list);
    }
 
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("TargetPlayerUUID")) {
            this.targetPlayerUUID = tag.getUUID("TargetPlayerUUID");
            this.entityData.set(TARGET_PLAYER_UUID, this.targetPlayerUUID.toString());
        }
        if (tag.contains("TargetPlayerName")) {
            this.targetPlayerName = tag.getString("TargetPlayerName");
            this.entityData.set(TARGET_PLAYER_NAME, this.targetPlayerName);
        }
        this.timeSinceSpawn        = tag.getInt("TimeSinceSpawn");
        this.hostileActivationTime = tag.contains("HostileActivationTime")
                ? tag.getInt("HostileActivationTime") : this.hostileActivationTime;
        this.isHostile             = tag.contains("IsHostile") && tag.getBoolean("IsHostile");
        this.hostilityTransformPlayed = tag.contains("HostilityTransformPlayed")
                && tag.getBoolean("HostilityTransformPlayed");
        this.allowVoicePlayback    = !tag.contains("AllowVoicePlayback") || tag.getBoolean("AllowVoicePlayback");
        this.allowInventoryCopy    = !tag.contains("AllowInventoryCopy")  || tag.getBoolean("AllowInventoryCopy");
        this.entityData.set(HOSTILE_MODE, this.isHostile);
        if (tag.hasUUID("AppearancePlayerUUID")) {
            this.appearancePlayerUUID = tag.getUUID("AppearancePlayerUUID");
            this.entityData.set(APPEARANCE_PLAYER_UUID, this.appearancePlayerUUID.toString());
        } else {
            this.appearancePlayerUUID = null;
            this.entityData.set(APPEARANCE_PLAYER_UUID, "");
        }
        if (tag.contains("CarriedItems")) {
            ListTag list = tag.getList("CarriedItems", 10);
            this.carriedItems = NonNullList.withSize(Math.max(9, list.size()), ItemStack.EMPTY);
            for (int i = 0; i < list.size() && i < this.carriedItems.size(); i++) {
                this.carriedItems.set(i, ItemStack.of(list.getCompound(i)));
            }
        }
    }
}