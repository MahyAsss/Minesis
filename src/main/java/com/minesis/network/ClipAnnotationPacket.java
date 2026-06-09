package com.minesis.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import com.minesis.voice.VoiceStorage;

import java.util.function.Supplier;

/**
 * Sent by a client whenever Vosk recognises speech, regardless of entity proximity.
 * The server uses it to annotate the most recent stored clip for that player with
 * a transcript, enabling semantic voice-clip matching later.
 */
public class ClipAnnotationPacket {

    private final String transcript;

    public ClipAnnotationPacket(String transcript) {
        this.transcript = transcript;
    }

    public static void encode(ClipAnnotationPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.transcript, 512);
    }

    public static ClipAnnotationPacket decode(FriendlyByteBuf buf) {
        return new ClipAnnotationPacket(buf.readUtf(512));
    }

    public static void handle(ClipAnnotationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null || msg.transcript.isBlank()) return;
            VoiceStorage.annotateLastClip(sender.getUUID(), msg.transcript);
        });
        ctx.get().setPacketHandled(true);
    }
}
