package com.mimesis.voice;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mimesis.utils.AudioUtils;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.server.level.ServerPlayer;

@ForgeVoicechatPlugin
public class MimesisVoiceChatPlugin implements VoicechatPlugin {

    private final Map<UUID, OpusDecoder> decoders = new ConcurrentHashMap<>();
    private static VoicechatServerApi cachedApi = null;

    @Override
    public String getPluginId() {
        return "mimesis_voice_bridge";
    }

    @Override
    public void initialize(de.maxhenkel.voicechat.api.VoicechatApi api) {
        // Store the API for later use in playback
        if (api instanceof VoicechatServerApi serverApi) {
            cachedApi = serverApi;
            VoicePlaybackService.setVoicechatApi(serverApi);
        }
    }

    public static VoicechatServerApi getVoicechatApi() {
        return cachedApi;
    }

    @Override
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
        if (!(rawPlayer instanceof ServerPlayer serverPlayer)) {
            return;
        }

        UUID playerUUID = serverPlayer.getUUID();
        String playerName = serverPlayer.getGameProfile().getName();

        VoiceCaptureHandler handler = VoiceManager.getOrCreateCaptureHandler(playerUUID, playerName);

        OpusDecoder decoder = decoders.computeIfAbsent(playerUUID, key -> event.getVoicechat().createDecoder());
        if (decoder == null || decoder.isClosed()) {
            return;
        }

        byte[] opusData = event.getPacket().getOpusEncodedData();
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

        OpusDecoder decoder = decoders.remove(playerUUID);
        if (decoder != null && !decoder.isClosed()) {
            decoder.close();
        }
    }

    private void onVoicechatServerStopped(VoicechatServerStoppedEvent event) {
        VoiceManager.stopAllCaptures();
        decoders.values().forEach(decoder -> {
            if (decoder != null && !decoder.isClosed()) {
                decoder.close();
            }
        });
        decoders.clear();
    }
}