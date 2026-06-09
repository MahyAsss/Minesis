package com.minesis.network;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;

import com.minesis.MinesisMod;
import java.util.function.Supplier;

/**
 * Network communication for voice and entity synchronization
 */
public class VoiceNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MinesisMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int ID = 0;

    public static void init() {
        INSTANCE.messageBuilder(VoicePlaybackPacket.class, ID++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(VoicePlaybackPacket::decode)
                .encoder(VoicePlaybackPacket::encode)
                .consumerMainThread(VoicePlaybackPacket::handle)
                .add();

        INSTANCE.messageBuilder(JumpscarePacket.class, ID++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(JumpscarePacket::decode)
                .encoder(JumpscarePacket::encode)
                .consumerMainThread(JumpscarePacket::handle)
                .add();

        // Vosk: client sends recognized text → server picks voice clip
        INSTANCE.messageBuilder(TranscriptionPacket.class, ID++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(TranscriptionPacket::decode)
                .encoder(TranscriptionPacket::encode)
                .consumerMainThread(TranscriptionPacket::handle)
                .add();

        // Vosk: client annotates its most recent recorded clip with a transcript
        INSTANCE.messageBuilder(ClipAnnotationPacket.class, ID++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(ClipAnnotationPacket::decode)
                .encoder(ClipAnnotationPacket::encode)
                .consumerMainThread(ClipAnnotationPacket::handle)
                .add();
    }

    /**
     * Packet for triggering voice playback on clients
     */
    public static class VoicePlaybackPacket {
        private final int entityId;
        private final byte[] audioData;

        public VoicePlaybackPacket(int entityId, byte[] audioData) {
            this.entityId = entityId;
            this.audioData = audioData;
        }

        public static void encode(VoicePlaybackPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.entityId);
            buf.writeByteArray(msg.audioData);
        }

        public static VoicePlaybackPacket decode(FriendlyByteBuf buf) {
            int entityId = buf.readInt();
            byte[] audioData = buf.readByteArray();
            return new VoicePlaybackPacket(entityId, audioData);
        }

        public static void handle(VoicePlaybackPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // Handle voice playback on client side
                // This will be implemented when Simple Voice Chat integration is complete
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
