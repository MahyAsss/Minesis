/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.properties.Property
 *  com.mojang.authlib.properties.PropertyMap
 *  de.maxhenkel.voicechat.api.VoicechatServerApi
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.NonNullList
 *  net.minecraft.core.particles.DustParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.ai.attributes.AttributeSupplier$Builder
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.ai.goal.FloatGoal
 *  net.minecraft.world.entity.ai.goal.Goal
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.monster.Monster
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.joml.Vector3f
 */
package com.mimesis.entity;

import com.mimesis.MimesisSounds;
import com.mimesis.ai.MimesisAttackGoal;
import com.mimesis.ai.MimesisMobCombatGoal;
import com.mimesis.ai.MimesisPlayerLikeMovementGoal;
import com.mimesis.client.SkinTextureLoader;
import com.mimesis.voice.MimesisVoiceChatPlugin;
import com.mimesis.voice.VoicePlaybackService;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import java.util.Collection;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

public class MimesisEntity
extends Monster {
    private static final int MIN_HOSTILE_DELAY_TICKS = 1200;
    private static final int MAX_HOSTILE_DELAY_TICKS = 3600;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final EntityDataAccessor<String> TARGET_PLAYER_UUID = SynchedEntityData.m_135353_(MimesisEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135030_);
    private static final EntityDataAccessor<String> TARGET_PLAYER_NAME = SynchedEntityData.m_135353_(MimesisEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135030_);
    private static final EntityDataAccessor<String> APPEARANCE_PLAYER_UUID = SynchedEntityData.m_135353_(MimesisEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135030_);
    private static final EntityDataAccessor<Integer> BEHAVIOR_TIMER = SynchedEntityData.m_135353_(MimesisEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    private static final EntityDataAccessor<Boolean> HOSTILE_MODE = SynchedEntityData.m_135353_(MimesisEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    private static final EntityDataAccessor<String> SKIN_TEXTURE_PROPERTIES = SynchedEntityData.m_135353_(MimesisEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135030_);
    private static final EntityDataAccessor<String> SKIN_MODEL = SynchedEntityData.m_135353_(MimesisEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135030_);
    private UUID targetPlayerUUID;
    private UUID appearancePlayerUUID;
    private String targetPlayerName;
    private boolean allowVoicePlayback = true;
    private boolean allowInventoryCopy = true;
    private int timeSinceSpawn = 0;
    private int hostileActivationTime = 300;
    private boolean isHostile = false;
    private int voicePlaybackCooldown = 0;
    private static final int VOICE_REPLAY_INTERVAL_MIN = 100;
    private static final int VOICE_REPLAY_INTERVAL_MAX = 240;
    private int armorSyncTimer = 0;
    private int hostileMobAggroTimer = 0;
    private int passiveItemCollectionTimer = 0;
    private NonNullList<ItemStack> carriedItems = NonNullList.m_122780_((int)9, (Object)ItemStack.f_41583_);
    private BlockPos cookingStationPos;
    private boolean provokedByPlayer = false;
    private int provokedByPlayerTimer = 0;
    private boolean provokedByMob = false;
    private int provokedByMobTimer = 0;
    private boolean hostilityTransformPlayed = false;
    private boolean playCustomHurtSound = false;
    private boolean singingLoopActive = false;
    private int singingLoopTimer = 0;
    private static final int SINGING_LOOP_INTERVAL = 120;

    public MimesisEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.f_21364_ = 50;
    }

    public boolean isProvokedByPlayer() {
        return this.provokedByPlayer;
    }

    public boolean isProvokedByMob() {
        return this.provokedByMob;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.m_33035_().m_22268_(Attributes.f_22276_, 30.0).m_22268_(Attributes.f_22279_, 0.28).m_22268_(Attributes.f_22281_, 4.0).m_22268_(Attributes.f_22277_, 64.0);
    }

    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(TARGET_PLAYER_UUID, (Object)"");
        this.f_19804_.m_135372_(TARGET_PLAYER_NAME, (Object)"");
        this.f_19804_.m_135372_(APPEARANCE_PLAYER_UUID, (Object)"");
        this.f_19804_.m_135372_(BEHAVIOR_TIMER, (Object)0);
        this.f_19804_.m_135372_(HOSTILE_MODE, (Object)false);
        this.f_19804_.m_135372_(SKIN_TEXTURE_PROPERTIES, (Object)"");
        this.f_19804_.m_135372_(SKIN_MODEL, (Object)"");
    }

    public void setTargetPlayer(Player player) {
        this.targetPlayerUUID = player.m_20148_();
        this.targetPlayerName = player.m_7755_().getString();
        this.allowVoicePlayback = true;
        this.allowInventoryCopy = true;
        this.f_19804_.m_135381_(TARGET_PLAYER_UUID, (Object)this.targetPlayerUUID.toString());
        this.f_19804_.m_135381_(TARGET_PLAYER_NAME, (Object)this.targetPlayerName);
        this.timeSinceSpawn = 0;
        this.hostileActivationTime = 1200 + this.f_19796_.m_188503_(2401);
        this.hostilityTransformPlayed = false;
        this.appearancePlayerUUID = player.m_20148_();
        this.f_19804_.m_135381_(APPEARANCE_PLAYER_UUID, (Object)this.appearancePlayerUUID.toString());
        this.setHostileMode(false);
        this.m_6593_(player.m_7755_());
        this.m_20340_(true);
        this.copyPlayerInventory(player);
        this.clearHeldItems();
    }

    public void setHostileMode(boolean hostile) {
        this.isHostile = hostile;
        this.f_19804_.m_135381_(HOSTILE_MODE, (Object)hostile);
        if (hostile) {
            this.clearNearbyAggro();
            if (!this.singingLoopActive && !this.m_9236_().f_46443_) {
                this.singingLoopActive = true;
                this.singingLoopTimer = 0;
                this.playHostileSinging();
            }
        } else {
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
        this.f_19804_.m_135381_(TARGET_PLAYER_UUID, (Object)"");
        this.f_19804_.m_135381_(TARGET_PLAYER_NAME, (Object)playerName);
        this.f_19804_.m_135381_(APPEARANCE_PLAYER_UUID, (Object)"");
        this.timeSinceSpawn = 0;
        this.hostileActivationTime = 1200 + this.f_19796_.m_188503_(2401);
        this.hostilityTransformPlayed = false;
        this.setHostileMode(false);
        this.m_6593_((Component)Component.m_237113_((String)playerName));
        this.m_20340_(true);
        this.clearHeldItems();
    }

    public void setAppearanceFromPlayer(Player player) {
        ServerPlayer serverPlayer;
        String textureProperties;
        if (player == null) {
            return;
        }
        this.appearancePlayerUUID = player.m_20148_();
        this.f_19804_.m_135381_(APPEARANCE_PLAYER_UUID, (Object)this.appearancePlayerUUID.toString());
        this.copyPlayerArmor(player);
        this.m_6593_(player.m_5446_());
        this.m_20340_(true);
        if (player instanceof ServerPlayer && (textureProperties = this.extractTextureProperties((serverPlayer = (ServerPlayer)player).m_36316_())) != null && !textureProperties.isEmpty()) {
            this.f_19804_.m_135381_(SKIN_TEXTURE_PROPERTIES, (Object)textureProperties);
            String model = SkinTextureLoader.extractSkinModelFromProperties(textureProperties);
            if (model != null) {
                this.f_19804_.m_135381_(SKIN_MODEL, (Object)model);
            }
        }
    }

    private String extractTextureProperties(GameProfile gameProfile) {
        try {
            Collection textures;
            PropertyMap properties = gameProfile.getProperties();
            if (properties != null && !(textures = properties.get((Object)"textures")).isEmpty()) {
                Property textureProp = (Property)textures.iterator().next();
                return textureProp.getValue();
            }
        }
        catch (Exception e) {
            LOGGER.debug("Failed to extract texture properties: " + e.getMessage());
        }
        return "";
    }

    public String getSkinTextureProperties() {
        return (String)this.f_19804_.m_135370_(SKIN_TEXTURE_PROPERTIES);
    }

    public void setAppearanceFromNameAndProperties(String playerName, UUID uuid, String textureProperties) {
        this.appearancePlayerUUID = uuid;
        this.f_19804_.m_135381_(APPEARANCE_PLAYER_UUID, (Object)uuid.toString());
        this.m_6593_((Component)Component.m_237113_((String)playerName));
        this.m_20340_(true);
        if (textureProperties != null && !textureProperties.isEmpty()) {
            this.f_19804_.m_135381_(SKIN_TEXTURE_PROPERTIES, (Object)textureProperties);
            String model = SkinTextureLoader.extractSkinModelFromProperties(textureProperties);
            if (model != null) {
                this.f_19804_.m_135381_(SKIN_MODEL, (Object)model);
            }
        }
    }

    public String getSkinModel() {
        return (String)this.f_19804_.m_135370_(SKIN_MODEL);
    }

    public void setNaturalTarget(Player targetPlayer, @Nullable Player appearanceSource) {
        this.targetPlayerUUID = targetPlayer.m_20148_();
        this.targetPlayerName = targetPlayer.m_7755_().getString();
        this.allowVoicePlayback = true;
        this.allowInventoryCopy = true;
        this.f_19804_.m_135381_(TARGET_PLAYER_UUID, (Object)this.targetPlayerUUID.toString());
        this.f_19804_.m_135381_(TARGET_PLAYER_NAME, (Object)this.targetPlayerName);
        this.timeSinceSpawn = 0;
        this.hostileActivationTime = 1200 + this.f_19796_.m_188503_(2401);
        this.hostilityTransformPlayed = false;
        this.m_20340_(false);
        this.appearancePlayerUUID = appearanceSource == null ? null : appearanceSource.m_20148_();
        this.f_19804_.m_135381_(APPEARANCE_PLAYER_UUID, (Object)(this.appearancePlayerUUID == null ? "" : this.appearancePlayerUUID.toString()));
        this.setHostileMode(false);
        this.clearHeldItems();
        this.m_6593_((Component)Component.m_237113_((String)this.targetPlayerName));
        this.m_20340_(false);
    }

    private void copyPlayerInventory(Player player) {
        if (!this.allowInventoryCopy) {
            return;
        }
        this.m_8061_(EquipmentSlot.HEAD, player.m_6844_(EquipmentSlot.HEAD).m_41777_());
        this.m_8061_(EquipmentSlot.CHEST, player.m_6844_(EquipmentSlot.CHEST).m_41777_());
        this.m_8061_(EquipmentSlot.LEGS, player.m_6844_(EquipmentSlot.LEGS).m_41777_());
        this.m_8061_(EquipmentSlot.FEET, player.m_6844_(EquipmentSlot.FEET).m_41777_());
        this.m_21409_(EquipmentSlot.HEAD, 0.0f);
        this.m_21409_(EquipmentSlot.CHEST, 0.0f);
        this.m_21409_(EquipmentSlot.LEGS, 0.0f);
        this.m_21409_(EquipmentSlot.FEET, 0.0f);
        this.m_21409_(EquipmentSlot.MAINHAND, 0.0f);
        this.m_21409_(EquipmentSlot.OFFHAND, 0.0f);
    }

    public void clearHeldItems() {
        this.m_8061_(EquipmentSlot.MAINHAND, ItemStack.f_41583_);
        this.m_8061_(EquipmentSlot.OFFHAND, ItemStack.f_41583_);
    }

    public void addToCarriedItems(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return;
        }
        for (int i = 0; i < this.carriedItems.size(); ++i) {
            ItemStack s = (ItemStack)this.carriedItems.get(i);
            if (s.m_41619_()) {
                this.carriedItems.set(i, (Object)stack.m_41777_());
                return;
            }
            if (!ItemStack.m_150942_((ItemStack)s, (ItemStack)stack) || s.m_41613_() >= s.m_41741_()) continue;
            int space = s.m_41741_() - s.m_41613_();
            int move = Math.min(space, stack.m_41613_());
            s.m_41769_(move);
            stack.m_41774_(move);
            if (!stack.m_41619_()) continue;
            return;
        }
        if (!this.m_9236_().f_46443_) {
            Vec3 pos = this.m_20182_();
            while (!stack.m_41619_()) {
                ItemStack take = stack.m_41620_(Math.min(stack.m_41613_(), stack.m_41741_()));
                this.m_19983_(take);
            }
        }
    }

    public void collectNearbyItems(BlockPos pos) {
        if (this.m_9236_().f_46443_) {
            return;
        }
        Level level = this.m_9236_();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        AABB box = new AABB((double)pos.m_123341_(), (double)pos.m_123342_(), (double)pos.m_123343_(), (double)(pos.m_123341_() + 1), (double)(pos.m_123342_() + 1), (double)(pos.m_123343_() + 1)).m_82400_(1.5);
        for (ItemEntity item : serverLevel.m_45976_(ItemEntity.class, box)) {
            if (item == null || item.m_213877_()) continue;
            ItemStack stack = item.m_32055_();
            if (!stack.m_41619_()) {
                this.addToCarriedItems(stack.m_41777_());
            }
            item.m_142687_(Entity.RemovalReason.DISCARDED);
        }
    }

    private void collectItemsPassively() {
        if (this.m_9236_().f_46443_) {
            return;
        }
        Level level = this.m_9236_();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        AABB box = this.m_20191_().m_82400_(0.5);
        for (ItemEntity item : serverLevel.m_45976_(ItemEntity.class, box)) {
            ItemStack stack;
            if (item == null || item.m_213877_() || item.m_32063_() || (stack = item.m_32055_()).m_41619_()) continue;
            this.addToCarriedItems(stack.m_41777_());
            item.m_142687_(Entity.RemovalReason.DISCARDED);
        }
    }

    public void copyPlayerArmor(Player player) {
        this.m_8061_(EquipmentSlot.HEAD, player.m_6844_(EquipmentSlot.HEAD).m_41777_());
        this.m_8061_(EquipmentSlot.CHEST, player.m_6844_(EquipmentSlot.CHEST).m_41777_());
        this.m_8061_(EquipmentSlot.LEGS, player.m_6844_(EquipmentSlot.LEGS).m_41777_());
        this.m_8061_(EquipmentSlot.FEET, player.m_6844_(EquipmentSlot.FEET).m_41777_());
        this.m_21409_(EquipmentSlot.HEAD, 0.0f);
        this.m_21409_(EquipmentSlot.CHEST, 0.0f);
        this.m_21409_(EquipmentSlot.LEGS, 0.0f);
        this.m_21409_(EquipmentSlot.FEET, 0.0f);
    }

    public UUID getTargetPlayerUUID() {
        String uuid = (String)this.f_19804_.m_135370_(TARGET_PLAYER_UUID);
        if (uuid.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(uuid);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getTargetPlayerName() {
        return (String)this.f_19804_.m_135370_(TARGET_PLAYER_NAME);
    }

    public UUID getAppearancePlayerUUID() {
        String uuid = (String)this.f_19804_.m_135370_(APPEARANCE_PLAYER_UUID);
        if (uuid.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(uuid);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isHostile() {
        return this.isHostile;
    }

    public boolean isHostileModeActive() {
        return (Boolean)this.f_19804_.m_135370_(HOSTILE_MODE);
    }

    public void setCookingStation(BlockPos pos) {
        this.cookingStationPos = pos == null ? null : pos.m_7949_();
    }

    public void clearCookingStation() {
        this.cookingStationPos = null;
    }

    public BlockPos getCookingStationPos() {
        return this.cookingStationPos;
    }

    protected void m_8099_() {
        this.f_21345_.m_25352_(0, (Goal)new FloatGoal((Mob)this));
        this.f_21345_.m_25352_(1, (Goal)new MimesisMobCombatGoal(this, 1.15));
        this.f_21345_.m_25352_(2, (Goal)new MimesisAttackGoal(this));
        this.f_21345_.m_25352_(3, (Goal)new MimesisPlayerLikeMovementGoal(this));
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            ++this.timeSinceSpawn;
            if (!this.isHostile && this.shouldEnterHostileMode()) {
                this.setHostileMode(true);
            }
            if (this.provokedByPlayerTimer > 0) {
                --this.provokedByPlayerTimer;
                if (this.provokedByPlayerTimer <= 0) {
                    this.provokedByPlayer = false;
                }
            }
            if (this.provokedByMobTimer > 0) {
                --this.provokedByMobTimer;
                if (this.provokedByMobTimer <= 0) {
                    this.provokedByMob = false;
                }
            }
            if (this.voicePlaybackCooldown <= 0 && this.m_6084_()) {
                this.attemptVoicePlayback();
                this.voicePlaybackCooldown = 100 + this.f_19796_.m_188503_(141);
            } else {
                --this.voicePlaybackCooldown;
            }
            if (this.singingLoopActive) {
                ++this.singingLoopTimer;
                if (this.singingLoopTimer >= 120) {
                    this.playHostileSinging();
                    this.singingLoopTimer = 0;
                }
            }
            ++this.armorSyncTimer;
            if (this.armorSyncTimer >= 20) {
                this.syncArmor();
                this.armorSyncTimer = 0;
            }
            if (!this.isHostileModeActive()) {
                ++this.hostileMobAggroTimer;
                if (this.hostileMobAggroTimer >= 40) {
                    this.pullHostileMobsAggro();
                    this.hostileMobAggroTimer = 0;
                }
            } else {
                this.hostileMobAggroTimer = 0;
            }
            ++this.passiveItemCollectionTimer;
            if (this.passiveItemCollectionTimer >= 5) {
                this.collectItemsPassively();
                this.passiveItemCollectionTimer = 0;
            }
            this.f_19804_.m_135381_(BEHAVIOR_TIMER, (Object)this.timeSinceSpawn);
        }
    }

    private void syncArmor() {
        Player targetPlayer;
        if (this.allowInventoryCopy && this.targetPlayerUUID != null && (targetPlayer = this.m_9236_().m_46003_(this.targetPlayerUUID)) != null) {
            this.syncArmorSlotFromPlayer(targetPlayer, EquipmentSlot.HEAD);
            this.syncArmorSlotFromPlayer(targetPlayer, EquipmentSlot.CHEST);
            this.syncArmorSlotFromPlayer(targetPlayer, EquipmentSlot.LEGS);
            this.syncArmorSlotFromPlayer(targetPlayer, EquipmentSlot.FEET);
        }
    }

    private void syncArmorSlotFromPlayer(Player targetPlayer, EquipmentSlot slot) {
        ItemStack desired;
        ItemStack current = this.m_6844_(slot);
        if (!this.isSameArmorIgnoringDurability(current, desired = targetPlayer.m_6844_(slot))) {
            this.m_8061_(slot, desired.m_41777_());
        }
    }

    private boolean isSameArmorIgnoringDurability(ItemStack a, ItemStack b) {
        if (a.m_41619_() && b.m_41619_()) {
            return true;
        }
        if (a.m_41619_() != b.m_41619_()) {
            return false;
        }
        ItemStack aa = a.m_41777_();
        ItemStack bb = b.m_41777_();
        aa.m_41749_("Damage");
        bb.m_41749_("Damage");
        return ItemStack.m_150942_((ItemStack)aa, (ItemStack)bb);
    }

    private void pullHostileMobsAggro() {
        if (this.m_9236_().f_46443_) {
            return;
        }
        for (Mob mob : this.m_9236_().m_45976_(Mob.class, this.m_20191_().m_82400_(24.0))) {
            if (mob == this || !mob.m_6084_() || mob instanceof MimesisEntity || !(mob instanceof Monster)) continue;
            mob.m_6710_((LivingEntity)this);
        }
    }

    private void clearNearbyAggro() {
        if (this.m_9236_().f_46443_) {
            return;
        }
        for (Mob mob : this.m_9236_().m_45976_(Mob.class, this.m_20191_().m_82400_(24.0))) {
            if (mob.m_5448_() != this) continue;
            mob.m_6710_(null);
        }
    }

    public void m_6710_(@Nullable LivingEntity target) {
        if (target instanceof Mob && this.isHostileModeActive()) {
            return;
        }
        super.m_6710_(target);
    }

    private boolean shouldEnterHostileMode() {
        if (this.getTargetPlayerName().isEmpty()) {
            return false;
        }
        Player nearestPlayer = this.m_9236_().m_45924_(this.m_20185_(), this.m_20186_(), this.m_20189_(), 5.0, false);
        return nearestPlayer != null;
    }

    private void attemptVoicePlayback() {
        UUID playerUUID = this.getTargetPlayerUUID();
        if (this.allowVoicePlayback && playerUUID != null) {
            try {
                VoicechatServerApi api = MimesisVoiceChatPlugin.getVoicechatApi();
                if (api != null) {
                    VoicePlaybackService.playVoiceClip(this, playerUUID, api);
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public void m_6668_(DamageSource damageSource) {
        if (!this.m_9236_().f_46443_) {
            for (ItemStack s : this.carriedItems) {
                if (s == null || s.m_41619_()) continue;
                this.m_19983_(s.m_41777_());
            }
        }
        super.m_6668_(damageSource);
    }

    protected void m_21226_() {
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        if (this.targetPlayerUUID != null) {
            tag.m_128362_("TargetPlayerUUID", this.targetPlayerUUID);
            tag.m_128359_("TargetPlayerName", this.targetPlayerName);
            tag.m_128405_("TimeSinceSpawn", this.timeSinceSpawn);
            tag.m_128405_("HostileActivationTime", this.hostileActivationTime);
            tag.m_128379_("IsHostile", this.isHostile);
            tag.m_128379_("HostilityTransformPlayed", this.hostilityTransformPlayed);
        }
        if (this.appearancePlayerUUID != null) {
            tag.m_128362_("AppearancePlayerUUID", this.appearancePlayerUUID);
        }
        tag.m_128379_("AllowVoicePlayback", this.allowVoicePlayback);
        tag.m_128379_("AllowInventoryCopy", this.allowInventoryCopy);
        ListTag list = new ListTag();
        for (ItemStack s : this.carriedItems) {
            CompoundTag itemTag = new CompoundTag();
            if (s != null) {
                s.m_41739_(itemTag);
            }
            list.add((Object)itemTag);
        }
        tag.m_128365_("CarriedItems", (Tag)list);
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        if (tag.m_128403_("TargetPlayerUUID")) {
            this.targetPlayerUUID = tag.m_128342_("TargetPlayerUUID");
            this.targetPlayerName = tag.m_128461_("TargetPlayerName");
            this.timeSinceSpawn = tag.m_128451_("TimeSinceSpawn");
            this.hostileActivationTime = tag.m_128441_("HostileActivationTime") ? tag.m_128451_("HostileActivationTime") : this.hostileActivationTime;
            this.isHostile = tag.m_128441_("IsHostile") ? tag.m_128471_("IsHostile") : false;
            this.hostilityTransformPlayed = tag.m_128441_("HostilityTransformPlayed") ? tag.m_128471_("HostilityTransformPlayed") : false;
            this.allowVoicePlayback = tag.m_128441_("AllowVoicePlayback") ? tag.m_128471_("AllowVoicePlayback") : true;
            this.allowInventoryCopy = tag.m_128441_("AllowInventoryCopy") ? tag.m_128471_("AllowInventoryCopy") : true;
            this.f_19804_.m_135381_(TARGET_PLAYER_UUID, (Object)this.targetPlayerUUID.toString());
            this.f_19804_.m_135381_(TARGET_PLAYER_NAME, (Object)this.targetPlayerName);
            this.f_19804_.m_135381_(HOSTILE_MODE, (Object)this.isHostile);
            if (tag.m_128403_("AppearancePlayerUUID")) {
                this.appearancePlayerUUID = tag.m_128342_("AppearancePlayerUUID");
                this.f_19804_.m_135381_(APPEARANCE_PLAYER_UUID, (Object)this.appearancePlayerUUID.toString());
            } else {
                this.appearancePlayerUUID = null;
                this.f_19804_.m_135381_(APPEARANCE_PLAYER_UUID, (Object)"");
            }
        } else {
            this.allowVoicePlayback = tag.m_128441_("AllowVoicePlayback") ? tag.m_128471_("AllowVoicePlayback") : true;
            this.allowInventoryCopy = tag.m_128441_("AllowInventoryCopy") ? tag.m_128471_("AllowInventoryCopy") : true;
            this.f_19804_.m_135381_(HOSTILE_MODE, (Object)false);
            if (tag.m_128403_("AppearancePlayerUUID")) {
                this.appearancePlayerUUID = tag.m_128342_("AppearancePlayerUUID");
                this.f_19804_.m_135381_(APPEARANCE_PLAYER_UUID, (Object)this.appearancePlayerUUID.toString());
            } else {
                this.appearancePlayerUUID = null;
                this.f_19804_.m_135381_(APPEARANCE_PLAYER_UUID, (Object)"");
            }
        }
        if (tag.m_128441_("CarriedItems")) {
            ListTag list = tag.m_128437_("CarriedItems", 10);
            this.carriedItems = NonNullList.m_122780_((int)Math.max(9, list.size()), (Object)ItemStack.f_41583_);
            for (int i = 0; i < list.size() && i < this.carriedItems.size(); ++i) {
                this.carriedItems.set(i, (Object)ItemStack.m_41712_((CompoundTag)list.m_128728_(i)));
            }
        }
    }

    public boolean m_6469_(DamageSource source, float amount) {
        if (source.m_7639_() instanceof Player) {
            this.playCustomHurtSound = this.isHostileModeActive();
            this.provokedByPlayer = true;
            this.provokedByPlayerTimer = 200;
            this.m_6710_((LivingEntity)source.m_7639_());
            boolean tookDamage = super.m_6469_(source, amount);
            if (tookDamage && !this.m_9236_().f_46443_) {
                this.setHostileMode(true);
                this.teleportNearTargetLikeEnderman();
            }
            this.playCustomHurtSound = false;
            return tookDamage;
        }
        if (source.m_7639_() instanceof Mob) {
            this.playCustomHurtSound = this.isHostileModeActive();
            this.provokedByMob = true;
            this.provokedByMobTimer = 200;
            this.m_6710_((LivingEntity)source.m_7639_());
            this.m_6703_((LivingEntity)((Mob)source.m_7639_()));
            this.m_6677_(source);
            this.playCustomHurtSound = false;
            return false;
        }
        this.playCustomHurtSound = this.isHostileModeActive();
        boolean tookDamage = super.m_6469_(source, amount);
        if (tookDamage && !this.m_9236_().f_46443_) {
            this.teleportNearTargetLikeEnderman();
        }
        this.playCustomHurtSound = false;
        return tookDamage;
    }

    protected SoundEvent m_7975_(DamageSource damageSource) {
        if (this.playCustomHurtSound) {
            return (SoundEvent)MimesisSounds.MIMESIS_HURT.get();
        }
        return super.m_7975_(damageSource);
    }

    private void teleportNearTargetLikeEnderman() {
        LivingEntity target = this.m_5448_();
        if (target == null) {
            return;
        }
        for (int i = 0; i < 16; ++i) {
            double z;
            double y;
            double x = target.m_20185_() + (this.f_19796_.m_188500_() * 20.0 - 10.0);
            if (!this.m_20984_(x, y = target.m_20186_() + (this.f_19796_.m_188500_() * 20.0 - 10.0), z = target.m_20189_() + (this.f_19796_.m_188500_() * 20.0 - 10.0), true)) continue;
            this.m_21573_().m_26573_();
            this.m_6710_(target);
            return;
        }
    }

    public void teleportNearCurrentTargetLikeEnderman() {
        if (!this.m_9236_().f_46443_) {
            this.teleportNearTargetLikeEnderman();
        }
    }

    public boolean m_6785_(double distanceToFurthestPlayer) {
        return false;
    }

    private void playTransformationEffects() {
        if (this.m_9236_().f_46443_) {
            return;
        }
        for (Player player : this.m_9236_().m_45976_(Player.class, this.m_20191_().m_82400_(20.0))) {
            player.m_7292_(new MobEffectInstance(MobEffects.f_216964_, 60, 0, false, false, true));
        }
        SoundEvent scream = (SoundEvent)MimesisSounds.MIMESIS_SCREAM.get();
        this.m_9236_().m_5594_(null, this.m_20183_(), scream, SoundSource.HOSTILE, 1.0f, 1.0f);
        Level level = this.m_9236_();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            serverLevel.m_8767_((ParticleOptions)new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.6f), this.m_20185_(), this.m_20186_() + 1.0, this.m_20189_(), 40, 0.5, 1.0, 0.5, 0.08);
        }
    }

    private void playHostileSinging() {
        if (this.m_9236_().f_46443_) {
            return;
        }
        SoundEvent singing = (SoundEvent)MimesisSounds.MIMESIS_SINGING.get();
        this.m_9236_().m_5594_(null, this.m_20183_(), singing, SoundSource.HOSTILE, 1.0f, 1.0f);
    }

    public void m_6667_(DamageSource damageSource) {
        this.singingLoopActive = false;
        this.singingLoopTimer = 0;
        super.m_6667_(damageSource);
    }
}

