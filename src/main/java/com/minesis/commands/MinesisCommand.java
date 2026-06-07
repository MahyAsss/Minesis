package com.minesis.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.minesis.ServerEvents;
import com.minesis.voice.VoiceManager;
import com.minesis.voice.VoiceStorage;

public class MinesisCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("minesis")
            .requires(source -> source.hasPermission(2));

        root.then(Commands.literal("spawnname")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> spawnAnonymousMinesis(context.getSource(), StringArgumentType.getString(context, "name")))));

        root.then(Commands.literal("me")
            .executes(context -> spawnMinesis(context.getSource())));

        root.then(Commands.literal("voicestatus")
            .executes(context -> showVoiceStatus(context.getSource()))
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> showVoiceStatusForName(context.getSource(), StringArgumentType.getString(context, "name")))));

        root.then(Commands.literal("test")
            .executes(context -> testSpawn(context.getSource())));

        root.then(Commands.literal("fakevoiceclip")
            .then(Commands.argument("count", IntegerArgumentType.integer(0))
                .executes(context -> fakeVoiceClip(context.getSource(),
                        IntegerArgumentType.getInteger(context, "count")))));

        dispatcher.register(root);
    }

    private static int spawnMinesis(CommandSourceStack source) {
        try {
            Player player = source.getPlayerOrException();
            ServerEvents.spawnMinesisNearPlayer(player);
            int clipCount = VoiceStorage.getClipCount(player.getUUID());
            source.sendSuccess(() -> Component.literal("§6[Minesis] §fA clone of you has spawned on you."), true);
            source.sendSuccess(() -> Component.literal("§6[Minesis] §fHunt mode will activate in about §e1 to 3 minutes§f."), false);
            source.sendSuccess(() -> Component.literal("§6[Minesis] §fStored voice clips: §e" + clipCount), false);
            if (clipCount == 0) {
                source.sendSuccess(() -> Component.literal("§6[Minesis] §fSpeak with Simple Voice Chat to let Minesis record your voice."), false);
            }
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§c[Minesis] §fPlayer not found."));
            return 0;
        }
    }

    private static int spawnAnonymousMinesis(CommandSourceStack source, String name) {
        try {
            Player player = source.getPlayerOrException();
            ServerEvents.spawnNamedMinesisNearPlayer(player, name);
            source.sendSuccess(() -> Component.literal("§6[Minesis] §fA clone of §e" + name + "§f has spawned on you."), true);
            source.sendSuccess(() -> Component.literal("§6[Minesis] §fHunt mode will activate in about §e1 to 3 minutes§f."), false);
            source.sendSuccess(() -> Component.literal("§6[Minesis] §fSkin copied from target player (online or from Mojang API)."), false);
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§c[Minesis] §fPlayer not found."));
            return 0;
        }
    }

    private static int showVoiceStatusForName(CommandSourceStack source, String name) {
        try {
            MinecraftServer server = source.getServer();
            if (server == null) {
                source.sendFailure(Component.literal("§c[Minesis] §fServer not available."));
                return 0;
            }

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.getName().getString().equalsIgnoreCase(name)) {
                    int clipCount = VoiceStorage.getClipCount(p.getUUID());
                    boolean voiceApiDetected = VoiceManager.isVoiceChatAvailable();
                    source.sendSuccess(() -> Component.literal("§6[Minesis] §fPlayer: §e" + p.getName().getString()), false);
                    source.sendSuccess(() -> Component.literal("§6[Minesis] §fVoice API detected: §e" + voiceApiDetected), false);
                    source.sendSuccess(() -> Component.literal("§6[Minesis] §fStored voice clips: §e" + clipCount), false);
                    return 1;
                }
            }

            source.sendFailure(Component.literal("§c[Minesis] §fPlayer not found or offline."));
            return 0;
        } catch (Exception ex) {
            source.sendFailure(Component.literal("§c[Minesis] §fError checking voice status."));
            return 0;
        }
    }

    private static int fakeVoiceClip(CommandSourceStack source, int count) {
        try {
            Player player = source.getPlayerOrException();
            VoiceStorage.fakeClipCount(player.getUUID(), count);
            source.sendSuccess(() -> Component.literal("§6[Minesis] §fVoice clip count set to §e" + count + "§f for " + player.getName().getString() + "."), false);
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§c[Minesis] §fThis command can only be run by a player."));
            return 0;
        }
    }

    private static int testSpawn(CommandSourceStack source) {
        try {
            Player player = source.getPlayerOrException();
            ServerEvents.runSpawnTest(source, player);
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§c[Minesis] §fThis command can only be run by a player."));
            return 0;
        }
    }

    private static int clearMinesis(CommandSourceStack source) {
        return 0;
    }

    private static int showVoiceStatus(CommandSourceStack source) {
        try {
            Player player = source.getPlayerOrException();
            String playerName = player.getName().getString();
            int clipCount = VoiceStorage.getClipCount(player.getUUID());
            boolean voiceApiDetected = VoiceManager.isVoiceChatAvailable();

            source.sendSuccess(() -> Component.literal("§6[Minesis] §fPlayer: §e" + playerName), false);
            source.sendSuccess(() -> Component.literal("§6[Minesis] §fVoice API detected: §e" + voiceApiDetected), false);
            source.sendSuccess(() -> Component.literal("§6[Minesis] §fStored voice clips: §e" + clipCount), false);
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§c[Minesis] §fPlayer not found."));
            return 0;
        }
    }

    private static int showHelp(CommandSourceStack source) {
        return 0;
    }

    private static int chasePlayer(CommandSourceStack source, String targetName) {
        return 0;
    }

    private static int stopChase(CommandSourceStack source) {
        return 0;
    }
}
