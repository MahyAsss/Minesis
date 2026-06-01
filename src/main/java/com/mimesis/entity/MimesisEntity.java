package com.mimesis.entity;

import java.util.UUID;
import java.util.Optional;

import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LightLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.joml.Vector3f;

import com.mimesis.voice.VoiceStorage;
import com.mimesis.voice.VoicePlaybackService;
import com.mimesis.ai.MimesisAttackGoal;
import com.mimesis.ai.MimesisMobCombatGoal;
import com.mimesis.ai.MimesisPlayerLikeMovementGoal;
import com.mimesis.MimesisSounds;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nullable;

public class MimesisEntity extends Monster {
    private static final int MIN_HOSTILE_DELAY_TICKS = 20 * 60; // 1 minute
    private static final int MAX_HOSTILE_DELAY_TICKS = 20 * 180; // 3 minutes
    private static final Logger LOGGER = LogManager.getLogger();
    private static final EntityDataAccessor<String> TARGET_PLAYER_UUID = 
            SynchedEntityData.defineId(MimesisEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TARGET_PLAYER_NAME = 
            SynchedEntityData.defineId(MimesisEntity.class, EntityDataSerializers.STRING);
        private static final EntityDataAccessor<String> APPEARANCE_PLAYER_UUID = 
            SynchedEntityData.defineId(MimesisEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> BEHAVIOR_TIMER = 
            SynchedEntityData.defineId(MimesisEntity.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Boolean> HOSTILE_MODE =
            SynchedEntityData.defineId(MimesisEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> SKIN_TEXTURE_PROPERTIES = 
            SynchedEntityData.defineId(MimesisEntity.class, EntityDataSerializers.STRING);
        private static final EntityDataAccessor<String> SKIN_MODEL =
            SynchedEntityData.defineId(MimesisEntity.class, EntityDataSerializers.STRING);

    private UUID targetPlayerUUID;
    private UUID appearancePlayerUUID;
    private String targetPlayerName;
    private boolean allowVoicePlayback = true;
    private boolean allowInventoryCopy = true;
    private int timeSinceSpawn = 0;
        private int hostileActivationTime = 300; // legacy - no automatic hostility now
        private boolean isHostile = false; // kept for compatibility but not auto-set
    private int voicePlaybackCooldown = 0;
    private static final int VOICE_REPLAY_INTERVAL_MIN = 100; // 5 seconds
    private static final int VOICE_REPLAY_INTERVAL_MAX = 240; // 12 seconds
    private int armorSyncTimer = 0;
    private int hostileMobAggroTimer = 0;
    private int passiveItemCollectionTimer = 0;
    private NonNullList<ItemStack> carriedItems = NonNullList.withSize(9, ItemStack.EMPTY);
    private BlockPos cookingStationPos;
    private boolean provokedByPlayer = false;
    private int provokedByPlayerTimer = 0;
    private boolean provokedByMob = false;
    private int provokedByMobTimer = 0;
    private boolean hostilityTransformPlayed = false;
    private boolean playCustomHurtSound = false;
    private boolean singingLoopActive = false;
    private int singingLoopTimer = 0;
    private static final int SINGING_LOOP_INTERVAL = 20 * 6; // Repeat every 6 seconds (120 ticks)

    public MimesisEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 50;
        // Ensure starting HP equals the configured max (a normal player = 20)
        this.setHealth(20.0F);
    }

    public boolean isProvokedByPlayer() {
        return this.provokedByPlayer;
    }

    public boolean isProvokedByMob() {
        return this.provokedByMob;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TARGET_PLAYER_UUID, "");
        this.entityData.define(TARGET_PLAYER_NAME, "");
        this.entityData.define(APPEARANCE_PLAYER_UUID, "");
        this.entityData.define(BEHAVIOR_TIMER, 0);
        this.entityData.define(HOSTILE_MODE, false);
        this.entityData.define(SKIN_TEXTURE_PROPERTIES, "");
        this.entityData.define(SKIN_MODEL, "");
    }

    public void setTargetPlayer(Player player) {
        this.targetPlayerUUID = player.getUUID();
        this.targetPlayerName = player.getName().getString();
        this.allowVoicePlayback = true;
        this.allowInventoryCopy = true;
        this.entityData.set(TARGET_PLAYER_UUID, targetPlayerUUID.toString());
        this.entityData.set(TARGET_PLAYER_NAME, targetPlayerName);
        // Start player-like, then switch to hunt mode after a random delay.
        this.timeSinceSpawn = 0;
        this.hostileActivationTime = MIN_HOSTILE_DELAY_TICKS + this.random.nextInt(MAX_HOSTILE_DELAY_TICKS - MIN_HOSTILE_DELAY_TICKS + 1);
        this.hostilityTransformPlayed = false;
        this.appearancePlayerUUID = player.getUUID();
        this.entityData.set(APPEARANCE_PLAYER_UUID, this.appearancePlayerUUID.toString());
        this.setHostileMode(false);
        
        // Copy player appearance
        this.setCustomName(player.getName());
        this.setCustomNameVisible(true);
        
        // Copy player inventory
        this.copyPlayerInventory(player);
        this.clearHeldItems();
    }

    /**
     * Explicitly enable or disable hostile (chase) mode. When enabled, Mimesis will focus players.
     */
    public void setHostileMode(boolean hostile) {
        this.isHostile = hostile;
        this.entityData.set(HOSTILE_MODE, hostile);
        if (hostile) {
            this.clearNearbyAggro();
            // Start the singing loop when entering hostile mode
            if (!this.singingLoopActive && !this.level().isClientSide) {
                this.singingLoopActive = true;
                this.singingLoopTimer = 0;
                this.playHostileSinging();
            }
            // Ensure Mimesis equips an iron sword when hostile
            try {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            } catch (Throwable ignored) {}
        } else {
            // Stop the singing when exiting hostile mode
            this.singingLoopActive = false;
            this.singingLoopTimer = 0;
        }
        if (hostile && !this.hostilityTransformPlayed) {
            this.playTransformationEffects();
            this.hostilityTransformPlayed = true;
        }
    }

    public void setAnonymousTarget(String playerName) {
        this.targetPlayerUUID = null;
        this.appearancePlayerUUID = null;
        this.targetPlayerName = playerName;
        this.allowVoicePlayback = false;
        this.allowInventoryCopy = false;
        this.entityData.set(TARGET_PLAYER_UUID, "");
        this.entityData.set(TARGET_PLAYER_NAME, playerName);
        this.entityData.set(APPEARANCE_PLAYER_UUID, "");
        this.timeSinceSpawn = 0;
        this.hostileActivationTime = MIN_HOSTILE_DELAY_TICKS + this.random.nextInt(MAX_HOSTILE_DELAY_TICKS - MIN_HOSTILE_DELAY_TICKS + 1);
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
        
        // Extract and store texture properties from GameProfile (like custom heads do)
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            String textureProperties = extractTextureProperties(serverPlayer.getGameProfile());
            if (textureProperties != null && !textureProperties.isEmpty()) {
                this.entityData.set(SKIN_TEXTURE_PROPERTIES, textureProperties);
                // Also extract model metadata (slim/default) when available
                String model = com.mimesis.client.SkinTextureLoader.extractSkinModelFromProperties(textureProperties);
                if (model != null) {
                    this.entityData.set(SKIN_MODEL, model);
                }
            }
        }
    }
    
    private String extractTextureProperties(com.mojang.authlib.GameProfile gameProfile) {
        try {
            com.mojang.authlib.properties.PropertyMap properties = gameProfile.getProperties();
            if (properties != null) {
                java.util.Collection<com.mojang.authlib.properties.Property> textures = properties.get("textures");
                if (!textures.isEmpty()) {
                    com.mojang.authlib.properties.Property textureProp = textures.iterator().next();
                    // Return base64 encoded texture properties
                    return textureProp.getValue();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to extract texture properties: " + e.getMessage());
        }
        return "";
    }
    
    public String getSkinTextureProperties() {
        return this.entityData.get(SKIN_TEXTURE_PROPERTIES);
    }

    /**
     * Set appearance from player name and texture properties (for offline players via Mojang API).
     */
    public void setAppearanceFromNameAndProperties(String playerName, UUID uuid, String textureProperties) {
        this.appearancePlayerUUID = uuid;
        this.entityData.set(APPEARANCE_PLAYER_UUID, uuid.toString());
        this.setCustomName(net.minecraft.network.chat.Component.literal(playerName));
        this.setCustomNameVisible(true);
        
        if (textureProperties != null && !textureProperties.isEmpty()) {
            this.entityData.set(SKIN_TEXTURE_PROPERTIES, textureProperties);
            // Also capture model metadata if provided by Mojang profile
            String model = com.mimesis.client.SkinTextureLoader.extractSkinModelFromProperties(textureProperties);
            if (model != null) {
                this.entityData.set(SKIN_MODEL, model);
            }
        }
    }

    public String getSkinModel() {
        return this.entityData.get(SKIN_MODEL);
    }

    public void setNaturalTarget(Player targetPlayer, @Nullable Player appearanceSource) {
        this.targetPlayerUUID = targetPlayer.getUUID();
        this.targetPlayerName = targetPlayer.getName().getString();
        this.allowVoicePlayback = true;
        this.allowInventoryCopy = true;
        this.entityData.set(TARGET_PLAYER_UUID, this.targetPlayerUUID.toString());
        this.entityData.set(TARGET_PLAYER_NAME, this.targetPlayerName);
        this.timeSinceSpawn = 0;
        this.hostileActivationTime = MIN_HOSTILE_DELAY_TICKS + this.random.nextInt(MAX_HOSTILE_DELAY_TICKS - MIN_HOSTILE_DELAY_TICKS + 1);
        this.hostilityTransformPlayed = false;
        this.setCustomNameVisible(false);

        this.appearancePlayerUUID = appearanceSource == null ? null : appearanceSource.getUUID();
        this.entityData.set(APPEARANCE_PLAYER_UUID, this.appearancePlayerUUID == null ? "" : this.appearancePlayerUUID.toString());

        this.setHostileMode(false);
        this.clearHeldItems();
        // Keep a hidden custom name so minimap/radar name renderers can use it like players.
        this.setCustomName(net.minecraft.network.chat.Component.literal(this.targetPlayerName));
        this.setCustomNameVisible(false);
    }
    
    private void copyPlayerInventory(Player player) {
        if (!this.allowInventoryCopy) {
            return;
        }

        // Copy armor with proper visibility
        this.setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.HEAD).copy());
        this.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST).copy());
        this.setItemSlot(EquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS).copy());
        this.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.FEET).copy());
        
        // Ensure armor is visible - set drop chances to 0 (never drop) so it persists
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        this.setDropChance(EquipmentSlot.LEGS, 0.0F);
        this.setDropChance(EquipmentSlot.FEET, 0.0F);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    public void clearHeldItems() {
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    public void addToCarriedItems(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        // Try to merge into existing stacks
        for (int i = 0; i < this.carriedItems.size(); i++) {
            ItemStack s = this.carriedItems.get(i);
            if (s.isEmpty()) {
                this.carriedItems.set(i, stack.copy());
                return;
            }
            if (ItemStack.isSameItemSameTags(s, stack) && s.getCount() < s.getMaxStackSize()) {
                int space = s.getMaxStackSize() - s.getCount();
                int move = Math.min(space, stack.getCount());
                s.grow(move);
                stack.shrink(move);
                if (stack.isEmpty()) return;
            }
        }

        // If still items left, spawn them into the world at the entity position
        if (!this.level().isClientSide) {
            Vec3 pos = this.position();
            while (!stack.isEmpty()) {
                ItemStack take = stack.split(Math.min(stack.getCount(), stack.getMaxStackSize()));
                this.spawnAtLocation(take);
            }
        }
    }

    public void collectNearbyItems(BlockPos pos) {
        if (this.level().isClientSide) return;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).inflate(1.5D);
        for (ItemEntity item : serverLevel.getEntitiesOfClass(ItemEntity.class, box)) {
            if (item != null && !item.isRemoved()) {
                ItemStack stack = item.getItem();
                if (!stack.isEmpty()) {
                    this.addToCarriedItems(stack.copy());
                }
                item.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private void collectItemsPassively() {
        if (this.level().isClientSide) return;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        // Collect only items touching the entity (0.5 block radius)
        AABB box = this.getBoundingBox().inflate(0.5D);
        for (ItemEntity item : serverLevel.getEntitiesOfClass(ItemEntity.class, box)) {
            if (item != null && !item.isRemoved() && !item.hasPickUpDelay()) {
                ItemStack stack = item.getItem();
                if (!stack.isEmpty()) {
                    this.addToCarriedItems(stack.copy());
                    item.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                }
            }
        }
    }

    public void copyPlayerArmor(Player player) {
        this.setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.HEAD).copy());
        this.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST).copy());
        this.setItemSlot(EquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS).copy());
        this.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.FEET).copy());

        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        this.setDropChance(EquipmentSlot.LEGS, 0.0F);
        this.setDropChance(EquipmentSlot.FEET, 0.0F);
    }

    public UUID getTargetPlayerUUID() {
        String uuid = this.entityData.get(TARGET_PLAYER_UUID);
        if (uuid.isEmpty()) return null;
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getTargetPlayerName() {
        return this.entityData.get(TARGET_PLAYER_NAME);
    }

    public UUID getAppearancePlayerUUID() {
        String uuid = this.entityData.get(APPEARANCE_PLAYER_UUID);
        if (uuid.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isHostile() {
        return this.isHostile;
    }

    public boolean isHostileModeActive() {
        return this.entityData.get(HOSTILE_MODE);
    }

    public void setCookingStation(BlockPos pos) {
        this.cookingStationPos = pos == null ? null : pos.immutable();
    }

    public void clearCookingStation() {
        this.cookingStationPos = null;
    }

    public BlockPos getCookingStationPos() {
        return this.cookingStationPos;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MimesisMobCombatGoal(this, 1.15D));
        this.goalSelector.addGoal(2, new MimesisAttackGoal(this));
        this.goalSelector.addGoal(3, new MimesisPlayerLikeMovementGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            this.timeSinceSpawn++;

            // Switch to hunt mode only when a player gets close enough.
            if (!this.isHostile && this.shouldEnterHostileMode()) {
                this.setHostileMode(true);
            }

            // Clear provocation timers
            if (this.provokedByPlayerTimer > 0) {
                this.provokedByPlayerTimer--;
                if (this.provokedByPlayerTimer <= 0) this.provokedByPlayer = false;
            }
            if (this.provokedByMobTimer > 0) {
                this.provokedByMobTimer--;
                if (this.provokedByMobTimer <= 0) this.provokedByMob = false;
            }

            // Attempt to replay voice
            if (this.voicePlaybackCooldown <= 0 && this.isAlive()) {
                this.attemptVoicePlayback();
                this.voicePlaybackCooldown = VOICE_REPLAY_INTERVAL_MIN
                        + this.random.nextInt(VOICE_REPLAY_INTERVAL_MAX - VOICE_REPLAY_INTERVAL_MIN + 1);
            } else {
                this.voicePlaybackCooldown--;
            }

            // Handle singing loop when in hostile mode
            if (this.singingLoopActive) {
                this.singingLoopTimer++;
                if (this.singingLoopTimer >= SINGING_LOOP_INTERVAL) {
                    this.playHostileSinging();
                    this.singingLoopTimer = 0;
                }
            }

            // Sync armor every 20 ticks to ensure it stays visible
            this.armorSyncTimer++;
            if (this.armorSyncTimer >= 20) {
                this.syncArmor();
                this.armorSyncTimer = 0;
            }

            if (!this.isHostileModeActive()) {
                this.hostileMobAggroTimer++;
                if (this.hostileMobAggroTimer >= 40) {
                    this.pullHostileMobsAggro();
                    this.hostileMobAggroTimer = 0;
                }
            } else {
                this.hostileMobAggroTimer = 0;
            }

            // Passive item collection: every 5 ticks, collect items near the entity
            this.passiveItemCollectionTimer++;
            if (this.passiveItemCollectionTimer >= 5) {
                this.collectItemsPassively();
                this.passiveItemCollectionTimer = 0;
            }

            this.entityData.set(BEHAVIOR_TIMER, this.timeSinceSpawn);
        }
    }

    private void syncArmor() {
        if (this.allowInventoryCopy && this.targetPlayerUUID != null) {
            Player targetPlayer = this.level().getPlayerByUUID(this.targetPlayerUUID);
            if (targetPlayer != null) {
                this.syncArmorSlotFromPlayer(targetPlayer, EquipmentSlot.HEAD);
                this.syncArmorSlotFromPlayer(targetPlayer, EquipmentSlot.CHEST);
                this.syncArmorSlotFromPlayer(targetPlayer, EquipmentSlot.LEGS);
                this.syncArmorSlotFromPlayer(targetPlayer, EquipmentSlot.FEET);
            }
        }
    }

    private void syncArmorSlotFromPlayer(Player targetPlayer, EquipmentSlot slot) {
        ItemStack current = this.getItemBySlot(slot);
        ItemStack desired = targetPlayer.getItemBySlot(slot);

        if (!this.isSameArmorIgnoringDurability(current, desired)) {
            this.setItemSlot(slot, desired.copy());
        }
    }

    private boolean isSameArmorIgnoringDurability(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) {
            return true;
        }
        if (a.isEmpty() != b.isEmpty()) {
            return false;
        }

        ItemStack aa = a.copy();
        ItemStack bb = b.copy();
        aa.removeTagKey("Damage");
        bb.removeTagKey("Damage");
        return ItemStack.isSameItemSameTags(aa, bb);
    }

    private void pullHostileMobsAggro() {
        if (this.level().isClientSide) {
            return;
        }

        for (Mob mob : this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(24.0D))) {
            if (mob == this || !mob.isAlive() || mob instanceof MimesisEntity) {
                continue;
            }

            if (mob instanceof net.minecraft.world.entity.monster.Monster) {
                mob.setTarget(this);
            }
        }
    }

    private void clearNearbyAggro() {
        if (this.level().isClientSide) {
            return;
        }

        for (Mob mob : this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(24.0D))) {
            if (mob.getTarget() == this) {
                mob.setTarget(null);
            }
        }
    }

    @Override
    public void setTarget(@Nullable net.minecraft.world.entity.LivingEntity target) {
        // Prevent other mobs from setting Mimesis as a target while it is in hostile (hunt) mode.
        if (target instanceof Mob && this.isHostileModeActive()) {
            return;
        }
        super.setTarget(target);
    }

    private boolean shouldEnterHostileMode() {
        if (this.getTargetPlayerName().isEmpty()) {
            return false;
        }

        Player nearestPlayer = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 5.0D, false);
        return nearestPlayer != null;
    }

    private void attemptVoicePlayback() {
        UUID playerUUID = this.getTargetPlayerUUID();
        if (this.allowVoicePlayback && playerUUID != null) {
            // Use VoicePlaybackService to handle playback through Voice Chat API
            try {
                de.maxhenkel.voicechat.api.VoicechatServerApi api = com.mimesis.voice.MimesisVoiceChatPlugin.getVoicechatApi();
                if (api != null) {
                    VoicePlaybackService.playVoiceClip(this, playerUUID, api);
                }
            } catch (Exception e) {
                // Silently ignore if Voice Chat API is not available
            }
        }
    }

    @Override
    public void dropAllDeathLoot(net.minecraft.world.damagesource.DamageSource damageSource) {
        // Drop carried items on death so players can loot what Mimesis collected
        if (!this.level().isClientSide) {
            for (ItemStack s : this.carriedItems) {
                if (s != null && !s.isEmpty()) {
                    this.spawnAtLocation(s.copy());
                }
            }
        }
        // Always call parent to ensure standard drops
        super.dropAllDeathLoot(damageSource);
    }

    @Override
    protected void dropExperience() {
        // Don't drop experience
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.targetPlayerUUID != null) {
            tag.putUUID("TargetPlayerUUID", this.targetPlayerUUID);
            tag.putString("TargetPlayerName", this.targetPlayerName);
            tag.putInt("TimeSinceSpawn", this.timeSinceSpawn);
            tag.putInt("HostileActivationTime", this.hostileActivationTime);
            tag.putBoolean("IsHostile", this.isHostile);
            tag.putBoolean("HostilityTransformPlayed", this.hostilityTransformPlayed);
        }
        if (this.appearancePlayerUUID != null) {
            tag.putUUID("AppearancePlayerUUID", this.appearancePlayerUUID);
        }
        tag.putBoolean("AllowVoicePlayback", this.allowVoicePlayback);
        tag.putBoolean("AllowInventoryCopy", this.allowInventoryCopy);
        // Save carried items
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
            this.targetPlayerName = tag.getString("TargetPlayerName");
            this.timeSinceSpawn = tag.getInt("TimeSinceSpawn");
            this.hostileActivationTime = tag.contains("HostileActivationTime") ? tag.getInt("HostileActivationTime") : this.hostileActivationTime;
            this.isHostile = tag.contains("IsHostile") ? tag.getBoolean("IsHostile") : false;
            this.hostilityTransformPlayed = tag.contains("HostilityTransformPlayed") ? tag.getBoolean("HostilityTransformPlayed") : false;
            this.allowVoicePlayback = tag.contains("AllowVoicePlayback") ? tag.getBoolean("AllowVoicePlayback") : true;
            this.allowInventoryCopy = tag.contains("AllowInventoryCopy") ? tag.getBoolean("AllowInventoryCopy") : true;
            this.entityData.set(TARGET_PLAYER_UUID, this.targetPlayerUUID.toString());
            this.entityData.set(TARGET_PLAYER_NAME, this.targetPlayerName);
            this.entityData.set(HOSTILE_MODE, this.isHostile);
            if (tag.hasUUID("AppearancePlayerUUID")) {
                this.appearancePlayerUUID = tag.getUUID("AppearancePlayerUUID");
                this.entityData.set(APPEARANCE_PLAYER_UUID, this.appearancePlayerUUID.toString());
            } else {
                this.appearancePlayerUUID = null;
                this.entityData.set(APPEARANCE_PLAYER_UUID, "");
            }
        } else {
            this.allowVoicePlayback = tag.contains("AllowVoicePlayback") ? tag.getBoolean("AllowVoicePlayback") : true;
            this.allowInventoryCopy = tag.contains("AllowInventoryCopy") ? tag.getBoolean("AllowInventoryCopy") : true;
            this.entityData.set(HOSTILE_MODE, false);
            if (tag.hasUUID("AppearancePlayerUUID")) {
                this.appearancePlayerUUID = tag.getUUID("AppearancePlayerUUID");
                this.entityData.set(APPEARANCE_PLAYER_UUID, this.appearancePlayerUUID.toString());
            } else {
                this.appearancePlayerUUID = null;
                this.entityData.set(APPEARANCE_PLAYER_UUID, "");
            }
        }
        if (tag.contains("CarriedItems")) {
            ListTag list = tag.getList("CarriedItems", 10);
            this.carriedItems = NonNullList.withSize(Math.max(9, list.size()), ItemStack.EMPTY);
            for (int i = 0; i < list.size() && i < this.carriedItems.size(); i++) {
                this.carriedItems.set(i, ItemStack.of(list.getCompound(i)));
            }
        }
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // Players can damage Mimesis (return true = apply damage)
        // Mobs cannot damage Mimesis (return false = ignore damage, just aggro)
        if (source.getEntity() instanceof Player) {
            this.playCustomHurtSound = this.isHostileModeActive();
            this.provokedByPlayer = true;
            this.provokedByPlayerTimer = 200; // ~10s of aggression/defense
            this.setTarget((net.minecraft.world.entity.LivingEntity) source.getEntity());
            // Allow player damage
            boolean tookDamage = super.hurt(source, amount);
            if (tookDamage && !this.level().isClientSide) {
                this.setHostileMode(true);
                this.teleportNearTargetLikeEnderman();
            }
            this.playCustomHurtSound = false;
            return tookDamage;
        } else if (source.getEntity() instanceof Mob) {
            this.playCustomHurtSound = this.isHostileModeActive();
            this.provokedByMob = true;
            this.provokedByMobTimer = 200;
            this.setTarget((net.minecraft.world.entity.LivingEntity) source.getEntity());
            this.setLastHurtByMob((Mob) source.getEntity());
            // Ignore mob damage but play hurt sound and aggro
            this.playHurtSound(source);
            this.playCustomHurtSound = false;
            return false;
        }
        // Other damage sources (environment, etc.) are allowed
        this.playCustomHurtSound = this.isHostileModeActive();
        boolean tookDamage = super.hurt(source, amount);
        if (tookDamage && !this.level().isClientSide) {
            this.teleportNearTargetLikeEnderman();
        }
        this.playCustomHurtSound = false;
        return tookDamage;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        if (this.playCustomHurtSound) {
            return MimesisSounds.MIMESIS_HURT.get();
        }
        return super.getHurtSound(damageSource);
    }

    private void teleportNearTargetLikeEnderman() {
        net.minecraft.world.entity.LivingEntity target = this.getTarget();
        if (target == null) {
            return;
        }

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
        if (!this.level().isClientSide) {
            this.teleportNearTargetLikeEnderman();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToFurthestPlayer) {
        // Never despawn, regardless of distance
        return false;
    }

    private void playTransformationEffects() {
        if (this.level().isClientSide) {
            return;
        }

        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(20.0D))) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
        }

        SoundEvent scream = MimesisSounds.MIMESIS_SCREAM.get();
        this.level().playSound(null, this.blockPosition(), scream, SoundSource.HOSTILE, 1.0F, 1.0F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 1.6F),
                    this.getX(),
                    this.getY() + 1.0D,
                    this.getZ(),
                    40,
                    0.5D,
                    1.0D,
                    0.5D,
                    0.08D);
        }
    }

    private void playHostileSinging() {
        if (this.level().isClientSide) {
            return;
        }

        SoundEvent singing = MimesisSounds.MIMESIS_SINGING.get();
        this.level().playSound(null, this.blockPosition(), singing, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    @Override
    public void die(DamageSource damageSource) {
        // Stop the singing loop when the entity dies
        this.singingLoopActive = false;
        this.singingLoopTimer = 0;
        super.die(damageSource);
    }
}
