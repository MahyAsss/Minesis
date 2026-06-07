package com.minesis.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class JumpscarePacket {

    public static void encode(JumpscarePacket msg, FriendlyByteBuf buf) {}

    public static JumpscarePacket decode(FriendlyByteBuf buf) {
        return new JumpscarePacket();
    }

    public static void handle(JumpscarePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> com.minesis.client.JumpscareOverlay::trigger));
        ctx.get().setPacketHandled(true);
    }
}
