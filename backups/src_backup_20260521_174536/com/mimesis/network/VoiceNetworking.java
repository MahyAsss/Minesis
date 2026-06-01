/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraftforge.network.NetworkDirection
 *  net.minecraftforge.network.NetworkEvent$Context
 *  net.minecraftforge.network.NetworkRegistry
 *  net.minecraftforge.network.simple.SimpleChannel
 */
package com.mimesis.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class VoiceNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel((ResourceLocation)new ResourceLocation("mimesis", "main"), () -> "1", "1"::equals, "1"::equals);
    private static int ID = 0;

    public static void init() {
        INSTANCE.messageBuilder(VoicePlaybackPacket.class, ID++, NetworkDirection.PLAY_TO_CLIENT).decoder(VoicePlaybackPacket::decode).encoder(VoicePlaybackPacket::encode).consumerMainThread(VoicePlaybackPacket::handle).add();
    }

    public static class VoicePlaybackPacket {
        private final int entityId;
        private final byte[] audioData;

        public VoicePlaybackPacket(int entityId, byte[] audioData) {
            this.entityId = entityId;
            this.audioData = audioData;
        }

        public static void encode(VoicePlaybackPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.entityId);
            buf.m_130087_(msg.audioData);
        }

        public static VoicePlaybackPacket decode(FriendlyByteBuf buf) {
            int entityId = buf.readInt();
            byte[] audioData = buf.m_130052_();
            return new VoicePlaybackPacket(entityId, audioData);
        }

        public static void handle(VoicePlaybackPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {});
            ctx.get().setPacketHandled(true);
        }
    }
}

