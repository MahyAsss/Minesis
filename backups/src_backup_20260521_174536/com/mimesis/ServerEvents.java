/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.monster.Monster
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LightLayer
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.event.RegisterCommandsEvent
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.event.TickEvent$ServerTickEvent
 *  net.minecraftforge.event.level.BlockEvent$BreakEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.mimesis;

import com.mimesis.commands.MimesisCommand;
import com.mimesis.entity.MimesisEntities;
import com.mimesis.entity.MimesisEntity;
import com.mimesis.util.MojangAPI;
import com.mimesis.voice.VoiceStorage;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid="mimesis", bus=Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();
    private static final int SPAWN_CHANCE = 5;
    private static final Map<UUID, Long> lastSpawnTimes = new ConcurrentHashMap<UUID, Long>();
    private static final long SPAWN_COOLDOWN_MS = 600000L;
    private static long lastNaturalSpawnTimeMs = 0L;
    private static final int NATURAL_SPAWN_DISTANCE = 30;
    private static final int NATURAL_APPEARANCE_MIN_CLIPS = 50;
    private static final double NATURAL_APPEARANCE_MIN_DISTANCE_SQR = 4096.0;
    private static final int MAX_VERTICAL_SPAWN_DIFF = 5;

    private static boolean hasAnyMimesisOnServer(ServerLevel level) {
        if (level.m_7654_() == null) {
            return !level.m_45976_(MimesisEntity.class, level.m_6857_().m_61946_().m_83215_()).isEmpty();
        }
        for (ServerLevel serverLevel : level.m_7654_().m_129785_()) {
            if (serverLevel.m_45976_(MimesisEntity.class, serverLevel.m_6857_().m_61946_().m_83215_()).isEmpty()) continue;
            return true;
        }
        return false;
    }

    private static Vec3 findSpawnOutsideFov(Player targetPlayer, double minDistance, double maxDistance) {
        double fz;
        double fx;
        ServerLevel level = (ServerLevel)targetPlayer.m_9236_();
        Vec3 look = targetPlayer.m_20154_().m_82541_();
        Vec3 look2D = new Vec3(look.f_82479_, 0.0, look.f_82481_).m_82541_();
        boolean requireUnderground = !level.m_45527_(targetPlayer.m_20183_());
        Vec3 fallback = null;
        for (int attempt = 0; attempt < 20; ++attempt) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double distance = minDistance + RANDOM.nextDouble() * (maxDistance - minDistance);
            double x = targetPlayer.m_20185_() + Math.cos(angle) * distance;
            double z = targetPlayer.m_20189_() + Math.sin(angle) * distance;
            Vec3 toSpawn = new Vec3(x - targetPlayer.m_20185_(), 0.0, z - targetPlayer.m_20189_());
            if (toSpawn.m_82556_() < 1.0E-4) continue;
            Vec3 toSpawnNorm = toSpawn.m_82541_();
            double dot = look2D.m_82526_(toSpawnNorm);
            Integer y = ServerEvents.findSpawnYNearPlayer(level, targetPlayer, x, z, requireUnderground);
            if (y == null) continue;
            Vec3 candidate = new Vec3(x, (double)y.intValue(), z);
            if (dot < 0.15) {
                return candidate;
            }
            if (fallback != null && !(dot < 0.45)) continue;
            fallback = candidate;
        }
        if (fallback != null) {
            return fallback;
        }
        Vec3 behind = look2D.m_82490_(-(minDistance + maxDistance) * 0.5);
        Integer fy = ServerEvents.findSpawnYNearPlayer(level, targetPlayer, fx = targetPlayer.m_20185_() + behind.f_82479_, fz = targetPlayer.m_20189_() + behind.f_82481_, requireUnderground);
        return new Vec3(fx, fy == null ? targetPlayer.m_20186_() : (double)fy.intValue(), fz);
    }

    private static boolean isSpawnSpaceValid(ServerLevel level, BlockPos pos) {
        BlockState feet = level.m_8055_(pos);
        BlockState head = level.m_8055_(pos.m_7494_());
        BlockState ground = level.m_8055_(pos.m_7495_());
        return feet.m_60795_() && head.m_60795_() && !ground.m_60795_();
    }

    private static Integer findSpawnYNearPlayer(ServerLevel level, Player targetPlayer, double x, double z, boolean requireUnderground) {
        int baseY = targetPlayer.m_20183_().m_123342_();
        int minY = Math.max(level.m_141937_() + 1, baseY - 5);
        int maxY = Math.min(level.m_151558_() - 2, baseY + 5);
        int span = Math.max(1, maxY - minY + 1);
        int start = minY + RANDOM.nextInt(span);
        for (int i = 0; i < span; ++i) {
            int y = minY + (start - minY + i) % span;
            BlockPos pos = BlockPos.m_274561_((double)x, (double)y, (double)z);
            if (!ServerEvents.isSpawnSpaceValid(level, pos) || requireUnderground && (level.m_45527_(pos) || level.m_45517_(LightLayer.SKY, pos) > 0)) continue;
            return y;
        }
        return null;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MimesisCommand.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
    }

    public static void spawnMimesisNearPlayer(Player targetPlayer) {
        if (targetPlayer.m_9236_().f_46443_) {
            return;
        }
        Level level = targetPlayer.m_9236_();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        if (ServerEvents.hasAnyMimesisOnServer(serverLevel)) {
            return;
        }
        Vec3 spawnPos = ServerEvents.findSpawnOutsideFov(targetPlayer, 25.0, 35.0);
        MimesisEntity mimesis = new MimesisEntity((EntityType<? extends Monster>)((EntityType)MimesisEntities.MIMESIS_ENTITY.get()), targetPlayer.m_9236_());
        mimesis.m_6034_(spawnPos.f_82479_, spawnPos.f_82480_, spawnPos.f_82481_);
        mimesis.setTargetPlayer(targetPlayer);
        targetPlayer.m_9236_().m_7967_((Entity)mimesis);
    }

    public static void spawnAnonymousMimesisNearPlayer(Player targetPlayer, String name) {
        if (targetPlayer.m_9236_().f_46443_) {
            return;
        }
        Level level = targetPlayer.m_9236_();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        if (ServerEvents.hasAnyMimesisOnServer(serverLevel)) {
            return;
        }
        Vec3 spawnPos = ServerEvents.findSpawnOutsideFov(targetPlayer, 25.0, 35.0);
        MimesisEntity mimesis = new MimesisEntity((EntityType<? extends Monster>)((EntityType)MimesisEntities.MIMESIS_ENTITY.get()), targetPlayer.m_9236_());
        mimesis.m_6034_(spawnPos.f_82479_, spawnPos.f_82480_, spawnPos.f_82481_);
        mimesis.setAnonymousTarget(name);
        MinecraftServer server = targetPlayer.m_20194_();
        boolean copied = false;
        if (server != null) {
            for (ServerPlayer p : server.m_6846_().m_11314_()) {
                if (!p.m_7755_().getString().equalsIgnoreCase(name)) continue;
                mimesis.setAppearanceFromPlayer((Player)p);
                copied = true;
                break;
            }
        }
        if (!copied) {
            for (ServerPlayer p : targetPlayer.m_9236_().m_6907_()) {
                if (!p.m_7755_().getString().equalsIgnoreCase(name)) continue;
                mimesis.setAppearanceFromPlayer((Player)p);
                copied = true;
                break;
            }
        }
        targetPlayer.m_9236_().m_7967_((Entity)mimesis);
    }

    public static void spawnNamedMimesisNearPlayer(Player targetPlayer, String name) {
        if (targetPlayer.m_9236_().f_46443_) {
            return;
        }
        Level level = targetPlayer.m_9236_();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        if (ServerEvents.hasAnyMimesisOnServer(serverLevel)) {
            return;
        }
        Vec3 spawnPos = ServerEvents.findSpawnOutsideFov(targetPlayer, 25.0, 35.0);
        MimesisEntity mimesis = new MimesisEntity((EntityType<? extends Monster>)((EntityType)MimesisEntities.MIMESIS_ENTITY.get()), targetPlayer.m_9236_());
        mimesis.m_6034_(spawnPos.f_82479_, spawnPos.f_82480_, spawnPos.f_82481_);
        mimesis.setAnonymousTarget(name);
        MinecraftServer server = targetPlayer.m_20194_();
        boolean copied = false;
        if (server != null) {
            for (ServerPlayer p : server.m_6846_().m_11314_()) {
                if (!p.m_7755_().getString().equalsIgnoreCase(name)) continue;
                mimesis.setAppearanceFromPlayer((Player)p);
                copied = true;
                break;
            }
        }
        if (!copied) {
            for (ServerPlayer p : targetPlayer.m_9236_().m_6907_()) {
                if (!p.m_7755_().getString().equalsIgnoreCase(name)) continue;
                mimesis.setAppearanceFromPlayer((Player)p);
                copied = true;
                break;
            }
        }
        if (!copied) {
            try {
                UUID uuid;
                String textureProperties = MojangAPI.getTexturePropertiesByName(name);
                if (textureProperties != null && !textureProperties.isEmpty() && (uuid = MojangAPI.getUUIDByName(name)) != null) {
                    mimesis.setAppearanceFromNameAndProperties(name, uuid, textureProperties);
                }
            }
            catch (Exception e) {
                LOGGER.debug("Failed to fetch appearance from Mojang API for " + name + ": " + e.getMessage());
            }
        }
        targetPlayer.m_9236_().m_7967_((Entity)mimesis);
    }

    private static Vec3 findNaturalCaveSpawnOutsideFov(Player targetPlayer, double minDistance, double maxDistance) {
        Vec3 look = targetPlayer.m_20154_().m_82541_();
        Vec3 look2D = new Vec3(look.f_82479_, 0.0, look.f_82481_).m_82541_();
        ServerLevel level = (ServerLevel)targetPlayer.m_9236_();
        Vec3 fallback = null;
        for (int attempt = 0; attempt < 28; ++attempt) {
            double z;
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double distance = minDistance + RANDOM.nextDouble() * (maxDistance - minDistance);
            double x = targetPlayer.m_20185_() + Math.cos(angle) * distance;
            Integer yObj = ServerEvents.findSpawnYNearPlayer(level, targetPlayer, x, z = targetPlayer.m_20189_() + Math.sin(angle) * distance, true);
            if (yObj == null) continue;
            int y = yObj;
            BlockPos pos = BlockPos.m_274561_((double)x, (double)y, (double)z);
            Vec3 toSpawn = new Vec3(x - targetPlayer.m_20185_(), 0.0, z - targetPlayer.m_20189_());
            if (toSpawn.m_82556_() < 1.0E-4) continue;
            Vec3 toSpawnNorm = toSpawn.m_82541_();
            double dot = look2D.m_82526_(toSpawnNorm);
            Vec3 candidate = new Vec3(x, (double)pos.m_123342_(), z);
            if (dot < 0.15) {
                return candidate;
            }
            if (fallback != null && !(dot < 0.45)) continue;
            fallback = candidate;
        }
        return fallback;
    }

    private static Player findDistantAppearanceSource(Player targetPlayer) {
        ServerLevel level = (ServerLevel)targetPlayer.m_9236_();
        MinecraftServer server = targetPlayer.m_20194_();
        if (server == null || server.m_6846_().m_11314_().size() < 2) {
            return null;
        }
        ServerPlayer best = null;
        for (ServerPlayer candidate : server.m_6846_().m_11314_()) {
            if (candidate == targetPlayer || candidate.m_9236_() == level && candidate.m_20280_((Entity)targetPlayer) < 4096.0 || VoiceStorage.getClipCount(candidate.m_20148_()) < 50) continue;
            best = candidate;
            break;
        }
        return best;
    }

    public static void spawnNaturalMimesisNearPlayer(Player targetPlayer) {
        if (targetPlayer.m_9236_().f_46443_) {
            return;
        }
        UUID puuid = targetPlayer.m_20148_();
        int clipCount = VoiceStorage.getClipCount(puuid);
        if (clipCount < 50) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastNaturalSpawnTimeMs < 600000L) {
            return;
        }
        Long last = lastSpawnTimes.get(puuid);
        if (last != null && now - last < 600000L) {
            return;
        }
        Level level = targetPlayer.m_9236_();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        if (ServerEvents.hasAnyMimesisOnServer(serverLevel)) {
            return;
        }
        MinecraftServer server = targetPlayer.m_20194_();
        if (server == null || server.m_6846_().m_11314_().size() < 2) {
            return;
        }
        if (!serverLevel.m_46462_()) {
            return;
        }
        if (serverLevel.m_45527_(targetPlayer.m_20183_())) {
            return;
        }
        Vec3 spawnPos = ServerEvents.findNaturalCaveSpawnOutsideFov(targetPlayer, 12.0, 24.0);
        if (spawnPos == null) {
            return;
        }
        Player appearanceSource = ServerEvents.findDistantAppearanceSource(targetPlayer);
        if (appearanceSource == null) {
            return;
        }
        MimesisEntity m = new MimesisEntity((EntityType<? extends Monster>)((EntityType)MimesisEntities.MIMESIS_ENTITY.get()), targetPlayer.m_9236_());
        m.m_6034_(spawnPos.f_82479_, spawnPos.f_82480_, spawnPos.f_82481_);
        m.setNaturalTarget(targetPlayer, appearanceSource);
        if (appearanceSource != null) {
            m.copyPlayerArmor(appearanceSource);
        }
        targetPlayer.m_9236_().m_7967_((Entity)m);
        lastSpawnTimes.put(puuid, now);
        lastNaturalSpawnTimeMs = now;
    }

    public static long getRemainingSpawnCooldownMs(Player player) {
        long diff;
        if (player == null) {
            return 0L;
        }
        UUID puuid = player.m_20148_();
        int clipCount = VoiceStorage.getClipCount(puuid);
        if (clipCount < 50) {
            return Long.MAX_VALUE;
        }
        Long last = lastSpawnTimes.get(puuid);
        long now = System.currentTimeMillis();
        long perPlayerRemaining = 0L;
        if (last != null && (diff = now - last) < 600000L) {
            perPlayerRemaining = 600000L - diff;
        }
        long globalRemaining = 0L;
        long globalDiff = now - lastNaturalSpawnTimeMs;
        if (globalDiff < 600000L) {
            globalRemaining = 600000L - globalDiff;
        }
        return Math.max(perPlayerRemaining, globalRemaining);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (ServerLevel level : event.getServer().m_129785_()) {
            for (Player player : level.m_6907_()) {
                if (player.m_9236_().f_46443_ || RANDOM.nextInt(100) >= 5) continue;
                ServerEvents.spawnNaturalMimesisNearPlayer(player);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().m_5776_()) {
            return;
        }
        BlockPos pos = event.getPos();
        for (MimesisEntity mimesis : event.getLevel().m_45976_(MimesisEntity.class, new AABB(pos).m_82400_(16.0))) {
            BlockPos cookingStation = mimesis.getCookingStationPos();
            if (cookingStation == null || !cookingStation.equals((Object)pos)) continue;
            event.setCanceled(true);
            return;
        }
    }
}

