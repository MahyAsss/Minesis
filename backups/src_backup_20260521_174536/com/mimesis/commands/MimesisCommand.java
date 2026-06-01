/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 */
package com.mimesis.commands;

import com.mimesis.ServerEvents;
import com.mimesis.voice.VoiceManager;
import com.mimesis.voice.VoiceStorage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class MimesisCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder root = (LiteralArgumentBuilder)Commands.m_82127_((String)"mimesis").requires(source -> source.m_6761_(0));
        root.then(Commands.m_82127_((String)"spawnname").then(Commands.m_82129_((String)"name", (ArgumentType)StringArgumentType.word()).executes(context -> MimesisCommand.spawnAnonymousMimesis((CommandSourceStack)context.getSource(), StringArgumentType.getString((CommandContext)context, (String)"name")))));
        root.then(Commands.m_82127_((String)"me").executes(context -> MimesisCommand.spawnMimesis((CommandSourceStack)context.getSource())));
        root.then(((LiteralArgumentBuilder)Commands.m_82127_((String)"voicestatus").executes(context -> MimesisCommand.showVoiceStatus((CommandSourceStack)context.getSource()))).then(Commands.m_82129_((String)"name", (ArgumentType)StringArgumentType.word()).executes(context -> MimesisCommand.showVoiceStatusForName((CommandSourceStack)context.getSource(), StringArgumentType.getString((CommandContext)context, (String)"name")))));
        dispatcher.register(root);
    }

    private static int spawnMimesis(CommandSourceStack source) {
        try {
            ServerPlayer player = source.m_81375_();
            ServerEvents.spawnMimesisNearPlayer((Player)player);
            int clipCount = VoiceStorage.getClipCount(player.m_20148_());
            source.m_288197_(() -> Component.m_237113_((String)"\u00c2\u00a76[Mimesis] \u00c2\u00a7fA clone of you has spawned on you."), true);
            source.m_288197_(() -> Component.m_237113_((String)"\u00c2\u00a76[Mimesis] \u00c2\u00a7fHunt mode will activate in about \u00c2\u00a7e1 to 3 minutes\u00c2\u00a7f."), false);
            source.m_288197_(() -> Component.m_237113_((String)("\u00c2\u00a76[Mimesis] \u00c2\u00a7fStored voice clips: \u00c2\u00a7e" + clipCount)), false);
            if (clipCount == 0) {
                source.m_288197_(() -> Component.m_237113_((String)"\u00c2\u00a76[Mimesis] \u00c2\u00a7fSpeak with Simple Voice Chat to let Mimesis record your voice."), false);
            }
            return 1;
        }
        catch (CommandSyntaxException e) {
            source.m_81352_((Component)Component.m_237113_((String)"\u00c2\u00a7c[Mimesis] \u00c2\u00a7fPlayer not found."));
            return 0;
        }
    }

    private static int spawnAnonymousMimesis(CommandSourceStack source, String name) {
        try {
            ServerPlayer player = source.m_81375_();
            ServerEvents.spawnNamedMimesisNearPlayer((Player)player, name);
            source.m_288197_(() -> Component.m_237113_((String)("\u00c2\u00a76[Mimesis] \u00c2\u00a7fA clone of \u00c2\u00a7e" + name + "\u00c2\u00a7f has spawned on you.")), true);
            source.m_288197_(() -> Component.m_237113_((String)"\u00c2\u00a76[Mimesis] \u00c2\u00a7fHunt mode will activate in about \u00c2\u00a7e1 to 3 minutes\u00c2\u00a7f."), false);
            source.m_288197_(() -> Component.m_237113_((String)"\u00c2\u00a76[Mimesis] \u00c2\u00a7fSkin copied from target player (online or from Mojang API)."), false);
            return 1;
        }
        catch (CommandSyntaxException e) {
            source.m_81352_((Component)Component.m_237113_((String)"\u00c2\u00a7c[Mimesis] \u00c2\u00a7fPlayer not found."));
            return 0;
        }
    }

    private static int showVoiceStatusForName(CommandSourceStack source, String name) {
        try {
            MinecraftServer server = source.m_81377_();
            if (server == null) {
                source.m_81352_((Component)Component.m_237113_((String)"\u00c2\u00a7c[Mimesis] \u00c2\u00a7fServer not available."));
                return 0;
            }
            for (ServerPlayer p : server.m_6846_().m_11314_()) {
                if (!p.m_7755_().getString().equalsIgnoreCase(name)) continue;
                int clipCount = VoiceStorage.getClipCount(p.m_20148_());
                boolean voiceApiDetected = VoiceManager.isVoiceChatAvailable();
                source.m_288197_(() -> Component.m_237113_((String)("\u00c2\u00a76[Mimesis] \u00c2\u00a7fPlayer: \u00c2\u00a7e" + p.m_7755_().getString())), false);
                source.m_288197_(() -> Component.m_237113_((String)("\u00c2\u00a76[Mimesis] \u00c2\u00a7fVoice API detected: \u00c2\u00a7e" + voiceApiDetected)), false);
                source.m_288197_(() -> Component.m_237113_((String)("\u00c2\u00a76[Mimesis] \u00c2\u00a7fStored voice clips: \u00c2\u00a7e" + clipCount)), false);
                return 1;
            }
            source.m_81352_((Component)Component.m_237113_((String)"\u00c2\u00a7c[Mimesis] \u00c2\u00a7fPlayer not found or offline."));
            return 0;
        }
        catch (Exception ex) {
            source.m_81352_((Component)Component.m_237113_((String)"\u00c2\u00a7c[Mimesis] \u00c2\u00a7fError checking voice status."));
            return 0;
        }
    }

    private static int clearMimesis(CommandSourceStack source) {
        return 0;
    }

    private static int showVoiceStatus(CommandSourceStack source) {
        try {
            ServerPlayer player = source.m_81375_();
            String playerName = player.m_7755_().getString();
            int clipCount = VoiceStorage.getClipCount(player.m_20148_());
            boolean voiceApiDetected = VoiceManager.isVoiceChatAvailable();
            source.m_288197_(() -> Component.m_237113_((String)("\u00c2\u00a76[Mimesis] \u00c2\u00a7fPlayer: \u00c2\u00a7e" + playerName)), false);
            source.m_288197_(() -> Component.m_237113_((String)("\u00c2\u00a76[Mimesis] \u00c2\u00a7fVoice API detected: \u00c2\u00a7e" + voiceApiDetected)), false);
            source.m_288197_(() -> Component.m_237113_((String)("\u00c2\u00a76[Mimesis] \u00c2\u00a7fStored voice clips: \u00c2\u00a7e" + clipCount)), false);
            return 1;
        }
        catch (CommandSyntaxException e) {
            source.m_81352_((Component)Component.m_237113_((String)"\u00c2\u00a7c[Mimesis] \u00c2\u00a7fPlayer not found."));
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

