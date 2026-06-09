package com.minesis;

import java.util.Comparator;
import java.util.Random;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.minesis.voice.MinesisVoiceChatPlugin;
import com.minesis.voice.PlayerActivityTracker;
import com.minesis.voice.VoiceContext;
import com.minesis.voice.VoicePlaybackService;
import com.minesis.voice.VoiceStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import com.minesis.voice.VoicePersistenceManager;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;

import com.minesis.entity.MinesisEntity;
import com.minesis.entity.MinesisEntities;
import com.minesis.utils.MinesisConfig;
import com.minesis.commands.MinesisCommand;
import com.minesis.util.MojangAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

@Mod.EventBusSubscriber(modid = "minesis", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();
    private static final int SPAWN_ATTEMPT_CHANCE = 40; // 40% per attempt
    private static final long SPAWN_INTERVAL_MIN_MS = 15L * 60L * 1000L; // 15 minutes
    private static final long SPAWN_INTERVAL_MAX_MS = 60L * 60L * 1000L; // 60 minutes
    private static final Map<UUID, Long> nextSpawnAttemptTimes = new ConcurrentHashMap<>();
    private static final double NATURAL_APPEARANCE_MIN_DISTANCE_SQR = 64.0D * 64.0D;
    private static final int MAX_VERTICAL_SPAWN_DIFF = 5;

    // Speech-response trigger (SVC-based, no Vosk required)
    private static final Map<UUID, Long> lastSpeechResponseTime = new ConcurrentHashMap<>();
    private static final long SPEECH_RESPONSE_COOLDOWN_MS = 8_000L; // 8s between responses
    private static final long SPEECH_END_MIN_MS  = 500L;  // silence must last at least 500ms
    private static final long SPEECH_END_MAX_MS  = 3_000L; // but player spoke within 3s

    private static boolean hasAnyMinesisOnServer(ServerLevel level) {
        if (level.getServer() == null) {
            return !level.getEntitiesOfClass(MinesisEntity.class, level.getWorldBorder().getCollisionShape().bounds()).isEmpty();
        }

        for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
            if (!serverLevel.getEntitiesOfClass(
                    MinesisEntity.class,
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
     * Spawn a Minesis entity near a player
     * Can be triggered by chat commands or random events
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        VoicePersistenceManager.init(event.getServer());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MinesisCommand.register(event.getDispatcher());
    }

    public static void spawnMinesisNearPlayer(Player targetPlayer) {
        if (targetPlayer.level().isClientSide) {
            return;
        }
        if (!(targetPlayer.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (hasAnyMinesisOnServer(serverLevel)) {
            return;
        }

        // Spawn away from player to avoid instant hostile activation
        Vec3 spawnPos = findSpawnOutsideFov(targetPlayer, 25.0D, 35.0D);
        
        // Create the entity away from the player (no restrictions for commands)
        MinesisEntity minesis = new MinesisEntity(MinesisEntities.MIMESIS_ENTITY.get(), targetPlayer.level());
        minesis.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        minesis.setTargetPlayer(targetPlayer);
        targetPlayer.level().addFreshEntity(minesis);
    }

    /**
     * Natural spawn: spawns a Minesis with anonymous target.
     * Only copies appearance from players CURRENTLY ONLINE.
     */
    public static void spawnAnonymousMinesisNearPlayer(Player targetPlayer, String name) {
        if (targetPlayer.level().isClientSide) {
            return;
        }
        if (!(targetPlayer.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (hasAnyMinesisOnServer(serverLevel)) {
            return;
        }

        // Spawn away from player to avoid instant hostile activation
        Vec3 spawnPos = findSpawnOutsideFov(targetPlayer, 25.0D, 35.0D);
        
        // Natural spawn: only online players
        MinesisEntity minesis = new MinesisEntity(MinesisEntities.MIMESIS_ENTITY.get(), targetPlayer.level());
        minesis.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        minesis.setAnonymousTarget(name);

        // Search for the named player on this server
        MinecraftServer server = targetPlayer.getServer();
        boolean copied = false;
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.getName().getString().equalsIgnoreCase(name)) {
                    minesis.setAppearanceFromPlayer(p);
                    copied = true;
                    break;
                }
            }
        }

        // Fallback: check players in same level
        if (!copied) {
            for (Player p : targetPlayer.level().players()) {
                if (p.getName().getString().equalsIgnoreCase(name)) {
                    minesis.setAppearanceFromPlayer(p);
                    copied = true;
                    break;
                }
            }
        }

        // If not found online, Minesis will just have default appearance
        // We do NOT use Mojang API for natural spawns

        targetPlayer.level().addFreshEntity(minesis);
    }

    /**
     * Manual spawn via command: spawns a Minesis with named appearance.
     * Can use Mojang API to fetch skins of players who are never connected.
     */
    public static void spawnNamedMinesisNearPlayer(Player targetPlayer, String name) {
        if (targetPlayer.level().isClientSide) {
            return;
        }
        if (!(targetPlayer.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (hasAnyMinesisOnServer(serverLevel)) {
            return;
        }

        // Spawn away from player to avoid instant hostile activation
        Vec3 spawnPos = findSpawnOutsideFov(targetPlayer, 25.0D, 35.0D);
        
        // Manual command spawn: can use offline player skins
        MinesisEntity minesis = new MinesisEntity(MinesisEntities.MIMESIS_ENTITY.get(), targetPlayer.level());
        minesis.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        minesis.setAnonymousTarget(name);

        // Priority 1: Search for the named player on this server (online)
        MinecraftServer server = targetPlayer.getServer();
        boolean copied = false;
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.getName().getString().equalsIgnoreCase(name)) {
                    minesis.setAppearanceFromPlayer(p);
                    copied = true;
                    break;
                }
            }
        }

        // Priority 2: Fallback to checking players in same level
        if (!copied) {
            for (Player p : targetPlayer.level().players()) {
                if (p.getName().getString().equalsIgnoreCase(name)) {
                    minesis.setAppearanceFromPlayer(p);
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
                        minesis.setAppearanceFromNameAndProperties(name, uuid, textureProperties);
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to fetch appearance from Mojang API for " + name + ": " + e.getMessage());
            }
        }

        targetPlayer.level().addFreshEntity(minesis);
    }

    /**
     * Natural spawn: place Minesis ~50 blocks behind the player
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
            Integer yObj = findSpawnYNearPlayer(level, targetPlayer, x, z, false);
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

        java.util.List<ServerPlayer> eligible = new java.util.ArrayList<>();
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            if (candidate == targetPlayer) continue;
            if (candidate.level() == level && candidate.distanceToSqr(targetPlayer) < NATURAL_APPEARANCE_MIN_DISTANCE_SQR) continue;
            if (VoiceStorage.getClipCount(candidate.getUUID()) < MinesisConfig.NATURAL_APPEARANCE_MIN_CLIPS.get()) continue;
            eligible.add(candidate);
        }
        if (eligible.isEmpty()) return null;
        return eligible.get(RANDOM.nextInt(eligible.size()));
    }

    public static void spawnNaturalMinesisNearPlayer(Player targetPlayer) {
        if (targetPlayer.level().isClientSide) return;

        if (!(targetPlayer.level() instanceof ServerLevel serverLevel)) return;
        if (hasAnyMinesisOnServer(serverLevel)) return;

        MinecraftServer server = targetPlayer.getServer();
        if (server == null || server.getPlayerList().getPlayers().size() < 2) return;

        if (!serverLevel.isNight()) return;

        Vec3 spawnPos = findNaturalCaveSpawnOutsideFov(targetPlayer, 20.0D, 30.0D);
        if (spawnPos == null) return;

        Player appearanceSource = findDistantAppearanceSource(targetPlayer);
        if (appearanceSource == null) return;

        // Ensure spawn is not too close to the player whose appearance is copied
        if (appearanceSource.distanceToSqr(spawnPos.x, spawnPos.y, spawnPos.z) < 20.0D * 20.0D) return;

        MinesisEntity m = new MinesisEntity(MinesisEntities.MIMESIS_ENTITY.get(), targetPlayer.level());
        m.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        m.setNaturalTarget(targetPlayer, appearanceSource);
        m.copyPlayerArmor(appearanceSource);
        targetPlayer.level().addFreshEntity(m);
    }

    /**
     * Return remaining ms until the next spawn attempt for this player, or 0 if due now.
     */
    public static long getRemainingSpawnCooldownMs(Player player) {
        if (player == null) return 0L;
        Long nextAttempt = nextSpawnAttemptTimes.get(player.getUUID());
        if (nextAttempt == null) return 0L;
        return Math.max(0L, nextAttempt - System.currentTimeMillis());
    }

    public static void runSpawnTest(CommandSourceStack source, Player executorPlayer) {
        final String P = "§6[Minesis Test]§r ";

        // 1. Player count — required before picking a victim
        MinecraftServer server = executorPlayer.getServer();
        int playerCount = (server != null) ? server.getPlayerList().getPlayers().size() : 0;
        source.sendSuccess(() -> Component.literal(P + "Players online: §e" + playerCount), false);
        if (server == null || playerCount < 2) {
            source.sendSuccess(() -> Component.literal(P + "§cSpawn failed — at least 2 players are required for a natural spawn."), false);
            return;
        }
        source.sendSuccess(() -> Component.literal(P + "§aEnough players online."), false);

        // 2. Pick a random victim from all online players (mirrors the server tick loop)
        java.util.List<ServerPlayer> allPlayers = new java.util.ArrayList<>(server.getPlayerList().getPlayers());
        final ServerPlayer victim = allPlayers.get(RANDOM.nextInt(allPlayers.size()));
        source.sendSuccess(() -> Component.literal(P + "Random target selected: §e" + victim.getName().getString()), false);

        // 3. Server level check (for the victim)
        if (!(victim.level() instanceof ServerLevel serverLevel)) {
            source.sendSuccess(() -> Component.literal(P + "§cSpawn failed — target is not in a server level."), false);
            return;
        }

        // 4. No existing Minesis
        if (hasAnyMinesisOnServer(serverLevel)) {
            source.sendSuccess(() -> Component.literal(P + "§cSpawn failed — a Minesis is already active on the server."), false);
            return;
        }
        source.sendSuccess(() -> Component.literal(P + "§aNo active Minesis found."), false);

        // 5. Night check
        boolean isNight = serverLevel.isNight();
        source.sendSuccess(() -> Component.literal(P + "Night time: §e" + (isNight ? "yes" : "no")), false);
        if (!isNight) {
            source.sendSuccess(() -> Component.literal(P + "§cSpawn failed — it must be night for a natural spawn."), false);
            return;
        }
        source.sendSuccess(() -> Component.literal(P + "§aIt is night."), false);

        // 6. Appearance source (relative to the victim)
        source.sendSuccess(() -> Component.literal(P + "Searching for an eligible appearance source..."), false);
        final Player appearanceSource = findDistantAppearanceSource(victim);
        if (appearanceSource == null) {
            source.sendSuccess(() -> Component.literal(P + "§cSpawn failed — no eligible player found to copy appearance from (must have §e"
                    + MinesisConfig.NATURAL_APPEARANCE_MIN_CLIPS.get() + "+§c clips and be far enough away)."), false);
            return;
        }
        final String sourceName = appearanceSource.getName().getString();
        source.sendSuccess(() -> Component.literal(P + "§aAppearance source found: §e" + sourceName), false);

        // 7. Spawn position (near the victim)
        source.sendSuccess(() -> Component.literal(P + "Searching for a valid spawn position near §e" + victim.getName().getString() + "§r..."), false);
        final Vec3 spawnPos = findNaturalCaveSpawnOutsideFov(victim, 20.0D, 30.0D);
        if (spawnPos == null) {
            source.sendSuccess(() -> Component.literal(P + "§cSpawn failed — no valid spawn position found near §e" + victim.getName().getString() + "§c."), false);
            return;
        }
        double distToVictimD = Math.sqrt(victim.distanceToSqr(spawnPos.x, spawnPos.y, spawnPos.z));
        source.sendSuccess(() -> Component.literal(P + "§aSpawn position found §e" + String.format("%.1f", distToVictimD) + "m§a from target."), false);

        // 8. Distance from appearance source
        double distToSourceD = Math.sqrt(appearanceSource.distanceToSqr(spawnPos.x, spawnPos.y, spawnPos.z));
        source.sendSuccess(() -> Component.literal(P + "Distance from spawn to §e" + sourceName + "§r: §e"
                + String.format("%.1f", distToSourceD) + "m§r (minimum: §e20m§r)"), false);
        if (distToSourceD < 20.0D) {
            source.sendSuccess(() -> Component.literal(P + "§cSpawn failed — spawn position is too close to §e" + sourceName + "§c."), false);
            return;
        }
        source.sendSuccess(() -> Component.literal(P + "§aSafe distance from §e" + sourceName + "§a."), false);

        // 9. 40% chance roll
        int roll = RANDOM.nextInt(100);
        source.sendSuccess(() -> Component.literal(P + "Rolling 40% chance... rolled §e" + roll + "/100"), false);
        if (roll >= SPAWN_ATTEMPT_CHANCE) {
            source.sendSuccess(() -> Component.literal(P + "§cSpawn attempt failed — unlucky roll ("
                    + roll + "/100, needed < " + SPAWN_ATTEMPT_CHANCE + "). Better luck next time."), false);
            return;
        }

        // All conditions met — spawn the Minesis targeting the random victim
        MinesisEntity m = new MinesisEntity(MinesisEntities.MIMESIS_ENTITY.get(), victim.level());
        m.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        m.setNaturalTarget(victim, appearanceSource);
        m.copyPlayerArmor(appearanceSource);
        victim.level().addFreshEntity(m);
        final String victimName = victim.getName().getString();
        source.sendSuccess(() -> Component.literal(P + "§a✔ All conditions met! A Minesis targeting §e" + victimName + "§a has spawned."), false);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (VoicePersistenceManager.shouldCleanup()) VoicePersistenceManager.cleanup();
        long now = System.currentTimeMillis();
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Player player : level.players()) {
                if (player.level().isClientSide) continue;
                UUID uuid = player.getUUID();

                // ── Natural spawn scheduling ──────────────────────────────
                Long nextAttempt = nextSpawnAttemptTimes.get(uuid);
                if (nextAttempt == null) {
                    scheduleNextSpawnAttempt(uuid, now);
                } else if (now >= nextAttempt) {
                    scheduleNextSpawnAttempt(uuid, now);
                    if (RANDOM.nextInt(100) < SPAWN_ATTEMPT_CHANCE) {
                        spawnNaturalMinesisNearPlayer(player);
                    }
                }

                // ── SVC speech-end response trigger ───────────────────────
                // Fires when the player just stopped talking (SVC-detected),
                // bypassing Vosk entirely so the entity always responds.
                boolean wasSpeaking = PlayerActivityTracker.hasRecentlySpoken(uuid, SPEECH_END_MAX_MS);
                boolean justStopped = wasSpeaking && !PlayerActivityTracker.hasRecentlySpoken(uuid, SPEECH_END_MIN_MS);
                if (!justStopped) continue;

                Long lastResp = lastSpeechResponseTime.get(uuid);
                if (lastResp != null && now - lastResp < SPEECH_RESPONSE_COOLDOWN_MS) continue;

                MinesisEntity entity = level.getEntitiesOfClass(
                        MinesisEntity.class,
                        player.getBoundingBox().inflate(20.0)
                ).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
                if (entity == null) continue;

                UUID voiceUUID = entity.getAppearancePlayerUUID() != null
                        ? entity.getAppearancePlayerUUID() : entity.getTargetPlayerUUID();
                if (voiceUUID == null || !VoiceStorage.hasVoiceClips(voiceUUID)) continue;

                de.maxhenkel.voicechat.api.VoicechatServerApi api = MinesisVoiceChatPlugin.getVoicechatApi();
                VoiceContext ctx = playerVoiceContext((ServerPlayer) player);
                boolean played = VoicePlaybackService.playVoiceClipForQuery(
                        entity, voiceUUID, api, ctx, "");
                if (played) {
                    lastSpeechResponseTime.put(uuid, now);
                    entity.notifyResponseTriggered();
                }
            }
        }
    }

    private static VoiceContext playerVoiceContext(ServerPlayer player) {
        UUID uuid = player.getUUID();

        // Container-based (highest priority — intent is unambiguous)
        if (player.containerMenu instanceof net.minecraft.world.inventory.FurnaceMenu
                || player.containerMenu instanceof net.minecraft.world.inventory.BlastFurnaceMenu
                || player.containerMenu instanceof net.minecraft.world.inventory.SmokerMenu)
            return VoiceContext.SMELTING;
        if (player.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu)
            return VoiceContext.CRAFTING;

        // Recent damage
        if (PlayerActivityTracker.isHurt(uuid))
            return VoiceContext.HURT;

        // Fleeing: sprinting away from a nearby hostile mob
        if (player.isSprinting() && hasNearbyHostileMob(player, 16.0))
            return VoiceContext.FLEEING;

        // Block-interaction activities
        if (PlayerActivityTracker.isWoodcutting(uuid))
            return VoiceContext.WOODCUTTING;
        if (PlayerActivityTracker.isMining(uuid))
            return VoiceContext.MINING;
        if (PlayerActivityTracker.isBuilding(uuid))
            return VoiceContext.BUILDING;
        if (PlayerActivityTracker.isFarming(uuid))
            return VoiceContext.FARMING;

        // Movement
        if (player.isSwimming() || player.isUnderWater())
            return VoiceContext.SWIMMING;
        if (player.isSprinting())
            return VoiceContext.RUNNING;
        Vec3 vel = player.getDeltaMovement();
        if (vel.x * vel.x + vel.z * vel.z > 0.005D)
            return VoiceContext.WALKING;
        return VoiceContext.IDLE;
    }

    private static boolean hasNearbyHostileMob(Player player, double range) {
        return !player.level().getEntitiesOfClass(
                net.minecraft.world.entity.monster.Monster.class,
                player.getBoundingBox().inflate(range),
                e -> !(e instanceof MinesisEntity)
        ).isEmpty();
    }

    private static void scheduleNextSpawnAttempt(UUID uuid, long now) {
        long delay = SPAWN_INTERVAL_MIN_MS + (long)(RANDOM.nextDouble() * (SPAWN_INTERVAL_MAX_MS - SPAWN_INTERVAL_MIN_MS));
        nextSpawnAttemptTimes.put(uuid, now + delay);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        for (MinesisEntity minesis : event.getLevel().getEntitiesOfClass(MinesisEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(16.0D))) {
            BlockPos cookingStation = minesis.getCookingStationPos();
            if (cookingStation != null && cookingStation.equals(pos)) {
                event.setCanceled(true);
                return;
            }
        }

        // Track player block-break activity for voice context tagging
        if (event.getPlayer() instanceof net.minecraft.server.level.ServerPlayer sp) {
            net.minecraft.world.level.block.Block broken =
                    event.getLevel().getBlockState(event.getPos()).getBlock();
            if (isLogBlock(broken)) {
                PlayerActivityTracker.markWoodcutting(sp.getUUID());
            } else if (isCropBlock(broken)) {
                PlayerActivityTracker.markFarming(sp.getUUID());
            } else {
                PlayerActivityTracker.markMining(sp.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerActivityTracker.markHurt(sp.getUUID());
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerActivityTracker.markBuilding(sp.getUUID());
        }
    }

    private static boolean isLogBlock(net.minecraft.world.level.block.Block block) {
        return block.defaultBlockState().is(net.minecraft.tags.BlockTags.LOGS);
    }

    private static boolean isCropBlock(net.minecraft.world.level.block.Block block) {
        return block instanceof net.minecraft.world.level.block.CropBlock
                || block instanceof net.minecraft.world.level.block.StemBlock
                || block instanceof net.minecraft.world.level.block.NetherWartBlock
                || block instanceof net.minecraft.world.level.block.SweetBerryBushBlock
                || block == net.minecraft.world.level.block.Blocks.MELON
                || block == net.minecraft.world.level.block.Blocks.PUMPKIN;
    }

    /**
     * Optional: Randomly spawn Minesis entities near players
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
