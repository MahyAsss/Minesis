package com.mimesis;

import java.util.Random;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.mimesis.voice.VoiceStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;

import com.mimesis.entity.MimesisEntity;
import com.mimesis.entity.MimesisEntities;
import com.mimesis.commands.MimesisCommand;
import com.mimesis.util.MojangAPI;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

@Mod.EventBusSubscriber(modid = "mimesis", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();
    private static final int SPAWN_CHANCE = 5; // 5% chance per player tick
    private static final Map<UUID, Long> lastSpawnTimes = new ConcurrentHashMap<>();
    private static final long SPAWN_COOLDOWN_MS = 10L * 60L * 1000L; // 10 minutes
    private static long lastNaturalSpawnTimeMs = 0L;
    private static final int NATURAL_SPAWN_DISTANCE = 30;
    private static final int NATURAL_APPEARANCE_MIN_CLIPS = 50;
    private static final double NATURAL_APPEARANCE_MIN_DISTANCE_SQR = 64.0D * 64.0D;
    private static final int MAX_VERTICAL_SPAWN_DIFF = 5;

    private static boolean hasAnyMimesisOnServer(ServerLevel level) {
        if (level.getServer() == null) {
            return !level.getEntitiesOfClass(MimesisEntity.class, level.getWorldBorder().getCollisionShape().bounds()).isEmpty();
        }

        for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
            if (!serverLevel.getEntitiesOfClass(
                    MimesisEntity.class,
                    serverLevel.getWorldBorder().getCollisionShape().bounds()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static Vec3 findSpawnOutsideFov(Player targetPlayer, double minDistance, double maxDistance) {
        ServerLevel level = (ServerLevel) targetPlayer.level();
        Vec3 look = targetPlayer.getLookAngle().normalize();
        Vec3 look2D = new Vec3(look.x, 0.0D, look.z).normalize();
        boolean requireUnderground = !level.canSeeSky(targetPlayer.blockPosition());

        Vec3 fallback = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
            double distance = minDistance + RANDOM.nextDouble() * (maxDistance - minDistance);
            double x = targetPlayer.getX() + Math.cos(angle) * distance;
            double z = targetPlayer.getZ() + Math.sin(angle) * distance;

            Vec3 toSpawn = new Vec3(x - targetPlayer.getX(), 0.0D, z - targetPlayer.getZ());
            if (toSpawn.lengthSqr() < 0.0001D) {
                continue;
            }

            Vec3 toSpawnNorm = toSpawn.normalize();
            double dot = look2D.dot(toSpawnNorm);

            Integer y = findSpawnYNearPlayer(level, targetPlayer, x, z, requireUnderground);
            if (y == null) {
                continue;
            }
            Vec3 candidate = new Vec3(x, y, z);

            // Out of vision cone: reject if too far in front (dot near 1)
            if (dot < 0.15D) {
                return candidate;
            }

            if (fallback == null || dot < 0.45D) {
                fallback = candidate;
            }
        }

        if (fallback != null) {
            return fallback;
        }

        Vec3 behind = look2D.scale(-(minDistance + maxDistance) * 0.5D);
        double fx = targetPlayer.getX() + behind.x;
        double fz = targetPlayer.getZ() + behind.z;
        Integer fy = findSpawnYNearPlayer(level, targetPlayer, fx, fz, requireUnderground);
        return new Vec3(fx, fy == null ? targetPlayer.getY() : fy, fz);
    }

    private static boolean isSpawnSpaceValid(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState ground = level.getBlockState(pos.below());
        return feet.isAir() && head.isAir() && !ground.isAir();
    }

    private static Integer findSpawnYNearPlayer(ServerLevel level, Player targetPlayer, double x, double z, boolean requireUnderground) {
        int baseY = targetPlayer.blockPosition().getY();
        int minY = Math.max(level.getMinBuildHeight() + 1, baseY - MAX_VERTICAL_SPAWN_DIFF);
        int maxY = Math.min(level.getMaxBuildHeight() - 2, baseY + MAX_VERTICAL_SPAWN_DIFF);
        int span = Math.max(1, maxY - minY + 1);
        int start = minY + RANDOM.nextInt(span);

        for (int i = 0; i < span; i++) {
            int y = minY + ((start - minY + i) % span);
            BlockPos pos = BlockPos.containing(x, y, z);
            if (!isSpawnSpaceValid(level, pos)) {
                continue;
            }
            if (requireUnderground) {
                if (level.canSeeSky(pos) || level.getBrightness(LightLayer.SKY, pos) > 0) {
                    continue;
                }
            }
            return y;
        }

        return null;
    }

    /**
     * Spawn a Mimesis entity near a player
     * Can be triggered by chat commands or random events
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MimesisCommand.register(event.getDispatcher());
    }

    public static void spawnMimesisNearPlayer(Player targetPlayer) {
        if (targetPlayer.level().isClientSide) {
            return;
        }
        if (!(targetPlayer.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (hasAnyMimesisOnServer(serverLevel)) {
            return;
        }

        // Spawn away from player to avoid instant hostile activation
        Vec3 spawnPos = findSpawnOutsideFov(targetPlayer, 25.0D, 35.0D);
        
        // Create the entity away from the player (no restrictions for commands)
        MimesisEntity mimesis = new MimesisEntity(MimesisEntities.MIMESIS_ENTITY.get(), targetPlayer.level());
        mimesis.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        mimesis.setTargetPlayer(targetPlayer);
        targetPlayer.level().addFreshEntity(mimesis);
    }

    /**
     * Natural spawn: spawns a Mimesis with anonymous target.
     * Only copies appearance from players CURRENTLY ONLINE.
     */
    public static void spawnAnonymousMimesisNearPlayer(Player targetPlayer, String name) {
        if (targetPlayer.level().isClientSide) {
            return;
        }
        if (!(targetPlayer.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (hasAnyMimesisOnServer(serverLevel)) {
            return;
        }

        // Spawn away from player to avoid instant hostile activation
        Vec3 spawnPos = findSpawnOutsideFov(targetPlayer, 25.0D, 35.0D);
        
        // Natural spawn: only online players
        MimesisEntity mimesis = new MimesisEntity(MimesisEntities.MIMESIS_ENTITY.get(), targetPlayer.level());
        mimesis.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        mimesis.setAnonymousTarget(name);

        // Search for the named player on this server
        MinecraftServer server = targetPlayer.getServer();
        boolean copied = false;
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.getName().getString().equalsIgnoreCase(name)) {
                    mimesis.setAppearanceFromPlayer(p);
                    copied = true;
                    break;
                }
            }
        }

        // Fallback: check players in same level
        if (!copied) {
            for (Player p : targetPlayer.level().players()) {
                if (p.getName().getString().equalsIgnoreCase(name)) {
                    mimesis.setAppearanceFromPlayer(p);
                    copied = true;
                    break;
                }
            }
        }

        // If not found online, Mimesis will just have default appearance
        // We do NOT use Mojang API for natural spawns

        targetPlayer.level().addFreshEntity(mimesis);
    }

    /**
     * Manual spawn via command: spawns a Mimesis with named appearance.
     * Can use Mojang API to fetch skins of players who are never connected.
     */
    public static void spawnNamedMimesisNearPlayer(Player targetPlayer, String name) {
        if (targetPlayer.level().isClientSide) {
            return;
        }
        if (!(targetPlayer.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (hasAnyMimesisOnServer(serverLevel)) {
            return;
        }

        // Spawn away from player to avoid instant hostile activation
        Vec3 spawnPos = findSpawnOutsideFov(targetPlayer, 25.0D, 35.0D);
        
        // Manual command spawn: can use offline player skins
        MimesisEntity mimesis = new MimesisEntity(MimesisEntities.MIMESIS_ENTITY.get(), targetPlayer.level());
        mimesis.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        mimesis.setAnonymousTarget(name);

        // Priority 1: Search for the named player on this server (online)
        MinecraftServer server = targetPlayer.getServer();
        boolean copied = false;
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.getName().getString().equalsIgnoreCase(name)) {
                    mimesis.setAppearanceFromPlayer(p);
                    copied = true;
                    break;
                }
            }
        }

        // Priority 2: Fallback to checking players in same level
        if (!copied) {
            for (Player p : targetPlayer.level().players()) {
                if (p.getName().getString().equalsIgnoreCase(name)) {
                    mimesis.setAppearanceFromPlayer(p);
                    copied = true;
                    break;
                }
            }
        }

        // Priority 3: Try Mojang API (for offline players)
        if (!copied) {
            try {
                String textureProperties = MojangAPI.getTexturePropertiesByName(name);
                if (textureProperties != null && !textureProperties.isEmpty()) {
                    java.util.UUID uuid = MojangAPI.getUUIDByName(name);
                    if (uuid != null) {
                        mimesis.setAppearanceFromNameAndProperties(name, uuid, textureProperties);
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to fetch appearance from Mojang API for " + name + ": " + e.getMessage());
            }
        }

        targetPlayer.level().addFreshEntity(mimesis);
    }

    /**
     * Natural spawn: place Mimesis ~50 blocks behind the player
     */
    private static Vec3 findNaturalCaveSpawnOutsideFov(Player targetPlayer, double minDistance, double maxDistance) {
        Vec3 look = targetPlayer.getLookAngle().normalize();
        Vec3 look2D = new Vec3(look.x, 0.0D, look.z).normalize();
        ServerLevel level = (ServerLevel) targetPlayer.level();

        Vec3 fallback = null;
        for (int attempt = 0; attempt < 28; attempt++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
            double distance = minDistance + RANDOM.nextDouble() * (maxDistance - minDistance);
            double x = targetPlayer.getX() + Math.cos(angle) * distance;
            double z = targetPlayer.getZ() + Math.sin(angle) * distance;
            Integer yObj = findSpawnYNearPlayer(level, targetPlayer, x, z, true);
            if (yObj == null) {
                continue;
            }
            int y = yObj;
            BlockPos pos = BlockPos.containing(x, y, z);

            Vec3 toSpawn = new Vec3(x - targetPlayer.getX(), 0.0D, z - targetPlayer.getZ());
            if (toSpawn.lengthSqr() < 0.0001D) {
                continue;
            }

            Vec3 toSpawnNorm = toSpawn.normalize();
            double dot = look2D.dot(toSpawnNorm);
            Vec3 candidate = new Vec3(x, pos.getY(), z);

            if (dot < 0.15D) {
                return candidate;
            }

            if (fallback == null || dot < 0.45D) {
                fallback = candidate;
            }
        }

        return fallback;
    }

    private static Player findDistantAppearanceSource(Player targetPlayer) {
        ServerLevel level = (ServerLevel) targetPlayer.level();
        MinecraftServer server = targetPlayer.getServer();
        if (server == null || server.getPlayerList().getPlayers().size() < 2) {
            return null;
        }

        Player best = null;
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            if (candidate == targetPlayer) {
                continue;
            }
            if (candidate.level() == level && candidate.distanceToSqr(targetPlayer) < NATURAL_APPEARANCE_MIN_DISTANCE_SQR) {
                continue;
            }
            if (VoiceStorage.getClipCount(candidate.getUUID()) < NATURAL_APPEARANCE_MIN_CLIPS) {
                continue;
            }
            best = candidate;
            break;
        }
        return best;
    }

    public static void spawnNaturalMimesisNearPlayer(Player targetPlayer) {
        if (targetPlayer.level().isClientSide) return;

        UUID puuid = targetPlayer.getUUID();
        int clipCount = VoiceStorage.getClipCount(puuid);
        if (clipCount < NATURAL_APPEARANCE_MIN_CLIPS) return;

        long now = System.currentTimeMillis();
        if (now - lastNaturalSpawnTimeMs < SPAWN_COOLDOWN_MS) return;
        Long last = lastSpawnTimes.get(puuid);
        if (last != null && now - last.longValue() < SPAWN_COOLDOWN_MS) return;

        if (!(targetPlayer.level() instanceof ServerLevel serverLevel)) return;

        if (hasAnyMimesisOnServer(serverLevel)) return;

        MinecraftServer server = targetPlayer.getServer();
        if (server == null || server.getPlayerList().getPlayers().size() < 2) return;

        if (!serverLevel.isNight()) return;
        if (serverLevel.canSeeSky(targetPlayer.blockPosition())) return;

        Vec3 spawnPos = findNaturalCaveSpawnOutsideFov(targetPlayer, 12.0D, 24.0D);
        if (spawnPos == null) return;

        Player appearanceSource = findDistantAppearanceSource(targetPlayer);
        if (appearanceSource == null) return;

        MimesisEntity m = new MimesisEntity(MimesisEntities.MIMESIS_ENTITY.get(), targetPlayer.level());
        m.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        m.setNaturalTarget(targetPlayer, appearanceSource);
        if (appearanceSource != null) {
            m.copyPlayerArmor(appearanceSource);
        }
        targetPlayer.level().addFreshEntity(m);
        lastSpawnTimes.put(puuid, now);
        lastNaturalSpawnTimeMs = now;
    }

    /**
     * Return remaining cooldown in ms for spawning a mimesis for this player, or 0 if allowed.
     */
    public static long getRemainingSpawnCooldownMs(Player player) {
        if (player == null) return 0L;
        UUID puuid = player.getUUID();
        int clipCount = VoiceStorage.getClipCount(puuid);
        if (clipCount < NATURAL_APPEARANCE_MIN_CLIPS) return Long.MAX_VALUE; // indicate insufficient clips
        Long last = lastSpawnTimes.get(puuid);
        long now = System.currentTimeMillis();
        long perPlayerRemaining = 0L;
        if (last != null) {
            long diff = now - last.longValue();
            if (diff < SPAWN_COOLDOWN_MS) {
                perPlayerRemaining = SPAWN_COOLDOWN_MS - diff;
            }
        }

        long globalRemaining = 0L;
        long globalDiff = now - lastNaturalSpawnTimeMs;
        if (globalDiff < SPAWN_COOLDOWN_MS) {
            globalRemaining = SPAWN_COOLDOWN_MS - globalDiff;
        }

        return Math.max(perPlayerRemaining, globalRemaining);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Player player : level.players()) {
                if (player.level().isClientSide) {
                    continue;
                }
                if (RANDOM.nextInt(100) >= SPAWN_CHANCE) {
                    continue;
                }
                spawnNaturalMimesisNearPlayer(player);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        for (MimesisEntity mimesis : event.getLevel().getEntitiesOfClass(MimesisEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(16.0D))) {
            BlockPos cookingStation = mimesis.getCookingStationPos();
            if (cookingStation != null && cookingStation.equals(pos)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /**
     * Optional: Randomly spawn Mimesis entities near players
     * Uncomment to enable random spawning
     * Note: PlayerTickEvent has been removed in 1.20.1
     * Use other tick events instead
     */
    // @SubscribeEvent
    // public static void onServerTick(TickEvent.ServerTickEvent event) {
    //     if (event.phase != TickEvent.Phase.START) {
    //         return;
    //     }
    //     // Spawn logic here
    // }
}
