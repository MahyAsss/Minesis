/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  de.maxhenkel.voicechat.api.ForgeVoicechatPlugin
 *  de.maxhenkel.voicechat.api.VoicechatApi
 *  de.maxhenkel.voicechat.api.VoicechatConnection
 *  de.maxhenkel.voicechat.api.VoicechatPlugin
 *  de.maxhenkel.voicechat.api.VoicechatServerApi
 *  de.maxhenkel.voicechat.api.events.EventRegistration
 *  de.maxhenkel.voicechat.api.events.MicrophonePacketEvent
 *  de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent
 *  de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent
 *  de.maxhenkel.voicechat.api.opus.OpusDecoder
 *  de.maxhenkel.voicechat.api.packets.MicrophonePacket
 *  net.minecraft.server.level.ServerPlayer
 */
package com.mimesis.voice;

import com.mimesis.utils.AudioUtils;
import com.mimesis.voice.VoiceCaptureHandler;
import com.mimesis.voice.VoiceManager;
import com.mimesis.voice.VoicePlaybackService;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

@ForgeVoicechatPlugin
public class MimesisVoiceChatPlugin
implements VoicechatPlugin {
    private final Map<UUID, OpusDecoder> decoders = new ConcurrentHashMap<UUID, OpusDecoder>();
    private static VoicechatServerApi cachedApi = null;

    public String getPluginId() {
        return "mimesis_voice_bridge";
    }

    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi) {
            VoicechatServerApi serverApi;
            cachedApi = serverApi = (VoicechatServerApi)api;
            VoicePlaybackService.setVoicechatApi(serverApi);
        }
    }

    public static VoicechatServerApi getVoicechatApi() {
        return cachedApi;
    }

    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        registration.registerEvent(PlayerDisconnectedEvent.class, this::onPlayerDisconnected);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onVoicechatServerStopped);
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection senderConnection = event.getSenderConnection();
        if (senderConnection == null || senderConnection.getPlayer() == null) {
            return;
        }
        Object rawPlayer = senderConnection.getPlayer().getPlayer();
        if (!(rawPlayer instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer serverPlayer = (ServerPlayer)rawPlayer;
        UUID playerUUID = serverPlayer.m_20148_();
        String playerName = serverPlayer.m_36316_().getName();
        VoiceCaptureHandler handler = VoiceManager.getOrCreateCaptureHandler(playerUUID, playerName);
        OpusDecoder decoder = this.decoders.computeIfAbsent(playerUUID, key -> event.getVoicechat().createDecoder());
        if (decoder == null || decoder.isClosed()) {
            return;
        }
        byte[] opusData = ((MicrophonePacket)event.getPacket()).getOpusEncodedData();
        if (opusData == null || opusData.length == 0) {
            return;
        }
        short[] pcmShorts = decoder.decode(opusData);
        if (pcmShorts == null || pcmShorts.length == 0) {
            return;
        }
        byte[] pcmBytes = AudioUtils.shortsToBytes(pcmShorts);
        handler.onAudioData(pcmBytes);
    }

    private void onPlayerDisconnected(PlayerDisconnectedEvent event) {
        UUID playerUUID = event.getPlayerUuid();
        VoiceManager.unregisterCaptureHandler(playerUUID);
        OpusDecoder decoder = this.decoders.remove(playerUUID);
        if (decoder != null && !decoder.isClosed()) {
            decoder.close();
        }
    }

    private void onVoicechatServerStopped(VoicechatServerStoppedEvent event) {
        VoiceManager.stopAllCaptures();
        this.decoders.values().forEach(decoder -> {
            if (decoder != null && !decoder.isClosed()) {
                decoder.close();
            }
        });
        this.decoders.clear();
    }
}

